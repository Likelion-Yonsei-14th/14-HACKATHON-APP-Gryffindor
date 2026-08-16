package com.gryffindor.smartshopping.data.detection

import android.util.Log
import com.gryffindor.smartshopping.domain.model.CameraFrame

/**
 * One-shot format diagnostic tool for Gen2 CameraFrame data.
 *
 * Runs on the first CameraFrame received and classifies the byte format into
 * a candidate family. Does NOT claim a specific sub-format (e.g., NV21 vs NV12)
 * from byte length alone. Real Gen2 verification is required before the format
 * is considered confirmed.
 *
 * All work executes on the calling coroutine (must be called from a background dispatcher).
 */
internal class FormatVerifier {

    companion object {
        private const val TAG = "DetectionDiag"
        private const val HEX_SIGNATURE_BYTES = 16
    }

    @Volatile
    private var diagnosed = false

    /** The diagnostic result from the first frame. Null until [verifyIfNeeded] runs. */
    var diagnostic: FormatDiagnostic? = null
        private set

    /**
     * Classify the frame format if not already done. Safe to call on every frame;
     * only the first invocation performs actual work.
     *
     * @return the diagnostic record, or null if already diagnosed (returns cached)
     */
    fun verifyIfNeeded(frame: CameraFrame): FormatDiagnostic? {
        if (diagnosed) return diagnostic
        diagnosed = true

        val result = classify(frame)
        diagnostic = result
        logDiagnostic(result)
        return result
    }

    private fun classify(frame: CameraFrame): FormatDiagnostic {
        val w = frame.width
        val h = frame.height
        val size = frame.data.size
        val bpp = if (w > 0 && h > 0) size.toFloat() / (w * h) else 0f
        val rowSize = if (h > 0) size / h else 0

        return if (frame.isCompressed) {
            classifyCompressed(frame, w, h, size, bpp, rowSize)
        } else {
            classifyRaw(frame, w, h, size, bpp, rowSize)
        }
    }

    private fun classifyCompressed(
        frame: CameraFrame,
        w: Int,
        h: Int,
        size: Int,
        bpp: Float,
        rowSize: Int
    ): FormatDiagnostic {
        // Extract hex signature for diagnostic purposes
        val sigBytes = minOf(HEX_SIGNATURE_BYTES, frame.data.size)
        val hexSig = frame.data.take(sigBytes).joinToString("") { "%02X".format(it) }

        // Identify potential codec/format from byte signature (diagnostic hint only)
        val formatLabel = identifyCompressedHint(frame.data)

        return FormatDiagnostic(
            formatLabel = formatLabel,
            width = w,
            height = h,
            byteLength = size,
            bytesPerPixel = bpp,
            rowSize = rowSize,
            isCompressed = true,
            hexSignature = hexSig,
            verified = false
        )
    }

    /**
     * Inspect byte signature for common compressed format indicators.
     * This is a diagnostic HINT only — not a definitive codec identification.
     */
    private fun identifyCompressedHint(data: ByteArray): String {
        if (data.size < 4) return "COMPRESSED_UNKNOWN"

        // JPEG: starts with FF D8 FF
        if (data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte()) {
            return "COMPRESSED_JPEG_HINT"
        }

        // PNG: starts with 89 50 4E 47
        if (data[0] == 0x89.toByte() && data[1] == 0x50.toByte() &&
            data[2] == 0x4E.toByte() && data[3] == 0x47.toByte()
        ) {
            return "COMPRESSED_PNG_HINT"
        }

        // HEVC NAL unit: starts with 00 00 00 01 (or 00 00 01) + NAL type
        if (data.size >= 5 &&
            data[0] == 0x00.toByte() && data[1] == 0x00.toByte() &&
            data[2] == 0x00.toByte() && data[3] == 0x01.toByte()
        ) {
            return "COMPRESSED_NAL_HINT"
        }
        if (data.size >= 4 &&
            data[0] == 0x00.toByte() && data[1] == 0x00.toByte() && data[2] == 0x01.toByte()
        ) {
            return "COMPRESSED_NAL_SHORT_HINT"
        }

        return "COMPRESSED_UNKNOWN"
    }

    private fun classifyRaw(
        frame: CameraFrame,
        w: Int,
        h: Int,
        size: Int,
        bpp: Float,
        rowSize: Int
    ): FormatDiagnostic {
        val expectedYuv420 = (w * h * 3) / 2  // 1.5 bytes per pixel
        val expectedRgba = w * h * 4           // 4 bytes per pixel

        val formatLabel = when {
            w <= 0 || h <= 0 -> "UNKNOWN_INVALID_DIMENSIONS"
            size == expectedYuv420 -> "YUV420_FAMILY"  // Could be NV21, NV12, or I420
            size == expectedRgba -> "RGBA_CANDIDATE"
            else -> "UNKNOWN_RAW"
        }

        return FormatDiagnostic(
            formatLabel = formatLabel,
            width = w,
            height = h,
            byteLength = size,
            bytesPerPixel = bpp,
            rowSize = rowSize,
            isCompressed = false,
            hexSignature = null,
            verified = false
        )
    }

    private fun logDiagnostic(diag: FormatDiagnostic) {
        Log.i(
            TAG,
            buildString {
                append("=== Gen2 CameraFrame Format Diagnostic ===\n")
                append("  format: ${diag.formatLabel}\n")
                append("  isCompressed: ${diag.isCompressed}\n")
                append("  dimensions: ${diag.width}x${diag.height}\n")
                append("  byteLength: ${diag.byteLength}\n")
                append("  bytesPerPixel: ${"%.3f".format(diag.bytesPerPixel)}\n")
                append("  rowSize: ${diag.rowSize}\n")
                if (diag.hexSignature != null) {
                    append("  hexSignature: ${diag.hexSignature}\n")
                }
                append("  verified: ${diag.verified}\n")
                append("===========================================")
            }
        )

        if (!diag.verified) {
            Log.w(
                TAG,
                "Format is NOT verified on real Gen2. " +
                    "Do NOT treat '${diag.formatLabel}' as confirmed until real-device testing."
            )
        }
    }
}
