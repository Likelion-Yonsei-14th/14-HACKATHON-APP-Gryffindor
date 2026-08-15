package com.gryffindor.smartshopping.domain.camera

import com.gryffindor.smartshopping.domain.model.CameraFrame
import com.gryffindor.smartshopping.domain.model.CameraState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * SDK-independent camera boundary.
 *
 * Downstream components (A2 Object Detection, ViewModels) depend only on this interface.
 * The concrete implementation ([MetaCameraSource] in `data/meta/`) owns the Meta DAT lifecycle.
 *
 * No Meta DAT imports appear in this contract.
 */
interface CameraFrameProvider {

    /** Stream of app-owned camera frames. Bounded; stale frames may be dropped. */
    val frames: Flow<CameraFrame>

    /** Current camera pipeline state. */
    val cameraState: StateFlow<CameraState>

    /** Start the camera pipeline. Creates a new DeviceSession if none is active. */
    suspend fun startCamera()

    /** Stop the camera pipeline. Releases all DAT resources. Safe to call repeatedly. */
    suspend fun stopCamera()
}
