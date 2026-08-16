package com.gryffindor.smartshopping.core.config

/**
 * Application-level configuration.
 * All configurable thresholds and parameters are defined here — no magic numbers in logic code.
 */
object AppConfig {

    // ===== Detection Pipeline =====

    /** Minimum interval between frame deliveries to detector (ms). Range: 66–1000. */
    const val DETECTION_FRAME_INTERVAL_MS: Long = 100L  // ~10 FPS max detection rate

    /** Minimum confidence threshold for emitting detections. */
    const val DETECTION_CONFIDENCE_THRESHOLD: Float = 0.3f

    /** Maximum detections per frame forwarded to downstream. */
    const val DETECTION_MAX_PER_FRAME: Int = 20

    /** Inference timeout per frame (ms). Frame discarded if exceeded. */
    const val DETECTION_INFERENCE_TIMEOUT_MS: Long = 200L

    /** Maximum reusable conversion buffers. */
    const val DETECTION_MAX_BUFFERS: Int = 3

    // ===== Format Verification =====

    /**
     * Whether the assumed frame format has been verified on real Gen2.
     * This flag MUST remain false until empirical testing on actual hardware confirms the format.
     */
    const val DETECTION_FORMAT_VERIFIED: Boolean = false

    // ===== Metrics =====

    /** Rolling window for FPS calculation (ms). */
    const val DETECTION_METRICS_WINDOW_MS: Long = 5000L
}
