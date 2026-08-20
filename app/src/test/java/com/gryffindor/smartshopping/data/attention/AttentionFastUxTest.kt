package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.domain.attention.TrackedObject
import com.gryffindor.smartshopping.domain.model.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttentionFastUxTest {

    private lateinit var evaluator: AttentionEvaluator

    @Before
    fun setUp() {
        evaluator = AttentionEvaluator(
            centerRoiLeft = 0.15f,
            centerRoiRight = 0.85f,
            centerRoiTop = 0.10f,
            centerRoiBottom = 0.90f,
            minOccupancyRatio = 0.12f,
            minDwellMs = 600L,
            fastOccupancyRatio = 0.22f,
            fastOccupancyStabilityMs = 200L,
            maxDwellGapMs = 2000L
        )
    }

    @Test
    fun `strong centered occupancy triggers after 200ms stability`() {
        val objectInView = trackedObject(area = 0.24f)

        assertFalse(evaluator.evaluate(listOf(objectInView), 0L).triggered)
        val result = evaluator.evaluate(listOf(objectInView), 200_000L)

        assertTrue(result.triggered)
        assertEquals(TriggerType.OCCUPANCY, result.triggerType)
        assertEquals(200L, result.dwellMs)
    }

    @Test
    fun `one noisy strong-occupancy frame does not trigger`() {
        val objectInView = trackedObject(area = 0.24f)

        val result = evaluator.evaluate(listOf(objectInView), 0L)

        assertFalse(result.triggered)
    }

    @Test
    fun `moderate occupancy triggers by dwell at 600ms`() {
        val objectInView = trackedObject(area = 0.15f)
        evaluator.evaluate(listOf(objectInView), 0L)
        evaluator.evaluate(listOf(objectInView), 200_000L)
        evaluator.evaluate(listOf(objectInView), 400_000L)
        val result = evaluator.evaluate(listOf(objectInView), 600_000L)

        assertTrue(result.triggered)
        assertEquals(TriggerType.DWELL, result.triggerType)
        assertEquals(600L, result.dwellMs)
    }

    @Test
    fun `small background bbox never builds trigger dwell`() {
        val background = trackedObject(area = 0.08f)

        var result = evaluator.evaluate(listOf(background), 0L)
        for (timestampMs in 200L..2_000L step 200L) {
            result = evaluator.evaluate(listOf(background), timestampMs * 1000L)
        }

        assertFalse(result.triggered)
    }

    @Test
    fun `strong occupancy outside center never triggers`() {
        val offCenter = trackedObject(centerX = 0.05f, area = 0.30f)

        var result = evaluator.evaluate(listOf(offCenter), 0L)
        for (timestampMs in 200L..1_000L step 200L) {
            result = evaluator.evaluate(listOf(offCenter), timestampMs * 1000L)
        }

        assertFalse(result.triggered)
    }

    @Test
    fun `moderate dwell does not count as strong-occupancy stability`() {
        val moderate = trackedObject(area = 0.15f)
        evaluator.evaluate(listOf(moderate), 0L)
        evaluator.evaluate(listOf(moderate), 200_000L)
        evaluator.evaluate(listOf(moderate), 400_000L)

        val strongForOneFrame = trackedObject(area = 0.24f)
        val result = evaluator.evaluate(listOf(strongForOneFrame), 500_000L)

        assertFalse(result.triggered)
    }

    private fun trackedObject(
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
        area: Float
    ) = TrackedObject(
        trackingId = "track-1",
        centerX = centerX,
        centerY = centerY,
        area = area,
        label = "item",
        lastSeenTimestampUs = 0L,
        left = 0.30f,
        top = 0.30f,
        right = 0.70f,
        bottom = 0.70f
    )
}
