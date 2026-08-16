package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.CameraFrame
import com.gryffindor.smartshopping.domain.model.DetectionResult
import com.gryffindor.smartshopping.domain.model.TriggerType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the AttentionPipeline orchestration logic.
 *
 * Uses a standalone [TestableAttentionPipeline] that replicates the pipeline's
 * processDetectionFrame logic without Android Bitmap dependencies, allowing
 * verification of the core orchestration contract:
 *
 * - Exact source-frame timestamp match
 * - Suppression commit only after successful emission
 * - One candidate per continuous attention event
 * - Source frame miss → no candidate + no suppression
 * - Crop/JPEG failure → no candidate + no suppression
 * - Retry possible after failure
 */
class AttentionPipelineTest {

    private lateinit var objectTracker: ObjectTracker
    private lateinit var attentionEvaluator: AttentionEvaluator
    private lateinit var sourceFrameCache: SourceFrameCache
    private lateinit var pipeline: TestableAttentionPipeline

    @Before
    fun setUp() {
        objectTracker = ObjectTracker(
            maxCenterDistance = 0.20f,
            gracePeriodMs = 500L
        )
        attentionEvaluator = AttentionEvaluator(
            centerRoiLeft = 0.15f,
            centerRoiRight = 0.85f,
            centerRoiTop = 0.10f,
            centerRoiBottom = 0.90f,
            minOccupancyRatio = 0.12f,
            minDwellMs = 800L,
            // This suite verifies the pre-existing 800ms orchestration contract.
            // Fast-path behavior has dedicated coverage in AttentionFastUxTest.
            fastOccupancyRatio = 1.0f,
            fastOccupancyStabilityMs = 200L,
            maxDwellGapMs = 2000L
        )
        sourceFrameCache = SourceFrameCache(maxSize = 15)
        pipeline = TestableAttentionPipeline(
            objectTracker = objectTracker,
            attentionEvaluator = attentionEvaluator,
            sourceFrameCache = sourceFrameCache
        )
    }

    private fun makeFrame(timestampUs: Long): CameraFrame {
        return CameraFrame(
            data = ByteArray(504 * 896 * 3 / 2),
            width = 504,
            height = 896,
            timestampUs = timestampUs,
            isCompressed = false
        )
    }

    /** Create a detection roughly in center with significant occupancy. */
    private fun centeredLargeDetection(): DetectionResult {
        return DetectionResult(
            left = 0.25f,
            top = 0.25f,
            right = 0.75f,
            bottom = 0.75f,
            label = "handbag",
            confidence = 0.9f
        )
    }

    /**
     * Feed N frames of same detection to accumulate dwell past 800ms threshold.
     * At 200ms intervals, 5 frames = 800ms dwell.
     */
    private fun feedFramesForDwell(
        startTimestampUs: Long,
        count: Int,
        intervalUs: Long = 200_000L,
        detection: DetectionResult = centeredLargeDetection()
    ) {
        for (i in 0 until count) {
            val ts = startTimestampUs + i * intervalUs
            sourceFrameCache.put(makeFrame(ts))
            pipeline.processDetectionFrame(listOf(detection), ts)
        }
    }

    // --- Tests ---

    @Test
    fun `exact timestamp source-frame lookup - candidate uses matching frame`() {
        // Feed enough frames to accumulate dwell but stop just before trigger
        // At 200ms intervals: 4 frames accumulate 600ms dwell (need 800ms)
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 4)

        // 5th frame at the next sequential timestamp should trigger (800ms total dwell)
        val targetTs = 1_000_000L + 4 * 200_000L  // = 1_800_000
        sourceFrameCache.put(makeFrame(targetTs))
        pipeline.processDetectionFrame(listOf(centeredLargeDetection()), targetTs)

