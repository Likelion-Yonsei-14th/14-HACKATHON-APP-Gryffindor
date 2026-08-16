package com.gryffindor.smartshopping.data.detection

/**
 * Structured diagnostic record for CameraFrame format classification.
 *
 * This record captures what is observable from a single CameraFrame without
 * assuming a specific pixel layout or codec. The [verified] flag remains false
 * until empirical confirmation on real Gen2 hardware.
 */
internal data class FormatDiagnostic(
    /** Classification result, e.g. "YUV420_FAMILY", "RGBA", "COMPRESSED", "UNKNOWN" */
    val formatLabel: String,
    /** Frame width in pixels */
    val width: Int,
    /** Frame height in pixels */
    val height: Int,
    /** Total byte length of CameraFrame.data */
    val byteLength: Int,
    /** Computed as byteLength / (width * height). Useful for format family identification. */
    val bytesPerPixel: Float,
    /** Approximate row size: byteLength / height */
    val rowSize: Int,
    /** Whether the frame is compressed */
    val isCompressed: Boolean,
    /** Hex signature of the first bytes (for compressed frames) */
    val hexSignature: String?,
    /** Whether this format has been verified on real Gen2 hardware */
    val verified: Boolean = false
)
