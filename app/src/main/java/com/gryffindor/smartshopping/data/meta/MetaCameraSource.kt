package com.gryffindor.smartshopping.data.meta

import android.util.Log
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.camera.GlassesUpdateResult
import com.gryffindor.smartshopping.domain.model.CameraFrame
import com.gryffindor.smartshopping.domain.model.CameraState
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.Stream
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamState
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.DeviceSessionError
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Meta DAT camera adapter.
 *
 * Sole owner of the Meta DAT camera integration inside `data/meta/`.
 * Converts SDK-owned [VideoFrame] into app-owned [CameraFrame] and exposes them
 * through the SDK-independent [CameraFrameProvider] boundary.
 *
 * All Meta SDK types remain private within this class.
 */
class MetaCameraSource(
    private val frameRate: Int = DEFAULT_FRAME_RATE,
    private val videoQuality: VideoQuality = VideoQuality.MEDIUM
) : CameraFrameProvider {

    companion object {
        private const val TAG = "MetaCameraSource"
        private const val DEFAULT_FRAME_RATE = 15
        private const val SESSION_START_TIMEOUT_MS = 15_000L
    }

    /**
     * Set by the Activity after creation. Used to request DAT camera permission
     * via Meta AI when permission is not yet granted.
     */
    var permissionRequester: WearablePermissionRequester? = null

    /**
     * Set by the Activity after creation. Used to open the DAT glasses app update
     * flow when the glasses DAT app is outdated.
     */
    var updateRequester: WearableUpdateRequester? = null

    // --- SDK-independent public state ---

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.NotConnected)
    override val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _frames = MutableSharedFlow<CameraFrame>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val frames: Flow<CameraFrame> = _frames.asSharedFlow()

    // --- Private DAT resource references ---

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lifecycleMutex = Mutex()

    private var streamingJob: Job? = null
    private var activeSession: DeviceSession? = null
    private var activeCamera: Camera? = null

    // --- CameraFrameProvider implementation ---

    override suspend fun startCamera() {
        lifecycleMutex.withLock {
            // Already streaming — no-op
            if (streamingJob?.isActive == true) {
                return
            }

            // Clean up stale resources from any earlier failed lifecycle
            cleanupResourcesInOrder()

            _cameraState.value = CameraState.Connecting

            streamingJob = scope.launch {
                runCameraPipeline()
            }
        }
    }

    override suspend fun stopCamera() {
        lifecycleMutex.withLock {
            val job = streamingJob
            streamingJob = null

            // Cancel the streaming coroutine and wait for its completion.
            // The finally block inside runCameraPipeline will handle cleanup,
            // but we also run cleanup here to guarantee resources are released
            // even if the job was already cancelled/completed.
            job?.cancelAndJoin()

            // Ensure ordered cleanup after job termination
            withContext(Dispatchers.Default) {
                cleanupResourcesInOrder()
            }

            _cameraState.value = CameraState.NotConnected
        }
    }

    override suspend fun openGlassesUpdate(): GlassesUpdateResult {
        val requester = updateRequester
            ?: return GlassesUpdateResult.Unsupported
        return requester.openDatGlassesUpdate()
    }

    // --- DAT pipeline ---

    /**
     * Runs the full DAT camera pipeline.
     * Key fix: waits for DeviceSessionState.STARTED before adding camera.
     *
     * Pipeline:
     *   Wearables.createSession() → session.start() → await STARTED →
     *   session.addCamera() → stream.start() → videoStream.collect()
     *
     * On cancellation or failure, the finally block triggers ordered cleanup.
     */
    private suspend fun runCameraPipeline() {
        try {
            // Step 1: Create DeviceSession
            val session = createSession()
            activeSession = session

            // Step 2: Start session and await STARTED state
            awaitSessionStarted(session)

            // Step 3: Check/request DAT camera permission
            ensureCameraPermission()

            // Step 4: Add camera capability (only after STARTED + permission)
            val camera = addCameraCapability(session)
            activeCamera = camera

            _cameraState.value = CameraState.Ready

            // Step 5: Start the camera stream
            startStream(camera.stream)

            _cameraState.value = CameraState.Streaming

            // Step 6: Collect frames — suspends until cancellation or stream end
            collectFrames(camera)

        } catch (e: CancellationException) {
            // Normal cancellation (stopCamera called) — propagate
            throw e
        } catch (e: DatUpdateRequiredException) {
            Log.w(TAG, "DAT glasses app update required", e)
            _cameraState.value = CameraState.DatUpdateRequired(
                "스마트글래스 앱 업데이트가 필요합니다"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera pipeline error", e)
            _cameraState.value = CameraState.RecoverableError(
                e.message ?: "Camera pipeline failed"
            )
        } finally {
            // Single idempotent cleanup path — shared by normal stop, cancellation,
            // partial startup failure, and stream failure.
            cleanupResourcesInOrder()
        }
    }

    /**
     * Create a DeviceSession using AutoDeviceSelector.
     * Does NOT call start() — that's handled separately to allow state observation.
     */
    private fun createSession(): DeviceSession {
        if (!WearablesInitializer.isInitialized) {
            throw RuntimeException("Wearables SDK not initialized — call WearablesInitializer.initialize() first")
        }

        // Diagnostic: log registration/device state before session creation
        Log.i(TAG, "createSession: logging pre-start diagnostics")
        WearablesInitializer.logDiagnostics()

        val deviceSelector = AutoDeviceSelector()
        val result = Wearables.createSession(deviceSelector)

        val session = result.getOrNull()
            ?: throw RuntimeException(
                "Failed to create DeviceSession: ${result.errorOrNull()?.description ?: "unknown error"}"
            )

        Log.i(TAG, "DeviceSession created")
        return session
    }

    /**
     * Start the session and suspend until DeviceSessionState.STARTED is reached.
     *
     * Handles:
     * - Normal IDLE → STARTING → STARTED transition
     * - STOPPED arrival → treated as startup failure
     * - STOPPED + DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED → DatUpdateRequiredException
     * - Timeout → treated as startup failure
     *
     * Concurrently observes session.errors to detect DAT update requirement.
     * Does NOT block the main thread — uses Flow observation.
     */
    private suspend fun awaitSessionStarted(session: DeviceSession) {
        Log.i(TAG, "Session start requested, current state=${session.state.value}")
        session.start()
        Log.i(TAG, "session.start() called (fire-and-forget), awaiting STARTED...")

        // Atomic flag set by the error observer if DAT update error is detected
        val datUpdateErrorDetected = AtomicBoolean(false)

        // Launch a side coroutine to observe session.errors concurrently
        val errorObserverJob = scope.launch {
            session.errors.collect { error ->
                Log.w(TAG, "Session error received: $error")
                if (error == DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED) {
                    Log.w(TAG, "DAT glasses app update required error detected")
                    datUpdateErrorDetected.set(true)
                }
            }
        }

        try {
            val reachedState = withTimeoutOrNull(SESSION_START_TIMEOUT_MS) {
                // first { } suspends until a matching emission
                session.state.first { state ->
                    Log.i(TAG, "Session state=$state")
                    state == DeviceSessionState.STARTED || state == DeviceSessionState.STOPPED
                }
            }

            when (reachedState) {
                DeviceSessionState.STARTED -> {
                    Log.i(TAG, "Session reached STARTED")
                }
                DeviceSessionState.STOPPED -> {
                    // Check if the STOPPED was caused by a DAT update requirement
                    if (datUpdateErrorDetected.get()) {
                        throw DatUpdateRequiredException()
                    }
                    throw RuntimeException("Session reached STOPPED during startup — device may have disconnected")
                }
                null -> {
                    throw RuntimeException("Session start timeout (${SESSION_START_TIMEOUT_MS}ms) — stuck in state=${session.state.value}")
                }
                else -> {
                    throw RuntimeException("Unexpected session state during startup: $reachedState")
                }
            }
        } finally {
            // Always cancel the error observer to prevent leaks
            errorObserverJob.cancel()
        }
    }

    /**
     * Check DAT camera permission. If not granted, request it via the injected requester.
     * Throws if permission is denied or no requester is available.
     *
     * Per official DAT docs:
     *   Wearables.checkPermissionStatus(Permission.CAMERA) → PermissionStatus.Granted/Denied
     *   If not granted, launch Wearables.RequestPermissionContract via Activity.
     */
    private suspend fun ensureCameraPermission() {
        Log.i(TAG, "Checking DAT camera permission...")

        val checkResult = Wearables.checkPermissionStatus(Permission.CAMERA)
        val currentStatus = checkResult.getOrNull()

        if (currentStatus is PermissionStatus.Granted) {
            Log.i(TAG, "DAT camera permission already granted")
            return
        }

        Log.i(TAG, "DAT camera permission not granted (status=$currentStatus), requesting...")

        val requester = permissionRequester
            ?: throw RuntimeException(
                "DAT camera permission denied and no permissionRequester available. " +
                "Ensure MetaCameraSource is created with a WearablePermissionRequester."
            )

        val granted = requester.requestCameraPermission()
        if (granted) {
            Log.i(TAG, "DAT camera permission granted by user")
        } else {
            throw RuntimeException("DAT camera permission denied by user")
        }
    }

    /**
     * Add camera capability using the extension:
     *   session.addCamera(StreamConfiguration(...))
     *
     * Must only be called after session reaches STARTED.
     */
    private fun addCameraCapability(session: DeviceSession): Camera {
        val config = StreamConfiguration(
            videoQuality = videoQuality,
            frameRate = frameRate
        )
        Log.i(TAG, "Adding camera capability (quality=$videoQuality, fps=$frameRate)")
        val result = session.addCamera(config)

        val camera = result.getOrNull()
        if (camera != null) {
            Log.i(TAG, "Camera capability added successfully")
            return camera
        }

        val error = result.errorOrNull()
        throw RuntimeException(
            "Failed to add camera: ${error?.description ?: "unknown error"}"
        )
    }

    /**
     * Start the camera stream and log state transitions.
     */
    private fun startStream(stream: Stream) {
        Log.i(TAG, "Stream start requested, current streamState=${stream.state.value}")
        val result = stream.start()

        if (!result.isSuccess) {
            throw RuntimeException(
                "Failed to start stream: ${result.errorOrNull()?.description ?: "unknown error"}"
            )
        }
        Log.i(TAG, "Stream start() called successfully")
    }

    /**
     * Collect VideoFrames and convert to app-owned CameraFrame.
     * Also observes stream state transitions for diagnostics.
     */
    private suspend fun collectFrames(camera: Camera) {
        val stream = camera.stream

        // Launch a diagnostic observer for stream state (non-blocking)
        val stateObserverJob = scope.launch {
            stream.state.collect { state ->
                Log.i(TAG, "Stream state=$state")
            }
        }

        try {
            var frameCount = 0L
            stream.videoStream.collect { videoFrame: VideoFrame ->
                val frame = transferFrameOwnership(videoFrame)
                _frames.tryEmit(frame)
                frameCount++
                if (frameCount == 1L) {
                    Log.i(TAG, "VideoFrame received (first frame: ${frame.width}x${frame.height})")
                    Log.i(TAG, "CameraFrame emitted to downstream")
                }
                if (frameCount % 100 == 0L) {
                    Log.d(TAG, "Frames emitted: $frameCount")
                }
            }
        } finally {
            stateObserverJob.cancel()
        }
    }

    /**
     * Safely transfer VideoFrame data into app-owned memory.
     *
     * Uses buffer.duplicate() to avoid mutating the original SDK buffer position.
     * The resulting ByteArray is independent of the SDK-managed frame lifetime.
     */
    private fun transferFrameOwnership(videoFrame: VideoFrame): CameraFrame {
        val source = videoFrame.buffer.duplicate()
        val bytes = ByteArray(source.remaining())
        source.get(bytes)

        return CameraFrame(
            data = bytes,
            width = videoFrame.width,
            height = videoFrame.height,
            timestampUs = videoFrame.presentationTimeUs,
            isCompressed = videoFrame.isCompressed
        )
    }

    // --- Private exception for DAT update requirement ---

    /**
     * Internal exception thrown when DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED is detected.
     * Caught in runCameraPipeline() to set the correct CameraState.
     */
    private class DatUpdateRequiredException : RuntimeException(
        "DAT glasses app update required"
    )

    // --- Ordered cleanup ---

    /**
     * Single idempotent cleanup path. Follows the lifecycle order validated in Stage 0:
     *   1. Camera stop/close (cascades to stream stop)
     *   2. DeviceSession stop
     *   3. Clear references
     *
     * Tolerates partially initialized resources.
     * A failure cleaning one resource does not prevent the remaining cleanup.
     * A stopped DeviceSession is never reused — the next start creates a new one.
     */
    private fun cleanupResourcesInOrder() {
        // Step 1: Close camera (cascades to stream stop, releases capability)
        val camera = activeCamera
        activeCamera = null
        if (camera != null) {
            try {
                camera.close()
                Log.d(TAG, "Camera closed")
            } catch (e: Exception) {
                Log.w(TAG, "Error closing camera", e)
            }
        }

        // Step 2: Stop DeviceSession
        val session = activeSession
        activeSession = null
        if (session != null) {
            try {
                session.stop()
                Log.d(TAG, "Session stopped")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping session", e)
            }
        }
    }
}
