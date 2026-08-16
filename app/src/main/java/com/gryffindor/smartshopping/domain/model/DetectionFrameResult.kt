package com.gryffindor.smartshopping.domain.model

/**
 * SDK-independent result of object detection for a single processed frame.
 *
 * Contains zero or more [DetectionResult] instances produced from the source CameraFrame.
 * An empty [detections] list means no objects were detected above the confidence threshold.
 *
 * No ML SDK types are referenced by this class.
 */
data class DetectionFrameResult(
    /** Timestamp of the source CameraFrame in microseconds. */
    val frameTimestampUs: Long,
    /** Detected objects in this frame. May be empty. Never null. */
    val detections: List<DetectionResult>
)
