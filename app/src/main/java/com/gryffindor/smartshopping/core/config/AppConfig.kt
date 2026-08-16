package com.gryffindor.smartshopping.core.config

/**
 * Application-level configuration.
 * All configurable thresholds and parameters are defined here — no magic numbers in logic code.
 */
object AppConfig {

    // ===== Detection Pipeline =====

    /** Minimum interval between frame deliveries to detector (ms). Range: 66–1000. */
    const val DETECTION_FRAME_INTERVAL_MS: Long = 200L  // ~5 FPS target detection rate

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

    /** Metrics summary log interval (ms). */
    const val DETECTION_METRICS_SUMMARY_INTERVAL_MS: Long = 5000L

    /** Maximum samples retained for p95 latency calculation. */
    const val DETECTION_METRICS_MAX_SAMPLES: Int = 200

    // ===== Attention Pipeline — Center ROI =====

    /** Normalized left boundary of center ROI (horizontal central 70%). */
    const val ATTENTION_CENTER_ROI_LEFT: Float = 0.15f

    /** Normalized right boundary of center ROI. */
    const val ATTENTION_CENTER_ROI_RIGHT: Float = 0.85f

    /** Normalized top boundary of center ROI (vertical central 80%). */
    const val ATTENTION_CENTER_ROI_TOP: Float = 0.10f

    /** Normalized bottom boundary of center ROI. */
    const val ATTENTION_CENTER_ROI_BOTTOM: Float = 0.90f

    // ===== Attention Pipeline — Thresholds =====

    /** Minimum occupancy ratio (bbox area / frame area) to qualify for attention. */
    const val ATTENTION_MIN_OCCUPANCY_RATIO: Float = 0.12f

    /** Minimum continuous dwell time (ms) for attention trigger. */
    const val ATTENTION_MIN_DWELL_MS: Long = 600L

    /** Strong occupancy evidence eligible for the guarded fast attention path. */
    const val ATTENTION_FAST_OCCUPANCY_RATIO: Float = 0.22f

    /** Continuous strong-occupancy stability required before fast-path trigger. */
    const val ATTENTION_FAST_OCCUPANCY_STABILITY_MS: Long = 200L

    // ===== Attention Pipeline — Tracking =====

    /** Maximum normalized center distance for associating a detection with an existing track. */
    const val TRACKING_MAX_CENTER_DISTANCE: Float = 0.20f

    /** Grace period (ms) to retain an unmatched track before removal. Uses frameTimestampUs. */
    const val TRACKING_GRACE_PERIOD_MS: Long = 500L

    // ===== Attention Pipeline — Crop =====

    /** Padding ratio applied relative to bbox size when cropping. */
    const val ATTENTION_CROP_PADDING_RATIO: Float = 0.15f

    /** Minimum short-side pixel length for a valid crop. */
    const val ATTENTION_MIN_CROP_SHORT_SIDE: Int = 160

    /** Maximum long-side pixel length; proportional downscale if exceeded. */
    const val ATTENTION_MAX_CROP_LONG_SIDE: Int = 1024

    // ===== Attention Pipeline — JPEG =====

    /** JPEG encoding quality [0–100]. */
    const val ATTENTION_JPEG_QUALITY: Int = 85

    // ===== Attention Pipeline — Source Frame Cache =====

    /** Maximum number of recent CameraFrames retained for timestamp matching. */
    const val SOURCE_FRAME_CACHE_MAX_SIZE: Int = 15

    // ===== Attention Pipeline — Dwell Gap Protection =====

    /** Maximum gap between consecutive frame timestamps (ms) counted as continuous dwell. */
    const val ATTENTION_MAX_DWELL_GAP_MS: Long = 2000L
}
