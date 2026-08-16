package com.gryffindor.smartshopping.data.attention

import android.util.Log
import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.domain.model.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Orchestrates the full A3 attention candidate pipeline:
 *
 *   Parallel Job 1: CameraFrameProvider.frames → SourceFrameCache.put()
 *   Parallel Job 2: DetectionResultProvider.detections → processDetectionFrame()
 *
 * processDetectionFrame flow:
 *   DetectionFrameResult
 *   → ObjectTracker.update(detections, frameTimestampUs)
 *   → AttentionEvaluator.evaluate(trackedObjects, frameTimestampUs)
 *   → IF triggered:
 *       → check duplicate suppression (trackingId already emitted?)
 *       → SourceFrameCache.get(frameTimestampUs)
 *       → IF source frame found:
 *           → CropGenerator.crop(sourceFrame, bbox)
 *           → IF crop valid:
 *               → JpegEncoder.encode(croppedBitmap)
 *               → IF jpeg valid:
 *                   → Build AttentionCandidate (capturedAt = wall-clock ISO 8601 UTC)
 *                   → Emit candidate
 *                   → Commit suppression for this trackingId
 *
 * Lifecycle: auto-start on CameraState.Streaming, auto-stop otherwise.
 * On stop: clears ObjectTracker, AttentionEvaluator, SourceFrameCache, suppression set.
 *
 * No Meta DAT, TFLite, Retrofit, or Backend DTO types referenced.
 */
