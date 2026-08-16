package com.gryffindor.smartshopping.data.attention

import android.graphics.Bitmap
import android.util.Log
import com.gryffindor.smartshopping.core.config.AppConfig
import java.io.ByteArrayOutputStream

/**
 * Encodes a Bitmap to JPEG bytes at the configured quality.
 *
 * Responsibilities:
 *   - Accept a Bitmap
 *   - Encode to JPEG at [AppConfig.ATTENTION_JPEG_QUALITY]
 *   - Return non-empty ByteArray, or null on failure
 *   - Recycle the input Bitmap after encoding
 *
 * All operations execute off Main thread.
 * No Meta DAT, TFLite, Retrofit, or Backend DTO types referenced.
 */
internal open class JpegEncoder(
    private val quality: Int = AppConfig.ATTENTION_JPEG_QUALITY
) {

    companion object {
        private const val TAG = "JpegEncoder"
    }

    /** Reusable output stream to reduce per-encoding allocations. */
    private val outputStream = ByteArrayOutputStream(1024 * 64)

    /**
     * Encode the given Bitmap to JPEG bytes.
     * The input Bitmap is recycled after encoding.
     *
     * @param bitmap The Bitmap to encode. Will be recycled by this method.
     * @return Non-empty JPEG ByteArray on success, null on failure.
     */
    open fun encode(bitmap: Bitmap): ByteArray? {
        return try {
            doEncode(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "JPEG encoding failed: ${e.message}")
            // Ensure bitmap is recycled even on unexpected error
            try { bitmap.recycle() } catch (_: Exception) {}
            null
        }
    }

    private fun doEncode(bitmap: Bitmap): ByteArray? {
        outputStream.reset()

        val success = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        bitmap.recycle()

        if (!success) {
            Log.e(TAG, "Bitmap.compress(JPEG) returned false")
            return null
        }

        val bytes = outputStream.toByteArray()
        if (bytes.isEmpty()) {
            Log.e(TAG, "JPEG encoding produced empty byte array")
            return null
        }

        return bytes
    }
}
