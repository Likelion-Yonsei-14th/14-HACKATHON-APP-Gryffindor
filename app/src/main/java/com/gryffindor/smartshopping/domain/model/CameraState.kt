package com.gryffindor.smartshopping.domain.model

/**
 * SDK-independent representation of the camera pipeline state.
 *
 * No Meta DAT SDK types, exceptions, or enums are exposed through this sealed class.
 */
sealed class CameraState {

    /** No active camera pipeline. Initial state and state after successful stop. */
    data object NotConnected : CameraState()

    /** DeviceSession / camera setup is in progress. */
    data object Connecting : CameraState()

    /** DeviceSession and camera capability are ready, stream not yet started. */
    data object Ready : CameraState()

    /** Camera stream is active and producing frames. */
    data object Streaming : CameraState()

    /** Current camera pipeline failed but a fresh start may recover. */
    data class RecoverableError(val message: String) : CameraState()

    /** User or device intervention is required before retry. */
    data class BlockingError(val message: String) : CameraState()

    /** DAT glasses app update is required before camera can function. */
    data class DatUpdateRequired(val message: String) : CameraState()
}
