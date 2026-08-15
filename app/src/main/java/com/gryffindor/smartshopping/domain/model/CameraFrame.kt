package com.gryffindor.smartshopping.domain.model

/**
 * App-owned representation of a single camera frame.
 *
 * [data] contains frame bytes copied from the SDK-owned buffer into app memory.
 * After construction, the original SDK buffer may be invalidated without affecting this object.
 *
 * This type contains NO Meta DAT SDK references and is safe to use across all layers.
 */
data class CameraFrame(
    /** App-owned frame bytes. Never references SDK-managed memory. */
    val data: ByteArray,
    /** Frame width in pixels. */
    val width: Int,
    /** Frame height in pixels. */
    val height: Int,
    /** Presentation timestamp in microseconds, derived from DAT frame timestamp. */
    val timestampUs: Long,
    /** Whether the frame data is compressed (e.g. HEVC) or raw pixel data. */
    val isCompressed: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraFrame) return false
        return data.contentEquals(other.data) &&
            width == other.width &&
            height == other.height &&
            timestampUs == other.timestampUs &&
            isCompressed == other.isCompressed
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + timestampUs.hashCode()
        result = 31 * result + isCompressed.hashCode()
        return result
    }
}
