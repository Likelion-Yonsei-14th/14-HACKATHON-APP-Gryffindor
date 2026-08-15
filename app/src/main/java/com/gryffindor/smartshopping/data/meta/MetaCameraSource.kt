package com.gryffindor.smartshopping.data.meta

import android.util.Log
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.model.CameraFrame
import com.gryffindor.smartshopping.domain.model.CameraState
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    }

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

    // --- DAT pipeline ---

    /**
     * Runs the full DAT camera pipeline.
     * This follows the lifecycle validated in Stage 0:
     *   Wearables.createSession() → session.start() → session.addCamera() →
     *   camera.stream.start() → camera.stream.videoStream.collect()
     *
     * On cancellation or failure, the finally block triggers ordered cleanup.
     */
    private suspend fun runCameraPipeline() {
        try {
            // Step 1: Create and start DeviceSession
            val session = createAndStartSession()
            activeSession = session

            // Step 2: Add camera capability
            val camera = addCameraCapability(session)
            activeCamera = camera

            _cameraState.value = CameraState.Ready

            // Step 3: Start the camera stream
            startStream(camera)

            _cameraState.value = CameraState.Streaming

            // Step 4: Collect frames — suspends until cancellation or stream end
            collectFrames(camera)

        } catch (e: CancellationException) {
            // Normal cancellation (stopCamera called) — propagate
            throw e
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
     * Create and start a DeviceSession using the validated Stage 0 pattern:
     *   Wearables.createSession(AutoDeviceSelector()) → session.start()
     *
     * Wearables.createSession() is non-suspend; returns DatResult.
     */
    private fun createAndStartSession(): DeviceSession {
        val deviceSelector = AutoDeviceSelector()
        val result = Wearables.createSession(deviceSelector)

        val session = result.getOrNull()
            ?: throw RuntimeException(
                "Failed to create DeviceSession: ${result.errorOrNull()?.description ?: "unknown error"}"
            )

        session.start()
        return session
    }

    /**
     * Add camera capability using the validated Stage 0 pattern:
     *   session.addCamera(StreamConfiguration(...))
     *
     * session.addCamera() is non-suspend; returns DatResult.
     */
    private fun addCameraCapability(session: DeviceSession): Camera {
        val config = StreamConfiguration(
            videoQuality = videoQuality,
            frameRate = frameRate,
            compressVideo = false
        )
        val result = session.addCamera(config)

        return result.getOrNull()
            ?: throw RuntimeException(
                "Failed to add camera: ${result.errorOrNull()?.description ?: "unknown error"}"
            )
    }

    /**
     * Start the camera stream using the validated Stage 0 pattern:
     *   camera.stream.start()
     *
     * stream.start() is non-suspend; returns DatResult.
     */
    private fun startStream(camera: Camera) {
        val stream = camera.stream
        val result = stream.start()

        if (!result.isSuccess) {
            throw RuntimeException(
                "Failed to start stream: ${result.errorOrNull()?.description ?: "unknown error"}"
            )
        }
    }

    /**
     * Collect VideoFrames and convert to app-owned CameraFrame.
     *
     * For each frame, we perform the minimum work inside the collection scope:
     *   1. Read metadata
     *   2. Copy SDK-owned ByteBuffer → app-owned ByteArray (via duplicate to preserve position)
     *   3. Construct CameraFrame
     *   4. Bounded emit (tryEmit with DROP_OLDEST)
     */
    private suspend fun collectFrames(camera: Camera) {
        camera.stream.videoStream.collect { videoFrame: VideoFrame ->
            val frame = transferFrameOwnership(videoFrame)
            _frames.tryEmit(frame)
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
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping session", e)
            }
        }
    }
}
