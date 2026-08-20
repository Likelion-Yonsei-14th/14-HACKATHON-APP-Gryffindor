package com.gryffindor.smartshopping.data.attention

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.gryffindor.smartshopping.core.config.AppConfig
import com.gryffindor.smartshopping.data.image.PackedI420Converter
import com.gryffindor.smartshopping.domain.model.CameraFrame
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Generates a cropped Bitmap from the original Gen2 CameraFrame using normalized bbox coordinates.
 *
 * Crop pipeline:
 *   1. Convert full-resolution CameraFrame (e.g. 504×896) to Bitmap via YUV→JPEG→Bitmap
 *      (does NOT resize to 300×300 like the detector path)
 *   2. Apply padding to normalized bbox
 *   3. Clamp padded coordinates to frame boundaries
 *   4. Crop the region from the full-resolution Bitmap
 *   5. Validate minimum short-side requirement
 *   6. Downscale if long side exceeds maximum
 *
 * All operations execute off Main thread.
 * No Meta DAT, TFLite, or Retrofit types referenced.
 */
internal open class CropGenerator(
    private val paddingRatio: Float = AppConfig.ATTENTION_CROP_PADDING_RATIO,
    private val minShortSide: Int = AppConfig.ATTENTION_MIN_CROP_SHORT_SIDE,
    private val maxLongSide: Int = AppConfig.ATTENTION_MAX_CROP_LONG_SIDE
) {

    companion object {
        private const val TAG = "CropGenerator"
        private const val JPEG_QUALITY_FOR_DECODE = 90
    }

    /** Result of a successful crop operation. */
    data class CropResult(
        val bitmap: Bitmap,
        val width: Int,
        val height: Int
    )

    /** Reusable output stream to reduce allocations. */
    private val jpegOutputStream = ByteArrayOutputStream(1024 * 128)

    /**
     * Crop the attended object region from the original CameraFrame.
     *
     * @param frame Original Gen2 CameraFrame (e.g. 504×896 packed I420/YUV420).
     * @param left Normalized left bbox [0.0, 1.0].
     * @param top Normalized top bbox [0.0, 1.0].
     * @param right Normalized right bbox [0.0, 1.0].
     * @param bottom Normalized bottom bbox [0.0, 1.0].
     * @return CropResult with the cropped Bitmap and dimensions, or null on failure.
     */
    open fun crop(frame: CameraFrame, left: Float, top: Float, right: Float, bottom: Float): CropResult? {
        return try {
            doCrop(frame, left, top, right, bottom)
        } catch (e: Exception) {
            Log.e(TAG, "Crop failed: ${e.message}")
            null
        }
    }

    private fun doCrop(
        frame: CameraFrame,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): CropResult? {
        // 1. Convert full-resolution CameraFrame to Bitmap
        val fullBitmap = convertFrameToBitmap(frame)
        if (fullBitmap == null) {
            Log.w(TAG, "Failed to convert CameraFrame to Bitmap")
            return null
        }

        val frameWidth = fullBitmap.width
        val frameHeight = fullBitmap.height

        // 2. Apply padding to normalized bbox
        val bboxWidth = right - left
        val bboxHeight = bottom - top
        val padX = bboxWidth * paddingRatio
        val padY = bboxHeight * paddingRatio

        val paddedLeft = left - padX
        val paddedTop = top - padY
        val paddedRight = right + padX
        val paddedBottom = bottom + padY

        // 3. Clamp to frame boundaries [0, 1]
        val clampedLeft = paddedLeft.coerceIn(0f, 1f)
        val clampedTop = paddedTop.coerceIn(0f, 1f)
        val clampedRight = paddedRight.coerceIn(0f, 1f)
        val clampedBottom = paddedBottom.coerceIn(0f, 1f)

        // Convert normalized to pixel coordinates
        val pixelLeft = (clampedLeft * frameWidth).roundToInt().coerceIn(0, frameWidth - 1)
        val pixelTop = (clampedTop * frameHeight).roundToInt().coerceIn(0, frameHeight - 1)
        val pixelRight = (clampedRight * frameWidth).roundToInt().coerceIn(pixelLeft + 1, frameWidth)
        val pixelBottom = (clampedBottom * frameHeight).roundToInt().coerceIn(pixelTop + 1, frameHeight)

        val cropWidth = pixelRight - pixelLeft
        val cropHeight = pixelBottom - pixelTop

        Log.d(TAG, buildString {
            append("CropGenerator: rect=($pixelLeft,$pixelTop,$pixelRight,$pixelBottom) ")
            append("output=${cropWidth}x${cropHeight} ")
            append("frame=${frameWidth}x${frameHeight}")
        })

        // 4. Validate minimum short-side requirement
        val shortSide = min(cropWidth, cropHeight)
        if (shortSide < minShortSide) {
            Log.d(TAG, "Crop too small: ${cropWidth}x${cropHeight}, short side $shortSide < $minShortSide")
            fullBitmap.recycle()
            return null
        }

        // 5. Crop region from full-resolution Bitmap
        val rawCrop = Bitmap.createBitmap(fullBitmap, pixelLeft, pixelTop, cropWidth, cropHeight)

        // CRITICAL: Bitmap.createBitmap() may return the source bitmap itself
        // when the crop region equals the entire source (full-frame clamp).
        // In that case, we must NOT recycle fullBitmap because it IS the crop.
        val croppedBitmap = if (rawCrop === fullBitmap) {
            // Full-frame crop case: create an independent copy so we have clear ownership
            Log.d(TAG, "Full-frame crop detected (crop === source), creating independent copy")
            val copy = rawCrop.copy(rawCrop.config ?: Bitmap.Config.ARGB_8888, false)
            fullBitmap.recycle()
            copy ?: run {
                Log.e(TAG, "Bitmap.copy() returned null for full-frame crop")
                return null
            }
        } else {
            // Normal partial crop: safe to recycle source
            fullBitmap.recycle()
            rawCrop
        }

        // 6. Downscale if long side exceeds maximum
        val longSide = max(croppedBitmap.width, croppedBitmap.height)
        val finalBitmap = if (longSide > maxLongSide) {
            val scale = maxLongSide.toFloat() / longSide.toFloat()
            val scaledWidth = (croppedBitmap.width * scale).roundToInt()
            val scaledHeight = (croppedBitmap.height * scale).roundToInt()
            val scaled = Bitmap.createScaledBitmap(croppedBitmap, scaledWidth, scaledHeight, true)
            croppedBitmap.recycle()
            scaled
        } else {
            croppedBitmap
        }

        // Capture dimensions BEFORE any potential recycle by caller
        val resultWidth = finalBitmap.width
        val resultHeight = finalBitmap.height

        Log.d(TAG, "CropGenerator output: ${resultWidth}x${resultHeight} isRecycled=${finalBitmap.isRecycled}")

        return CropResult(
            bitmap = finalBitmap,
            width = resultWidth,
            height = resultHeight
        )
    }

    /**
     * Convert CameraFrame to full-resolution Bitmap.
     * Reuses the NV21→YuvImage→JPEG→Bitmap path from A2 FrameConverter,
     * but does NOT resize to 300×300.
     */
    private fun convertFrameToBitmap(frame: CameraFrame): Bitmap? {
        if (frame.isCompressed) {
            // Compressed frames (JPEG/PNG) — decode directly
            return BitmapFactory.decodeByteArray(frame.data, 0, frame.data.size)
        }

        // Meta DAT emits packed I420 (Y + U + V); YuvImage requires NV21 (Y + VU).
        val nv21Data = PackedI420Converter.toNv21(frame.data, frame.width, frame.height)
        if (nv21Data == null) {
            val expectedSize = PackedI420Converter.expectedSize(frame.width, frame.height)
            Log.w(TAG, "Invalid packed I420 frame: actual=${frame.data.size}, expected=$expectedSize")
            return null
        }

        // I420 → NV21 → YuvImage → JPEG → Bitmap (full resolution)
        val yuvImage = YuvImage(nv21Data, ImageFormat.NV21, frame.width, frame.height, null)

        jpegOutputStream.reset()
        val success = yuvImage.compressToJpeg(
            Rect(0, 0, frame.width, frame.height),
            JPEG_QUALITY_FOR_DECODE,
            jpegOutputStream
        )
        if (!success) {
            Log.e(TAG, "YuvImage.compressToJpeg failed for crop")
            return null
        }

        val jpegBytes = jpegOutputStream.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }
}
