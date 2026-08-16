package com.gryffindor.smartshopping.data.detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.gryffindor.smartshopping.domain.model.CameraFrame
import java.io.ByteArrayOutputStream

/**
 * Converts CameraFrame bytes into a detector-compatible Bitmap (300x300 ARGB_8888).
 *
 * Gen2 실기기 검증 결과:
 *   width=504, height=896, data.size=677376, bytesPerPixel=1.5, isCompressed=false
 *   → YUV420 family (uncompressed)
 *
 * YUV420 sub-format (NV21 vs NV12 vs I420)은 아직 확정되지 않았으므로,
 * 첫 프레임에서 최소 probe를 수행하여 UV plane 배치를 추론한다.
 *
 * Probe 전략:
 *   NV21: Y plane [W*H] + interleaved VU [W*H/2], V first
 *   NV12: Y plane [W*H] + interleaved UV [W*H/2], U first
 *   I420: Y plane [W*H] + U plane [W*H/4] + V plane [W*H/4]
 *
 * Android YuvImage는 NV21만 공식 지원하므로:
 *   - NV21 → YuvImage 직접 사용
 *   - NV12 → UV swap → NV21 → YuvImage
 *   - I420 → planar to NV21 interleave → YuvImage
 *   - UNKNOWN → NV21 시도 (green tint 등이 보이면 사용자가 보고)
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
        UNKNOWN     // Could not determine — attempt NV21
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
     * Probe the YUV layout on the first frame and log the result.
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

    /**
     * Probe the UV plane arrangement.
     *
     * Heuristic: In NV21 the chroma plane starts at offset W*H and is interleaved V,U,V,U,...
     * In NV12 it's U,V,U,V,...
     * In I420, U and V planes are separate (each W*H/4).
     *
     * We check byte patterns at the chroma plane start:
     *   - For a real image, U and V values cluster around 128 (neutral).
     *   - NV21 vs NV12: check if byte pairs show V-first or U-first by examining
     *     the alternating pattern against a statistical heuristic.
     *
     * Since we cannot reliably distinguish NV21/NV12 from byte values alone without
     * a known color reference, we use a practical approach:
     *   1. Try NV21 decode → if it produces a reasonable image (no green tint), use NV21
     *   2. If NV21 fails or we detect clear interleave inversion, switch to NV12
     *
     * For Gen2 hackathon: default to NV21 (most common Android format) and let the
     * live visual check confirm. Log enough info for quick diagnosis if wrong.
     */
    private fun probeYuvLayout(frame: CameraFrame): YuvLayout {
        val w = frame.width
        val h = frame.height
        val expectedSize = (w * h * 3) / 2

        if (frame.data.size != expectedSize) {
            Log.w(TAG, "Data size ${frame.data.size} != expected YUV420 size $expectedSize")
            return YuvLayout.UNKNOWN
        }

        val yPlaneSize = w * h
        val chromaSize = frame.data.size - yPlaneSize  // Should be W*H/2

        // Check if chroma is interleaved (NV21/NV12) or planar (I420)
        // Interleaved: chroma bytes come in pairs covering 2x2 pixel blocks
        // Planar: U plane (W*H/4) then V plane (W*H/4)
        //
        // Heuristic for planar detection:
        // In I420, the midpoint of chroma should be a boundary between U and V planes.
        // Both U and V planes individually should have relatively consistent values for
        // uniform regions. Interleaved formats would show alternation even in uniform regions.
        //
        // Simple check: sample stride pattern in chroma area
        val chromaStart = yPlaneSize
        val halfChroma = chromaSize / 2

        // Sample first 16 bytes of chroma in pairs and check for interleave signature
        val sampleSize = minOf(32, chromaSize)
        if (sampleSize < 4) return YuvLayout.UNKNOWN

        // Check if consecutive bytes alternate significantly (interleaved hint)
        // vs remain similar (planar hint for uniform regions)
        var interleaveScore = 0
        var planarScore = 0

        for (i in 0 until sampleSize - 2 step 2) {
            val b0 = frame.data[chromaStart + i].toInt() and 0xFF
            val b1 = frame.data[chromaStart + i + 1].toInt() and 0xFF
            val b2 = frame.data[chromaStart + i + 2].toInt() and 0xFF

            // In interleaved, b0 and b2 are same channel → likely similar
            // b0 and b1 are different channels → might differ
            val sameChannelDiff = kotlin.math.abs(b0 - b2)
            val crossChannelDiff = kotlin.math.abs(b0 - b1)

            if (sameChannelDiff < crossChannelDiff) {
                interleaveScore++
            } else {
                planarScore++
            }
        }

        // Also check: does the second half of chroma differ from first half?
        // In I420, first half is all U, second half is all V (or vice versa)
        val firstHalfAvg = averageBytes(frame.data, chromaStart, chromaStart + halfChroma / 4)
        val secondHalfAvg = averageBytes(frame.data, chromaStart + halfChroma, chromaStart + halfChroma + halfChroma / 4)
        val halfDiff = kotlin.math.abs(firstHalfAvg - secondHalfAvg)

        // Strong divergence between halves suggests planar
        if (halfDiff > 20 && planarScore > interleaveScore) {
            Log.d(TAG, "Probe: planar signature detected (halfDiff=$halfDiff, planar=$planarScore, interleave=$interleaveScore)")
            return YuvLayout.I420
        }

        // Default to NV21 for interleaved (most common on Android)
        // NV12 vs NV21 cannot be reliably distinguished without a color reference.
        // Log both scores so user can confirm visually.
        Log.d(TAG, "Probe: interleaved signature (interleave=$interleaveScore, planar=$planarScore, halfDiff=$halfDiff)")
        Log.d(TAG, "Probe: defaulting to NV21 (most common Android YUV420). " +
            "If colors appear wrong (blue/red swap), switch to NV12.")

        return YuvLayout.NV21
    }

    private fun averageBytes(data: ByteArray, from: Int, to: Int): Int {
        if (to <= from || from >= data.size) return 128
        val end = minOf(to, data.size)
        var sum = 0L
        for (i in from until end) {
            sum += (data[i].toInt() and 0xFF)
        }
        return (sum / (end - from)).toInt()
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
                // Attempt NV21 interpretation — may produce artifacts
                Log.w(TAG, "Unknown layout — attempting NV21 interpretation")
                frame.data
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
        val yPlaneSize = width * height
        val uvPlaneSize = yPlaneSize / 4
        val totalSize = (width * height * 3) / 2

        val buffer = getOrAllocateBuffer(totalSize)
        // Copy Y plane
        System.arraycopy(data, 0, buffer, 0, yPlaneSize)

        // Interleave V and U (NV21 = V first)
        val uPlaneOffset = yPlaneSize
        val vPlaneOffset = yPlaneSize + uvPlaneSize

        var destIdx = yPlaneSize
        for (j in 0 until uvPlaneSize) {
            buffer[destIdx++] = data[vPlaneOffset + j]  // V
            buffer[destIdx++] = data[uPlaneOffset + j]  // U
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
