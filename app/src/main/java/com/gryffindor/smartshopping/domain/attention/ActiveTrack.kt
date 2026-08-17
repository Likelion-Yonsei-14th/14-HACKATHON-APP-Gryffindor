package com.gryffindor.smartshopping.domain.attention

/**
 * An active tracked object combined with its current dwell accumulation state.
 *
 * No Meta DAT, TFLite, or Retrofit types referenced.
 */
data class ActiveTrack(
    /** The tracked object spatial state. */
    val trackedObject: TrackedObject,
    /** Current dwell accumulation for this track. */
    val dwellState: DwellState
)
