package com.gryffindor.smartshopping.data.detection

import android.content.Context
import android.util.Log
import com.gryffindor.smartshopping.core.config.AppConfig
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionPipelineState
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.domain.model.DetectionFrameResult
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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Orchestrates the full object detection pipeline:
 *   CameraFrameProvider.frames → sampling → format verification → conversion → inference → filtering → emission
 *
 * Lifecycle is driven by CameraState: starts when Streaming, stops otherwise.
 * All heavy work runs on Dispatchers.Default — never blocks Main.
 *
 * Implements [DetectionResultProvider] so downstream (ViewModel) depends only on domain interface.
 */
internal class DetectionPipeline(
    private val cameraFrameProvider: CameraFrameProvider,
    private val context: Context
) : DetectionResultProvider {

    companion object {
        private const val TAG = "DetectionPipeline"
    }

    // --- Output flows ---

    private val _detections = MutableSharedFlow<DetectionFrameResult>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val detections: Flow<DetectionFrameResult> = _detections.asSharedFlow()

    private val _pipelineState = MutableStateFlow<DetectionPipelineState>(DetectionPipelineState.Idle)
    override val pipelineState: StateFlow<DetectionPipelineState> = _pipelineState.asStateFlow()

    // --- Internal components ---

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lifecycleMutex = Mutex()

    private val frameSampler = FrameSampler()
    private val formatVerifier = FormatVerifier()
    private val frameConverter = FrameConverter()
    private val detector = TFLiteDetectorAdapter(context)
    private val metrics = DetectionMetrics()

    private var detectionJob: Job? = null

    init {
        // Observe camera state to auto-start/stop detection
        scope.launch {
            cameraFrameProvider.cameraState.collect { state ->
                when (state) {
                    is CameraState.Streaming -> startDetection()
                    else -> stopDetection()
                }
            }
        }
    }

    // --- Lifecycle ---

    private suspend fun startDetection() {
        lifecycleMutex.withLock {
            // Already running
            if (detectionJob?.isActive == true) return

            Log.d(TAG, "Starting detection pipeline")

            // Initialize detector
            val initialized = detector.initialize()
            if (!initialized) {
                Log.e(TAG, "Failed to initialize TFLite detector")
                _pipelineState.value = DetectionPipelineState.Error("Model initialization failed")
                return
            }

            frameSampler.reset()
            metrics.reset()
            _pipelineState.value = DetectionPipelineState.Running

            detectionJob = scope.launch {
                runDetectionLoop()
            }
        }
    }

    private suspend fun stopDetection() {
        lifecycleMutex.withLock {
            val job = detectionJob
            if (job == null || !job.isActive) {
                // Ensure state is Idle if there's no active job
                if (_pipelineState.value is DetectionPipelineState.Running) {
                    _pipelineState.value = DetectionPipelineState.Idle
                }
                return
            }

            Log.d(TAG, "Stopping detection pipeline")
            detectionJob = null
            job.cancelAndJoin()

            detector.release()
            _pipelineState.value = DetectionPipelineState.Idle
        }
    }

    // --- Detection loop ---

    private suspend fun runDetectionLoop() {
        try {
            cameraFrameProvider.frames
                .conflate()
                .collect { frame ->
                    // Track every frame that reaches the pipeline (post-conflate)
                    metrics.recordFrameReceived()

                    // Rate limiting
                    if (!frameSampler.shouldProcess(frame)) return@collect

                    metrics.recordFrameSampled()

                    val totalStartNs = System.nanoTime()

                    // One-time format verification (logs diagnostics)
                    formatVerifier.verifyIfNeeded(frame)

                    // Convert CameraFrame → Bitmap
                    val conversionStartNs = System.nanoTime()
                    val bitmap = frameConverter.convert(frame)
                    val conversionMs = (System.nanoTime() - conversionStartNs) / 1_000_000

                    if (bitmap == null) {
                        Log.w(TAG, "Frame conversion failed, skipping frame ts=${frame.timestampUs}")
                        metrics.recordFrameDropped()
                        return@collect
                    }

                    // Run inference with timeout
                    val inferenceStartNs = System.nanoTime()
                    val rawResults = withTimeoutOrNull(AppConfig.DETECTION_INFERENCE_TIMEOUT_MS) {
                        detector.detect(bitmap)
                    }
                    val inferenceMs = (System.nanoTime() - inferenceStartNs) / 1_000_000

                    // Recycle bitmap after inference
                    bitmap.recycle()

                    if (rawResults == null) {
                        Log.w(TAG, "Inference timeout for frame ts=${frame.timestampUs}")
                        metrics.recordFrameDropped()
                        return@collect
                    }

                    // Filter by confidence and cap count
                    val filtered = rawResults
                        .filter { it.confidence >= AppConfig.DETECTION_CONFIDENCE_THRESHOLD }
                        .take(AppConfig.DETECTION_MAX_PER_FRAME)

                    val totalMs = (System.nanoTime() - totalStartNs) / 1_000_000

                    // Record successful processing metrics
                    metrics.recordFrameProcessed(conversionMs, inferenceMs, totalMs)

                    val result = DetectionFrameResult(
                        frameTimestampUs = frame.timestampUs,
                        detections = filtered
                    )

                    // Emit result
                    _detections.tryEmit(result)

                    // Log detection results for live verification
                    if (filtered.isNotEmpty()) {
                        Log.i(TAG, buildString {
                            append("Detection [${totalMs}ms] frame=${frame.timestampUs} count=${filtered.size}\n")
                            filtered.forEach { det ->
                                append("  → ${det.label} conf=${"%.2f".format(det.confidence)} " +
                                    "bbox(${("%.3f".format(det.left))}, ${("%.3f".format(det.top))}, " +
                                    "${("%.3f".format(det.right))}, ${("%.3f".format(det.bottom))})\n")
                            }
                        })
                    } else {
                        Log.d(TAG, "Detection [${totalMs}ms] frame=${frame.timestampUs} count=0 (no objects above threshold)")
                    }
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Detection loop error", e)
            _pipelineState.value = DetectionPipelineState.Error(e.message ?: "Detection loop failed")
        }
    }
}
