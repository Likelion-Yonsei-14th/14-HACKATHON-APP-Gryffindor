package com.gryffindor.smartshopping.data.detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.gryffindor.smartshopping.data.image.PackedI420Converter
import com.gryffindor.smartshopping.domain.model.CameraFrame
import java.io.ByteArrayOutputStream

/**
 * Converts CameraFrame bytes into a detector-compatible Bitmap (300x300 ARGB_8888).
 *
 * Gen2 실기기 검증 결과:
 *   width=504, height=896, data.size=677376, bytesPerPixel=1.5, isCompressed=false
 *   → YUV420 family (uncompressed)
 *
 * Meta DAT의 uncompressed VideoFrame은 packed I420으로 전달된다.
 *
 * Packed layout reference:
 *   NV21: Y plane [W*H] + interleaved VU [W*H/2], V first
 *   NV12: Y plane [W*H] + interleaved UV [W*H/2], U first
 *   I420: Y plane [W*H] + U plane [W*H/4] + V plane [W*H/4]
 *
 * Android YuvImage는 NV21만 공식 지원하므로:
 *   - NV21 → YuvImage 직접 사용
 *   - NV12 → UV swap → NV21 → YuvImage
 *   - I420 → planar to NV21 interleave → YuvImage
 *   - malformed/unknown → conversion 실패로 처리해 색상 artifact를 만들지 않음
 *
 * All operations run off Main thread.
 */
internal class FrameConverter {

    companion object {
        private const val TAG = "FrameConverter"
        private const val DETECTOR_INPUT_SIZE = 300
        private const val JPEG_QUALITY = 90
    }

    /**
     * Detected YUV sub-layout after probe.
     * null = not yet probed.
     */
    @Volatile
    private var detectedLayout: YuvLayout? = null

    /**
     * Reusable JPEG output stream to avoid per-frame allocation.
     */
    private val jpegOutputStream = ByteArrayOutputStream(DETECTOR_INPUT_SIZE * DETECTOR_INPUT_SIZE * 2)

    /**
     * Reusable NV21 conversion buffer for NV12→NV21 or I420→NV21 swap.
     * Allocated lazily on first use at the required size.
     */
    private var conversionBuffer: ByteArray? = null

    enum class YuvLayout {
        NV21,       // V-U interleaved (Android standard)
        NV12,       // U-V interleaved (iOS/some cameras)
        I420,       // Planar Y + U + V
        UNKNOWN     // Invalid or unsupported buffer
    }

