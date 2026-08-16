package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.domain.attention.TrackedObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AttentionEvaluatorTest {

    private lateinit var evaluator: AttentionEvaluator

    @Before
    fun setUp() {
        // Default thresholds: center ROI 0.15–0.85 / 0.10–0.90, occupancy 0.12, dwell 800ms, gap 2000ms
        evaluator = AttentionEvaluator(
            centerRoiLeft = 0.15f,
            centerRoiRight = 0.85f,
            centerRoiTop = 0.10f,
            centerRoiBottom = 0.90f,
            minOccupancyRatio = 0.12f,
            minDwellMs = 800L,
            maxDwellGapMs = 2000L
        )
    }

    private fun trackedObj(
        trackingId: String = "track-1",
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        area: Float = 0.15f,
        left: Float = 0.3f,
        top: Float = 0.3f,
        right: Float = 0.7f,
        bottom: Float = 0.7f
    ): TrackedObject {
        return TrackedObject(
            trackingId = trackingId,
            centerX = centerX,
            centerY = centerY,
            area = area,
            label = "item",
            lastSeenTimestampUs = 0L,
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }

    // --- Center ROI tests ---

    @Test
    fun `center ROI - object inside center is accepted`() {
        val obj = trackedObj(centerX = 0.5f, centerY = 0.5f)
        // First frame: dwell starts
        val r1 = evaluator.evaluate(listOf(obj), 1_000_000L)
        assertFalse(r1.triggered) // dwell not yet reached
    }

    @Test
    fun `center ROI - object outside horizontal range is rejected`() {
        // centerX = 0.10 < 0.15 (left boundary)
        val obj = trackedObj(centerX = 0.10f, centerY = 0.5f)
        // Even with enough dwell time simulation, should never trigger
        for (i in 0..10) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 2_200_000L)
        assertFalse(result.triggered)
    }

    @Test
    fun `center ROI - object outside vertical range is rejected`() {
        // centerY = 0.05 < 0.10 (top boundary)
        val obj = trackedObj(centerX = 0.5f, centerY = 0.05f)
        for (i in 0..10) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 2_200_000L)
        assertFalse(result.triggered)
    }

    @Test
    fun `center ROI - object at boundary is accepted`() {
        // Exactly at left=0.15 and top=0.10
        val obj = trackedObj(centerX = 0.15f, centerY = 0.10f)
        // Build up dwell
        for (i in 0..5) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 1_200_000L)
        // Should trigger if dwell is enough (depends on accumulated time)
        // 5 frames * 200ms = 1000ms dwell accumulated at frame 5,
        // then at 1_200_000 (gap from 1_000_000 = 200ms → total 1200ms)
        assertTrue(result.triggered)
    }

    // --- Occupancy tests ---

    @Test
    fun `occupancy - below threshold does not trigger`() {
        // area = 0.10 < 0.12
        val obj = trackedObj(area = 0.10f, left = 0.4f, top = 0.4f, right = 0.5f, bottom = 0.6f)
        for (i in 0..10) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 2_200_000L)
        assertFalse(result.triggered)
    }

    @Test
    fun `occupancy - exactly at threshold triggers with enough dwell`() {
        // area = 0.12 exactly
        val obj = trackedObj(area = 0.12f, left = 0.3f, top = 0.3f, right = 0.7f, bottom = 0.6f)
        for (i in 0..5) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 1_200_000L)
        assertTrue(result.triggered)
    }

    @Test
    fun `occupancy - above threshold triggers with enough dwell`() {
        val obj = trackedObj(area = 0.20f)
        for (i in 0..5) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 1_200_000L)
        assertTrue(result.triggered)
    }

    // --- Dwell tests ---

    @Test
    fun `dwell accumulation - same trackingId across N frames increases dwell`() {
        val obj = trackedObj()
        // Frame at 0, 200ms, 400ms, 600ms, 800ms intervals (in us)
        val r0 = evaluator.evaluate(listOf(obj), 0L)
        assertFalse(r0.triggered) // 0ms dwell

        val r1 = evaluator.evaluate(listOf(obj), 200_000L)
        assertFalse(r1.triggered) // 200ms dwell

        val r2 = evaluator.evaluate(listOf(obj), 400_000L)
        assertFalse(r2.triggered) // 400ms dwell

        val r3 = evaluator.evaluate(listOf(obj), 600_000L)
        assertFalse(r3.triggered) // 600ms dwell

        val r4 = evaluator.evaluate(listOf(obj), 800_000L)
        assertTrue(r4.triggered) // 800ms dwell — exactly at threshold
        assertEquals("track-1", r4.trackingId)
        assertEquals(800L, r4.dwellMs)
    }

    @Test
    fun `dwell reset - object leaves center resets dwell to zero`() {
        val objCenter = trackedObj(centerX = 0.5f, centerY = 0.5f)
        val objOutside = trackedObj(centerX = 0.05f, centerY = 0.5f) // outside center ROI

        // Accumulate 600ms
        evaluator.evaluate(listOf(objCenter), 0L)
        evaluator.evaluate(listOf(objCenter), 200_000L)
        evaluator.evaluate(listOf(objCenter), 400_000L)
        evaluator.evaluate(listOf(objCenter), 600_000L)
        val r = evaluator.evaluate(listOf(objCenter), 600_000L)
        assertFalse(r.triggered) // 600ms < 800ms

        // Object leaves center → dwell reset
        evaluator.evaluate(listOf(objOutside), 800_000L)

        // Object returns — dwell restarts from zero
        evaluator.evaluate(listOf(objCenter), 1_000_000L)
        evaluator.evaluate(listOf(objCenter), 1_200_000L)
        evaluator.evaluate(listOf(objCenter), 1_400_000L)
        evaluator.evaluate(listOf(objCenter), 1_600_000L)
        val r2 = evaluator.evaluate(listOf(objCenter), 1_600_000L)
        assertFalse(r2.triggered) // only 600ms since restart (not yet 800ms)
    }

    @Test
    fun `timestamp gap protection - large gap does not count as dwell`() {
        val obj = trackedObj()
        // Build up some dwell
        evaluator.evaluate(listOf(obj), 0L)
        evaluator.evaluate(listOf(obj), 200_000L) // 200ms
        evaluator.evaluate(listOf(obj), 400_000L) // 400ms

        // Gap of 3000ms (> 2000ms max) — should reset dwell
        evaluator.evaluate(listOf(obj), 3_400_000L)
        // Now try to accumulate from the restart
        val r1 = evaluator.evaluate(listOf(obj), 3_600_000L) // 200ms since restart
        assertFalse(r1.triggered)
        val r2 = evaluator.evaluate(listOf(obj), 3_800_000L) // 400ms
        assertFalse(r2.triggered)
        val r3 = evaluator.evaluate(listOf(obj), 4_000_000L) // 600ms
        assertFalse(r3.triggered)
        val r4 = evaluator.evaluate(listOf(obj), 4_200_000L) // 800ms — now triggers
        assertTrue(r4.triggered)
    }

    // --- Multi-object selection ---

    @Test
    fun `multi-object - prefer closest to center`() {
        val objCenter = trackedObj(trackingId = "center-obj", centerX = 0.5f, centerY = 0.5f, area = 0.15f,
            left = 0.3f, top = 0.3f, right = 0.7f, bottom = 0.7f)
        val objOff = trackedObj(trackingId = "off-obj", centerX = 0.7f, centerY = 0.7f, area = 0.20f,
            left = 0.5f, top = 0.5f, right = 0.9f, bottom = 0.9f)

        // Build dwell for both
        for (i in 0..5) {
            evaluator.evaluate(listOf(objCenter, objOff), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(objCenter, objOff), 1_200_000L)
        assertTrue(result.triggered)
        assertEquals("center-obj", result.trackingId)
    }

    @Test
    fun `multi-object - tie-break by larger occupancy`() {
        // Both at same distance from center
        val obj1 = trackedObj(trackingId = "obj-small", centerX = 0.5f, centerY = 0.5f, area = 0.15f,
            left = 0.3f, top = 0.3f, right = 0.7f, bottom = 0.7f)
        val obj2 = trackedObj(trackingId = "obj-large", centerX = 0.5f, centerY = 0.5f, area = 0.25f,
            left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)

        for (i in 0..5) {
            evaluator.evaluate(listOf(obj1, obj2), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj1, obj2), 1_200_000L)
        assertTrue(result.triggered)
        assertEquals("obj-large", result.trackingId)
    }

    // --- Invalid bbox ---

    @Test
    fun `invalid bbox - gracefully ignored`() {
        // right < left
        val invalid = TrackedObject(
            trackingId = "invalid",
            centerX = 0.5f, centerY = 0.5f,
            area = 0.20f, label = "item",
            lastSeenTimestampUs = 0L,
            left = 0.7f, top = 0.3f, right = 0.3f, bottom = 0.7f
        )
        for (i in 0..10) {
            evaluator.evaluate(listOf(invalid), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(invalid), 2_200_000L)
        assertFalse(result.triggered)
    }

    // --- Trigger requires ALL three conditions ---

    @Test
    fun `trigger requires all three - center only is not enough`() {
        // In center, low occupancy
        val obj = trackedObj(area = 0.05f, left = 0.4f, top = 0.4f, right = 0.5f, bottom = 0.5f)
        for (i in 0..10) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 2_200_000L)
        assertFalse(result.triggered)
    }

    @Test
    fun `trigger requires all three - occupancy only is not enough`() {
        // Outside center, high occupancy
        val obj = trackedObj(centerX = 0.05f, centerY = 0.5f, area = 0.30f,
            left = 0.0f, top = 0.2f, right = 0.1f, bottom = 0.8f)
        for (i in 0..10) {
            evaluator.evaluate(listOf(obj), (i * 200_000L))
        }
        val result = evaluator.evaluate(listOf(obj), 2_200_000L)
        assertFalse(result.triggered)
    }

    // --- Reset ---

    @Test
    fun `reset clears all dwell state`() {
        val obj = trackedObj()
        // Build up 600ms dwell
        evaluator.evaluate(listOf(obj), 0L)
        evaluator.evaluate(listOf(obj), 200_000L)
        evaluator.evaluate(listOf(obj), 400_000L)
        evaluator.evaluate(listOf(obj), 600_000L)

        // Reset
        evaluator.reset()

        // Continue — dwell should start from zero
        evaluator.evaluate(listOf(obj), 800_000L) // 0ms (fresh start)
        evaluator.evaluate(listOf(obj), 1_000_000L) // 200ms
        evaluator.evaluate(listOf(obj), 1_200_000L) // 400ms
        val result = evaluator.evaluate(listOf(obj), 1_400_000L) // 600ms
        assertFalse(result.triggered) // only 600ms, need 800ms
    }
}
