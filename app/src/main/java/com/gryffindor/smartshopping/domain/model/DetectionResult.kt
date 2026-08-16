package com.gryffindor.smartshopping.domain.model

/**
 * SDK-independent representation of a single detected object.
 *
 * Bounding box coordinates are normalized to [0.0, 1.0] relative to the source frame dimensions.
 * Coordinate system: (0.0, 0.0) = top-left, (1.0, 1.0) = bottom-right.
 *
 * No ML SDK types are referenced by this class.
 */
data class DetectionResult(
    /** Normalized left edge [0.0, 1.0]. */
    val left: Float,
    /** Normalized top edge [0.0, 1.0]. */
    val top: Float,
    /** Normalized right edge [0.0, 1.0]. */
    val right: Float,
    /** Normalized bottom edge [0.0, 1.0]. */
    val bottom: Float,
    /** Detected object category (e.g., "handbag", "bottle"). Non-empty. */
    val label: String,
    /** Detection confidence in [0.0, 1.0]. */
    val confidence: Float
)
