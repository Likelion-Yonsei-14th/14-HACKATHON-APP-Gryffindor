package com.gryffindor.smartshopping.domain.model

/**
 * Final A3 output: a cropped JPEG image with recognition metadata,
 * ready for Backend /recognize in A4.
 *
 * No Meta DAT, TFLite, Retrofit, or Backend DTO types referenced.
 */
data class AttentionCandidate(
    /** JPEG-encoded crop of the attended object. */
    val jpegBytes: ByteArray,
    /** Wall-clock capture time in ISO 8601 UTC (e.g., "2026-08-16T12:34:56.789Z"). */
    val capturedAt: String,
    /** Trigger policy that fired. */
    val triggerType: TriggerType,
    /** Occupancy ratio at trigger time. */
    val occupancyRatio: Float,
    /** Accumulated dwell time at trigger (ms). */
    val dwellMs: Long,
    /** Lightweight tracking ID, if available. */
    val trackingId: String?,
    /** Width of the cropped image in pixels. */
    val cropWidth: Int?,
    /** Height of the cropped image in pixels. */
    val cropHeight: Int?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AttentionCandidate) return false
        return jpegBytes.contentEquals(other.jpegBytes) &&
            capturedAt == other.capturedAt &&
            triggerType == other.triggerType &&
            occupancyRatio == other.occupancyRatio &&
            dwellMs == other.dwellMs &&
            trackingId == other.trackingId &&
            cropWidth == other.cropWidth &&
            cropHeight == other.cropHeight
    }

    override fun hashCode(): Int {
        var result = jpegBytes.contentHashCode()
        result = 31 * result + capturedAt.hashCode()
        result = 31 * result + triggerType.hashCode()
        result = 31 * result + occupancyRatio.hashCode()
        result = 31 * result + dwellMs.hashCode()
        result = 31 * result + (trackingId?.hashCode() ?: 0)
        result = 31 * result + (cropWidth ?: 0)
        result = 31 * result + (cropHeight ?: 0)
        return result
    }
}
