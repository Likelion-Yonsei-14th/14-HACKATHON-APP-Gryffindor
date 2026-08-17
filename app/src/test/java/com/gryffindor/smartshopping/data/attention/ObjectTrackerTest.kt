package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.domain.model.DetectionResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ObjectTrackerTest {

    private lateinit var tracker: ObjectTracker

    @Before
    fun setUp() {
        // maxCenterDistance = 0.20, gracePeriodMs = 500
        tracker = ObjectTracker(maxCenterDistance = 0.20f, gracePeriodMs = 500L)
    }

    private fun det(left: Float, top: Float, right: Float, bottom: Float, label: String = "item"): DetectionResult {
        return DetectionResult(
            left = left, top = top, right = right, bottom = bottom,
            label = label, confidence = 0.8f
        )
    }

    @Test
    fun `stable tracking - same position across frames keeps same trackingId`() {
        val d = det(0.4f, 0.4f, 0.6f, 0.6f)
        val result1 = tracker.update(listOf(d), 1_000_000L)
        assertEquals(1, result1.size)
        val id1 = result1[0].trackingId

        // 200ms later (within 500ms grace)
        val result2 = tracker.update(listOf(d), 1_200_000L)
        assertEquals(1, result2.size)
        assertEquals(id1, result2[0].trackingId)
    }

    @Test
    fun `new track creation - far detection gets new trackingId`() {
        val d1 = det(0.1f, 0.1f, 0.2f, 0.2f) // center ~0.15, 0.15
        val d2 = det(0.7f, 0.7f, 0.9f, 0.9f) // center ~0.80, 0.80
        val result1 = tracker.update(listOf(d1), 1_000_000L)
        val id1 = result1[0].trackingId

        // 200ms later (within grace), but detection is far away
        val result2 = tracker.update(listOf(d2), 1_200_000L)
        val id2 = result2[0].trackingId
        assertNotEquals(id1, id2)
    }

    @Test
    fun `grace period - track retained within period`() {
        val d = det(0.4f, 0.4f, 0.6f, 0.6f)
        tracker.update(listOf(d), 1_000_000L)
        assertEquals(1, tracker.trackCount())

        // No detection, but within grace (500ms = 500_000us)
        tracker.update(emptyList(), 1_400_000L)
        assertEquals(1, tracker.trackCount())
    }

    @Test
    fun `grace period - track removed after expiration`() {
        val d = det(0.4f, 0.4f, 0.6f, 0.6f)
        tracker.update(listOf(d), 1_000_000L)
        assertEquals(1, tracker.trackCount())

        // Beyond grace (500ms = 500_000us); gap = 600_000us
        tracker.update(emptyList(), 1_600_000L)
        assertEquals(0, tracker.trackCount())
    }

    @Test
    fun `reset clears all tracks`() {
        val d = det(0.4f, 0.4f, 0.6f, 0.6f)
        tracker.update(listOf(d), 1_000_000L)
        assertEquals(1, tracker.trackCount())

        tracker.reset()
        assertEquals(0, tracker.trackCount())
    }

    @Test
    fun `one-to-one - two detections close to same track position`() {
        // First frame: one detection
        val d1 = det(0.45f, 0.45f, 0.55f, 0.55f) // center 0.50, 0.50
        val result1 = tracker.update(listOf(d1), 1_000_000L)
        val existingId = result1[0].trackingId

        // Second frame (200ms later, within grace): two detections both close to 0.50, 0.50
        val d2a = det(0.46f, 0.46f, 0.56f, 0.56f) // center 0.51, 0.51
        val d2b = det(0.44f, 0.44f, 0.54f, 0.54f) // center 0.49, 0.49
        val result2 = tracker.update(listOf(d2a, d2b), 1_200_000L)

        // Both should be tracked but only one should match the existing track
        assertEquals(2, result2.size)
        val ids = result2.map { it.trackingId }.toSet()
        assertEquals(2, ids.size) // two distinct IDs — one-to-one enforced
        assertTrue(existingId in ids) // existing track matched to one
    }

    @Test
    fun `one-to-one - no two detections share same trackingId in same frame`() {
        // Create multiple initial tracks
        val detections = listOf(
            det(0.1f, 0.1f, 0.2f, 0.2f),
            det(0.4f, 0.4f, 0.6f, 0.6f),
            det(0.8f, 0.8f, 0.9f, 0.9f)
        )
        val result = tracker.update(detections, 1_000_000L)
        val ids = result.map { it.trackingId }
        // All IDs must be unique
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `slight movement within threshold maintains tracking`() {
        val d1 = det(0.40f, 0.40f, 0.60f, 0.60f) // center 0.50, 0.50
        val result1 = tracker.update(listOf(d1), 1_000_000L)
        val id1 = result1[0].trackingId

        // Small movement: center ~0.55, 0.55 — distance ~0.071, within 0.20. 200ms gap (within grace)
        val d2 = det(0.45f, 0.45f, 0.65f, 0.65f)
        val result2 = tracker.update(listOf(d2), 1_200_000L)
        assertEquals(id1, result2[0].trackingId)
    }

    @Test
    fun `multiple objects tracked simultaneously`() {
        val d1 = det(0.1f, 0.1f, 0.2f, 0.2f) // center 0.15, 0.15
        val d2 = det(0.7f, 0.7f, 0.9f, 0.9f) // center 0.80, 0.80
        val result = tracker.update(listOf(d1, d2), 1_000_000L)
        assertEquals(2, result.size)
        assertEquals(2, result.map { it.trackingId }.toSet().size)
        assertEquals(2, tracker.trackCount())
    }
}
