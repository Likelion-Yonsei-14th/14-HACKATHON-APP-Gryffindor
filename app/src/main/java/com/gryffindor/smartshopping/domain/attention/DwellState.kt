package com.gryffindor.smartshopping.domain.attention

/**
 * Dwell time accumulation state for a single tracked object.
 *
 * Dwell is accumulated based on frameTimestampUs (monotonic SDK timestamp),
 * NOT wall-clock time.
 *
 * No Meta DAT, TFLite, or Retrofit types referenced.
 */
data class DwellState(
    /** Accumulated dwell time in milliseconds while center+occupancy conditions are met. */
    val accumulatedDwellMs: Long = 0L,
    /** Frame timestamp (us) when the current continuous dwell period started. */
    val periodStartTimestampUs: Long = 0L,
    /** Frame timestamp (us) of the last qualifying frame in the current dwell period. */
    val lastQualifyingTimestampUs: Long = 0L,
    /** Whether the center and occupancy conditions were satisfied on the last evaluation. */
    val wasSatisfied: Boolean = false
)