    /**
     * Convert a CameraFrame to a 300x300 ARGB_8888 Bitmap suitable for TFLite inference.
     *
     * @return Bitmap on success, null if conversion fails.
     */
    fun convert(frame: CameraFrame): Bitmap? {
        return try {
            if (frame.isCompressed) {
                convertCompressed(frame)
            } else {
                convertYuv420(frame)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Frame conversion failed: ${e.message}")
            null
        }
    }

    /**
     * Confirm the packed layout emitted by Meta DAT's uncompressed VideoFrame.
     * Subsequent calls return the cached layout.
     */
    fun probeLayoutIfNeeded(frame: CameraFrame): YuvLayout {
        detectedLayout?.let { return it }

        val layout = probeYuvLayout(frame)
        detectedLayout = layout

        Log.i(TAG, buildString {
            append("=== YUV Layout Probe Result ===\n")
            append("  detected: $layout\n")
            append("  width: ${frame.width}, height: ${frame.height}\n")
            append("  data.size: ${frame.data.size}\n")
            append("  expected YUV420: ${(frame.width * frame.height * 3) / 2}\n")
            append("================================")
        })

        // Log to DetectionDiag tag as well for unified diagnostics
        Log.i("DetectionDiag", "YUV layout probe: $layout (${frame.width}x${frame.height})")

        return layout
    }

    private fun probeYuvLayout(frame: CameraFrame): YuvLayout {
        val w = frame.width
        val h = frame.height
        val expectedSize = (w * h * 3) / 2

        if (frame.data.size != expectedSize) {
            Log.w(TAG, "Data size ${frame.data.size} != expected YUV420 size $expectedSize")
            return YuvLayout.UNKNOWN
        }

        Log.d(TAG, "Gen2 uncompressed frame is packed I420 (${w}x${h}, ${frame.data.size} bytes)")
        return YuvLayout.I420
    }

    // --- Conversion paths ---

    private fun convertYuv420(frame: CameraFrame): Bitmap? {
        val layout = probeLayoutIfNeeded(frame)

        // Get NV21 data (swap if needed)
        val nv21Data = when (layout) {
            YuvLayout.NV21 -> frame.data
            YuvLayout.NV12 -> swapNv12ToNv21(frame.data, frame.width, frame.height)
            YuvLayout.I420 -> convertI420ToNv21(frame.data, frame.width, frame.height)
            YuvLayout.UNKNOWN -> {
                Log.e(TAG, "Unknown YUV layout — refusing conversion to avoid color artifacts")
                return null
            }
        }

        // Use Android YuvImage (NV21) → JPEG → Bitmap → resize
        val yuvImage = YuvImage(nv21Data, ImageFormat.NV21, frame.width, frame.height, null)

        jpegOutputStream.reset()
        val success = yuvImage.compressToJpeg(
            Rect(0, 0, frame.width, frame.height),
            JPEG_QUALITY,
            jpegOutputStream
        )
        if (!success) {
            Log.e(TAG, "YuvImage.compressToJpeg failed")
            return null
        }

        val jpegBytes = jpegOutputStream.toByteArray()
        val fullBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return null

        // Resize to detector input
        val resized = if (fullBitmap.width == DETECTOR_INPUT_SIZE && fullBitmap.height == DETECTOR_INPUT_SIZE) {
            fullBitmap
        } else {
            val scaled = Bitmap.createScaledBitmap(fullBitmap, DETECTOR_INPUT_SIZE, DETECTOR_INPUT_SIZE, true)
            fullBitmap.recycle()
            scaled
        }

        return resized
    }

    /**
     * Swap NV12 (U-V interleaved) to NV21 (V-U interleaved).
     * Only the chroma plane byte pairs need swapping.
     */
    private fun swapNv12ToNv21(data: ByteArray, width: Int, height: Int): ByteArray {
        val yPlaneSize = width * height
        val totalSize = data.size

        val buffer = getOrAllocateBuffer(totalSize)
        // Copy Y plane as-is
        System.arraycopy(data, 0, buffer, 0, yPlaneSize)

        // Swap UV pairs in chroma plane
        var i = yPlaneSize
        while (i < totalSize - 1) {
            buffer[i] = data[i + 1]     // V from position i+1
            buffer[i + 1] = data[i]     // U from position i
            i += 2
        }

        return buffer
    }

    /**
     * Convert I420 (planar Y + U + V) to NV21 (semi-planar Y + interleaved VU).
     */
    private fun convertI420ToNv21(data: ByteArray, width: Int, height: Int): ByteArray {
        val totalSize = PackedI420Converter.expectedSize(width, height)
            ?: throw IllegalArgumentException("Invalid I420 dimensions: ${width}x${height}")

        val buffer = getOrAllocateBuffer(totalSize)
        if (!PackedI420Converter.copyToNv21(data, width, height, buffer)) {
            throw IllegalArgumentException(
                "Invalid packed I420 data: actual=${data.size}, expected=$totalSize"
            )
        }
        return buffer
    }

    private fun getOrAllocateBuffer(size: Int): ByteArray {
        val existing = conversionBuffer
        if (existing != null && existing.size >= size) return existing
        val newBuffer = ByteArray(size)
        conversionBuffer = newBuffer
        return newBuffer
    }

    /**
     * Decode a compressed frame (JPEG/PNG) via BitmapFactory.
     */
    private fun convertCompressed(frame: CameraFrame): Bitmap? {
        val decoded = BitmapFactory.decodeByteArray(frame.data, 0, frame.data.size)
        if (decoded == null) {
            Log.e(TAG, "BitmapFactory failed to decode compressed frame (${frame.data.size} bytes)")
            return null
        }

        return if (decoded.width == DETECTOR_INPUT_SIZE && decoded.height == DETECTOR_INPUT_SIZE) {
            decoded
        } else {
            val scaled = Bitmap.createScaledBitmap(decoded, DETECTOR_INPUT_SIZE, DETECTOR_INPUT_SIZE, true)
            decoded.recycle()
            scaled
        }
    }

    /** Return the currently detected layout (for diagnostics). Null if not yet probed. */
    fun getDetectedLayout(): YuvLayout? = detectedLayout
}