        // Verify crop was requested with correct timestamp
        val lastCropTs = pipeline.lastCropRequestedTimestamp
        assertEquals("Crop must use exact timestamp frame", targetTs, lastCropTs)
    }

    @Test
    fun `different timestamp frame is never used for crop`() {
        val wrongTs = 9_999_999L
        // correctTs is the next sequential frame after 4 frames of dwell
        val correctTs = 1_000_000L + 4 * 200_000L  // 1_800_000

        // Only put frame with wrongTs into cache (not the correct one)
        sourceFrameCache.put(makeFrame(wrongTs))

        // Feed dwell frames (the feedFramesForDwell helper puts them in cache too)
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 4)

        // 5th frame has correctTs but no matching frame in cache for this specific ts
        // (feedFramesForDwell cached up to ts=1_600_000, wrongTs=9_999_999 is in cache,
        //  but correctTs=1_800_000 is NOT in cache)
        pipeline.processDetectionFrame(listOf(centeredLargeDetection()), correctTs)

        // Crop should NOT have been called (source frame miss for correctTs)
        assertNull("Should not crop with wrong timestamp", pipeline.lastCropRequestedTimestamp)
    }

    @Test
    fun `source frame miss produces no candidate and no suppression`() {
        // Feed dwell frames (put into cache for dwell to accumulate)
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 4)

        // 5th frame triggers but source frame NOT in cache (don't put it)
        val triggerTs = 1_000_000L + 4 * 200_000L
        pipeline.processDetectionFrame(listOf(centeredLargeDetection()), triggerTs)

        // No candidate emitted
        assertEquals(0, pipeline.emittedCandidates.size)
        // Suppression not committed — retry should be possible
        assertTrue(pipeline.suppressedIds.isEmpty())
    }

    @Test
    fun `crop failure produces no candidate and no suppression`() {
        pipeline.cropShouldFail = true

        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 5)

        assertEquals(0, pipeline.emittedCandidates.size)
        assertTrue(pipeline.suppressedIds.isEmpty())
    }

    @Test
    fun `JPEG failure produces no candidate and no suppression`() {
        pipeline.jpegShouldFail = true

        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 5)

        assertEquals(0, pipeline.emittedCandidates.size)
        assertTrue(pipeline.suppressedIds.isEmpty())
    }

    @Test
    fun `retry possible after crop failure`() {
        pipeline.cropShouldFail = true
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 5)
        assertEquals(0, pipeline.emittedCandidates.size)

        // Fix the crop
        pipeline.cropShouldFail = false
        // Next frame should trigger again (same track, not suppressed)
        val nextTs = 1_000_000L + 5 * 200_000L
        sourceFrameCache.put(makeFrame(nextTs))
        pipeline.processDetectionFrame(listOf(centeredLargeDetection()), nextTs)

        assertEquals(1, pipeline.emittedCandidates.size)
    }

    @Test
    fun `retry possible after JPEG failure`() {
        pipeline.jpegShouldFail = true
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 5)
        assertEquals(0, pipeline.emittedCandidates.size)

        // Fix encoder
        pipeline.jpegShouldFail = false
        val nextTs = 1_000_000L + 5 * 200_000L
        sourceFrameCache.put(makeFrame(nextTs))
        pipeline.processDetectionFrame(listOf(centeredLargeDetection()), nextTs)

        assertEquals(1, pipeline.emittedCandidates.size)
    }

    @Test
    fun `successful emission commits suppression - same trackingId does not emit twice`() {
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 5)

        // First trigger produces candidate
        assertEquals(1, pipeline.emittedCandidates.size)

        // Continue feeding the same detection — should be suppressed
        for (i in 5..10) {
            val ts = 1_000_000L + i * 200_000L
            sourceFrameCache.put(makeFrame(ts))
            pipeline.processDetectionFrame(listOf(centeredLargeDetection()), ts)
        }

        // Still only 1 candidate (duplicate suppressed)
        assertEquals(1, pipeline.emittedCandidates.size)
    }

    @Test
    fun `multiple qualifying objects - only one candidate per frame`() {
        val det1 = DetectionResult(0.35f, 0.35f, 0.65f, 0.65f, "handbag", 0.9f) // closer to center
        val det2 = DetectionResult(0.20f, 0.20f, 0.80f, 0.80f, "bottle", 0.85f) // larger but further

        // Feed both detections across enough frames for dwell
        for (i in 0..5) {
            val ts = 1_000_000L + i * 200_000L
            sourceFrameCache.put(makeFrame(ts))
            pipeline.processDetectionFrame(listOf(det1, det2), ts)
        }

        // The evaluator picks at most one per evaluation → at most one candidate emitted
        // (AttentionEvaluator returns only one triggered object)
        assertTrue("Should emit at most one candidate per trigger", pipeline.emittedCandidates.size <= 1)
    }

    @Test
    fun `candidate contains required fields`() {
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 5)

        assertEquals(1, pipeline.emittedCandidates.size)
        val candidate = pipeline.emittedCandidates.first()

        assertNotNull(candidate.jpegBytes)
        assertTrue(candidate.jpegBytes.isNotEmpty())
        assertNotNull(candidate.capturedAt)
        assertTrue(candidate.capturedAt.isNotEmpty())
        assertEquals(TriggerType.OCCUPANCY_AND_DWELL, candidate.triggerType)
        assertTrue(candidate.occupancyRatio >= 0.12f)
        assertTrue(candidate.dwellMs >= 800L)
        assertNotNull(candidate.trackingId)
        assertNotNull(candidate.cropWidth)
        assertNotNull(candidate.cropHeight)
    }

    @Test
    fun `capturedAt is ISO 8601 format`() {
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 5)
        val candidate = pipeline.emittedCandidates.first()

        // ISO 8601 UTC format contains 'T' and ends with 'Z' or has timezone offset
        assertTrue("capturedAt should be ISO 8601: ${candidate.capturedAt}",
            candidate.capturedAt.contains("T"))
    }

    @Test
    fun `source frame miss after retry still possible`() {
        // First attempt: source frame miss
        feedFramesForDwell(startTimestampUs = 1_000_000L, count = 4)
        val triggerTs = 1_000_000L + 4 * 200_000L
        // Don't put frame for trigger ts
        pipeline.processDetectionFrame(listOf(centeredLargeDetection()), triggerTs)
        assertEquals(0, pipeline.emittedCandidates.size)

        // Second attempt with frame in cache → should succeed
        val nextTs = triggerTs + 200_000L
        sourceFrameCache.put(makeFrame(nextTs))
        pipeline.processDetectionFrame(listOf(centeredLargeDetection()), nextTs)
        assertEquals(1, pipeline.emittedCandidates.size)
    }

    // --- Testable Pipeline (no Android Bitmap dependency) ---

    /**
     * Standalone test double that replicates the AttentionPipeline's processDetectionFrame logic
     * without any Android framework dependencies (Bitmap, YuvImage, etc.).
     *
     * Crop and JPEG stages are simulated with configurable success/failure flags.
     */
    internal class TestableAttentionPipeline(
        private val objectTracker: ObjectTracker,
        private val attentionEvaluator: AttentionEvaluator,
        private val sourceFrameCache: SourceFrameCache
    ) {
        val emittedCandidates = mutableListOf<AttentionCandidate>()
        val suppressedIds: Set<String> get() = _suppressedIds.toSet()
        private val _suppressedIds = mutableSetOf<String>()

        var cropShouldFail = false
        var jpegShouldFail = false
        var lastCropRequestedTimestamp: Long? = null

        fun processDetectionFrame(
            detections: List<DetectionResult>,
            frameTimestampUs: Long
        ) {
            // 1. Track objects
            val trackedObjects = objectTracker.update(detections, frameTimestampUs)

            // 2. Evaluate attention
            val evaluation = attentionEvaluator.evaluate(trackedObjects, frameTimestampUs)

            if (!evaluation.triggered) return

            val trackingId = evaluation.trackingId ?: return
            val trackedObject = evaluation.trackedObject ?: return

            // 3. Check duplicate suppression
            if (trackingId in _suppressedIds) return

            // 4. Lookup exact source frame by timestamp
            val sourceFrame = sourceFrameCache.get(frameTimestampUs)
            if (sourceFrame == null) return

            // 5. Simulate crop
            lastCropRequestedTimestamp = sourceFrame.timestampUs
            if (cropShouldFail) return

            val cropWidth = 200
            val cropHeight = 300

            // 6. Simulate JPEG encode
            if (jpegShouldFail) return
            val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02, 0x03)

            // 7. Build AttentionCandidate
            val candidate = AttentionCandidate(
                jpegBytes = jpegBytes,
                capturedAt = java.time.Instant.now().toString(),
                triggerType = TriggerType.OCCUPANCY_AND_DWELL,
                occupancyRatio = evaluation.occupancyRatio,
                dwellMs = evaluation.dwellMs,
                trackingId = trackingId,
                cropWidth = cropWidth,
                cropHeight = cropHeight
            )

            // 8. Emit + commit suppression
            emittedCandidates.add(candidate)
            _suppressedIds.add(trackingId)
        }
    }
}