internal class AttentionPipeline(
    private val cameraFrameProvider: CameraFrameProvider,
    private val detectionResultProvider: DetectionResultProvider,
    private val objectTracker: ObjectTracker = ObjectTracker(),
    private val attentionEvaluator: AttentionEvaluator = AttentionEvaluator(),
    private val sourceFrameCache: SourceFrameCache = SourceFrameCache(),
    private val cropGenerator: CropGenerator = CropGenerator(),
    private val jpegEncoder: JpegEncoder = JpegEncoder()
) : AttentionCandidateProvider {

    companion object {
        private const val TAG = "AttentionPipeline"
        private const val TIMING_TAG = "AttentionTiming"
    }

    // --- Output channel (bounded, not closed on stop for restart compatibility) ---

    private val _candidateChannel = Channel<AttentionCandidate>(capacity = 1)
    override val candidates: Flow<AttentionCandidate> = _candidateChannel.receiveAsFlow()

    // --- Lifecycle ---

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lifecycleMutex = Mutex()

    private var sourceFrameJob: Job? = null
    private var detectionJob: Job? = null

    /** Tracking IDs that have already emitted a candidate in the current continuous event. */
    private val suppressedTrackingIds = mutableSetOf<String>()

    /** Counts detection frames processed — used for rate-limited entry log. */
    @Volatile
    private var detectionFrameCount = 0L

    init {
        // Observe camera state to auto-start/stop
        scope.launch {
            cameraFrameProvider.cameraState.collect { state ->
                when (state) {
                    is CameraState.Streaming -> start()
                    else -> stop()
                }
            }
        }
    }

    // --- Start / Stop ---

    private suspend fun start() {
        lifecycleMutex.withLock {
            if (sourceFrameJob?.isActive == true && detectionJob?.isActive == true) return

            Log.d(TAG, "Starting attention pipeline")

            // Clear state for fresh start
            clearState()

            // Job 1: Collect camera frames into cache (NO conflate)
            sourceFrameJob = scope.launch {
                try {
                    cameraFrameProvider.frames.collect { frame ->
                        sourceFrameCache.put(frame)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Source frame collection error: ${e.message}")
                }
            }

            // Job 2: Collect detection results and process
            detectionJob = scope.launch {
                try {
                    detectionResultProvider.detections.collect { detectionFrameResult ->
                        processDetectionFrame(
                            detectionFrameResult.detections,
                            detectionFrameResult.frameTimestampUs
                        )
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Detection processing error: ${e.message}")
                }
            }
        }
    }

    private suspend fun stop() {
        lifecycleMutex.withLock {
            val sfJob = sourceFrameJob
            val dJob = detectionJob

            if (sfJob == null && dJob == null) return

            Log.d(TAG, "Stopping attention pipeline")

            sourceFrameJob = null
            detectionJob = null

            sfJob?.cancelAndJoin()
            dJob?.cancelAndJoin()

            clearState()
        }
    }

    private fun clearState() {
        objectTracker.reset()
        attentionEvaluator.reset()
        sourceFrameCache.clear()
        suppressedTrackingIds.clear()
        detectionFrameCount = 0L
        // Drain any stale candidate from previous session
        @Suppress("ControlFlowWithEmptyBody")
        while (_candidateChannel.tryReceive().isSuccess) {}
    }

    // --- Core processing ---

    private fun processDetectionFrame(
        detections: List<com.gryffindor.smartshopping.domain.model.DetectionResult>,
        frameTimestampUs: Long
    ) {
        detectionFrameCount++
        // Rate-limited entry log: first frame, then every 30th frame
        if (detectionFrameCount == 1L || detectionFrameCount % 30 == 0L) {
            Log.i(TIMING_TAG, "[Pipeline] RECV frame#=$detectionFrameCount " +
                "detections=${detections.size} frameTs=$frameTimestampUs " +
                "tracks=${objectTracker.trackCount()}")
        }

        // 1. Track objects
        val trackedObjects = objectTracker.update(detections, frameTimestampUs)

        // 2. Evaluate attention
        val evaluation = attentionEvaluator.evaluate(trackedObjects, frameTimestampUs)

        if (!evaluation.triggered) return

        val trackingId = evaluation.trackingId ?: return
        val trackedObject = evaluation.trackedObject ?: return

        // --- Timing log: attention trigger fired ---
        val firstSeenUs = objectTracker.getFirstSeenTimestampUs(trackingId)
        val trackAgeSinceFirstSeenMs = if (firstSeenUs != null) (frameTimestampUs - firstSeenUs) / 1000L else -1L

        Log.i(TIMING_TAG, buildString {
            append("[Pipeline] TRIGGER trackingId=$trackingId")
            append(" | firstSeenUs=$firstSeenUs")
            append(" | currentFrameUs=$frameTimestampUs")
            append(" | trackAgeMs=$trackAgeSinceFirstSeenMs")
            append(" | dwellMs=${evaluation.dwellMs}")
            append(" | occupancy=${"%.4f".format(evaluation.occupancyRatio)}")
            append(" | center=(${("%.3f".format(trackedObject.centerX))}, ${("%.3f".format(trackedObject.centerY))})")
        })

        Log.d(TAG, buildString {
            append("Attention trigger: trackingId=$trackingId ")
            append("occupancy=${"%.3f".format(evaluation.occupancyRatio)} ")
            append("dwell=${evaluation.dwellMs}ms")
        })

        // 3. Check duplicate suppression
        if (trackingId in suppressedTrackingIds) {
            Log.i(TIMING_TAG, "[Pipeline] DROP trackingId=$trackingId reason=already_suppressed")
            Log.d(TAG, "Suppressed duplicate for trackingId=$trackingId")
            return
        }

        // 4. Lookup exact source frame by timestamp
        val sourceFrame = sourceFrameCache.get(frameTimestampUs)
        if (sourceFrame == null) {
            Log.i(TIMING_TAG, "[Pipeline] DROP trackingId=$trackingId reason=source_frame_miss frameTs=$frameTimestampUs")
            Log.w(TAG, "Source frame miss: ts=$frameTimestampUs — no candidate, no suppression")
            return
        }
        Log.d(TAG, "Source frame match success: ts=$frameTimestampUs ${sourceFrame.width}x${sourceFrame.height}")

        // 5. Crop from original source frame
        val cropResult = cropGenerator.crop(
            frame = sourceFrame,
            left = trackedObject.left,
            top = trackedObject.top,
            right = trackedObject.right,
            bottom = trackedObject.bottom
        )
        if (cropResult == null) {
            Log.i(TIMING_TAG, "[Pipeline] DROP trackingId=$trackingId reason=crop_failed")
            Log.w(TAG, "Crop failed for trackingId=$trackingId — no suppression")
            return
        }

        // 6. JPEG encode (recycles bitmap)
        val jpegBytes = jpegEncoder.encode(cropResult.bitmap)
        if (jpegBytes == null) {
            Log.i(TIMING_TAG, "[Pipeline] DROP trackingId=$trackingId reason=jpeg_encode_failed")
            Log.w(TAG, "JPEG encoding failed for trackingId=$trackingId — no suppression")
            return
        }

        // 7. Build AttentionCandidate
        val capturedAt = Instant.now().toString() // ISO 8601 UTC wall-clock

        val candidate = AttentionCandidate(
            jpegBytes = jpegBytes,
            capturedAt = capturedAt,
            triggerType = TriggerType.OCCUPANCY_AND_DWELL,
            occupancyRatio = evaluation.occupancyRatio,
            dwellMs = evaluation.dwellMs,
            trackingId = trackingId,
            cropWidth = cropResult.width,
            cropHeight = cropResult.height
        )

        // 8. Emit candidate via bounded channel
        val sendResult = _candidateChannel.trySend(candidate)

        // 9. Commit suppression ONLY after successful channel insertion
        if (sendResult.isSuccess) {
            suppressedTrackingIds.add(trackingId)
            Log.i(TIMING_TAG, buildString {
                append("[Pipeline] CANDIDATE_CREATED trackingId=$trackingId")
                append(" | capturedAt=$capturedAt")
                append(" | dwellMs=${evaluation.dwellMs}")
                append(" | occupancy=${"%.4f".format(evaluation.occupancyRatio)}")
                append(" | crop=${cropResult.width}x${cropResult.height}")
                append(" | jpegBytes=${jpegBytes.size}")
                append(" | trackAgeMs=$trackAgeSinceFirstSeenMs")
            })
            Log.i(TAG, buildString {
                append("Candidate emitted: trackingId=$trackingId ")
                append("crop=${cropResult.width}x${cropResult.height} ")
                append("jpeg=${jpegBytes.size} bytes ")
                append("capturedAt=$capturedAt")
            })
        } else {
            Log.i(TIMING_TAG, "[Pipeline] DROP trackingId=$trackingId reason=channel_full")
            Log.w(TAG, "Candidate channel send failed — no suppression for trackingId=$trackingId")
        }
    }
}
