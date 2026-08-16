package com.gryffindor.smartshopping.domain.detection

import com.gryffindor.smartshopping.domain.model.DetectionFrameResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * SDK-independent domain interface for object detection output.
 *
 * Downstream components (ViewModel, Attention Policy) depend only on this interface.
 * No ML SDK types, lifecycle methods, or detector internals are exposed.
 *
 * The concrete implementation lives in `data/detection/` and is wired via AppContainer.
 */
interface DetectionResultProvider {

    /** Stream of detection results. One emission per processed frame. */
    val detections: Flow<DetectionFrameResult>

    /** Current pipeline state. */
    val pipelineState: StateFlow<DetectionPipelineState>
}

/**
 * Observable state of the detection pipeline.
 * Allows ViewModel to show detection status without coupling to ML SDK internals.
 */
sealed class DetectionPipelineState {
    /** Detection pipeline is not active. */
    data object Idle : DetectionPipelineState()

    /** Detection pipeline is actively processing frames. */
    data object Running : DetectionPipelineState()

    /** Detection pipeline encountered an error. */
    data class Error(val message: String) : DetectionPipelineState()
}
