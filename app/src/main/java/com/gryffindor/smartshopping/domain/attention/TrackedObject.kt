package com.gryffindor.smartshopping.domain.attention

/**
 * Represents a tracked object across frames.
 * Contains the current spatial state and identity used for dwell accumulation.
 *
 * No Meta DAT, TFLite, or Retrofit types referenced.
 */
data class TrackedObject(
    /** Stable identifier for this track. */
    val trackingId: String,
    /** Normalized bbox center X [0.0, 1.0]. */
    val centerX: Float,
    /** Normalized bbox center Y [0.0, 1.0]. */
    val centerY: Float,
    /** Normalized bbox area (width * height in [0.0, 1.0] space). */
    val area: Float,
    /** Detection label (e.g., "handbag"). */
    val label: String,
    /** Frame timestamp in microseconds when this track was last observed. */
    val lastSeenTimestampUs: Long,
    /** Normalized bounding box coordinates from detection. */
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
