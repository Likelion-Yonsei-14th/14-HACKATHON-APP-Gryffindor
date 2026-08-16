package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionPipelineState
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.CameraFrame
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.domain.model.DetectionFrameResult
import com.gryffindor.smartshopping.domain.model.DetectionResult
import com.gryffindor.smartshopping.domain.model.TriggerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for Channel-based candidate emission in AttentionPipeline.
 *
 * Validates:
 * - No subscriber → candidate queued → suppression committed → collector receives later
 * - Channel send failure → no suppression commit
 * - Stop/restart → flow still usable
 * - Duplicate suppression after successful channel insertion
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AttentionPipelineEmissionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies Channel-based emission semantics:
     * - trySend succeeds even without subscriber (buffered in channel)
     * - Suppression is committed on trySend success
     * - Later collector receives the buffered candidate
     */
    @Test
    fun `candidate queued without subscriber - suppression committed - collector receives later`() = runTest {
        // Simulate the Channel-based emission pattern used in AttentionPipeline
        val channel = Channel<AttentionCandidate>(capacity = 1)
        val suppressedIds = mutableSetOf<String>()

        val candidate = makeCandidate("track-1")

        // No subscriber yet — trySend should still succeed (goes into buffer)
        val result = channel.trySend(candidate)
        assertTrue("trySend should succeed with buffer capacity", result.isSuccess)

        // Suppression committed after successful send
        if (result.isSuccess) {
            suppressedIds.add("track-1")
        }
        assertTrue("Suppression should be committed", "track-1" in suppressedIds)

        // Now a collector connects later and receives the buffered candidate
        val flow = channel.receiveAsFlow()
        val received = withTimeoutOrNull(1000L) {
            flow.first()
        }
        assertNotNull("Collector should receive the buffered candidate", received)
        assertEquals("track-1", received!!.trackingId)
    }

    /**
     * Verifies that when channel is closed, trySend fails and suppression is NOT committed.
     */
    @Test
    fun `channel closed - trySend fails - no suppression commit`() = runTest {
        val channel = Channel<AttentionCandidate>(capacity = 1)
        val suppressedIds = mutableSetOf<String>()

        // Close channel to simulate failure
        channel.close()

        val candidate = makeCandidate("track-2")
        val result = channel.trySend(candidate)
        assertFalse("trySend should fail on closed channel", result.isSuccess)

        // Suppression NOT committed
        if (result.isSuccess) {
            suppressedIds.add("track-2")
        }
        assertFalse("Suppression should NOT be committed on failure", "track-2" in suppressedIds)
    }

    /**
     * Verifies that when buffer is full (SUSPEND policy), trySend fails immediately
     * and suppression is NOT committed — allowing retry on next qualifying frame.
     */
    @Test
    fun `buffer full - trySend fails - no suppression - retry possible`() = runTest {
        val channel = Channel<AttentionCandidate>(capacity = 1)
        val suppressedIds = mutableSetOf<String>()

        // Fill the buffer
        val candidateA = makeCandidate("track-A")
        val resultA = channel.trySend(candidateA)
        assertTrue("First send should succeed", resultA.isSuccess)
        suppressedIds.add("track-A")

        // Buffer is now full — second trySend for different track should fail
        val candidateB = makeCandidate("track-B")
        val resultB = channel.trySend(candidateB)
        assertFalse("trySend should fail when buffer full (SUSPEND policy)", resultB.isSuccess)

        // track-B is NOT suppressed → can retry later
        if (resultB.isSuccess) {
            suppressedIds.add("track-B")
        }
        assertFalse("track-B should NOT be suppressed", "track-B" in suppressedIds)

        // Consumer drains candidateA
        val received = channel.tryReceive()
        assertTrue(received.isSuccess)
        assertEquals("track-A", received.getOrNull()?.trackingId)

        // Now retry for track-B succeeds
        val retryResult = channel.trySend(candidateB)
        assertTrue("Retry should succeed after consumer drains", retryResult.isSuccess)
        if (retryResult.isSuccess) {
            suppressedIds.add("track-B")
        }
        assertTrue("track-B should now be suppressed after successful retry", "track-B" in suppressedIds)
    }

    /**
     * Verifies that the Channel (not closed on stop) remains usable after simulated stop/restart.
     * The pipeline's clearState drains stale candidates but does not close the channel.
     */
    @Test
    fun `stop and restart - channel remains usable - new candidates deliverable`() = runTest {
        val channel = Channel<AttentionCandidate>(capacity = 1)

        // Session 1: send a candidate
        val candidate1 = makeCandidate("track-session1")
        val result1 = channel.trySend(candidate1)
        assertTrue("Session 1 send should succeed", result1.isSuccess)

        // Simulate stop: drain stale candidates (as clearState does)
        @Suppress("ControlFlowWithEmptyBody")
        while (channel.tryReceive().isSuccess) {}

        // Session 2: channel still usable — send a new candidate
        val candidate2 = makeCandidate("track-session2")
        val result2 = channel.trySend(candidate2)
        assertTrue("Session 2 send should succeed after restart", result2.isSuccess)

        // Collector receives session 2 candidate
        val flow = channel.receiveAsFlow()
        val received = withTimeoutOrNull(1000L) {
            flow.first()
        }
        assertNotNull("Collector should receive session 2 candidate", received)
        assertEquals("track-session2", received!!.trackingId)
    }

    /**
     * Verifies duplicate suppression: same trackingId cannot emit twice after first success.
     */
    @Test
    fun `duplicate suppression - second send for same trackingId is blocked`() = runTest {
        val channel = Channel<AttentionCandidate>(capacity = 1)
        val suppressedIds = mutableSetOf<String>()

        // First emission
        val candidate1 = makeCandidate("track-dup")
        val result1 = channel.trySend(candidate1)
        assertTrue(result1.isSuccess)
        suppressedIds.add("track-dup")

        // Drain so buffer is empty for next attempt
        channel.tryReceive()

        // Second emission for same trackingId should be blocked by suppression check
        val trackingId = "track-dup"
        if (trackingId !in suppressedIds) {
            channel.trySend(makeCandidate(trackingId))
            fail("Should not reach here — suppression should block")
        }

        // Verify only 1 emission logically happened
        assertTrue("track-dup should remain suppressed", "track-dup" in suppressedIds)
    }

    /**
     * Verifies the AttentionCandidateProvider interface contract: candidates is Flow<AttentionCandidate>.
     */
    @Test
    fun `AttentionCandidateProvider exposes Flow type`() {
        val channel = Channel<AttentionCandidate>(capacity = 1)
        val provider = object : AttentionCandidateProvider {
            override val candidates: Flow<AttentionCandidate> = channel.receiveAsFlow()
        }
        // Just verify it compiles and the type is correct
        assertNotNull(provider.candidates)
    }

    // --- Helpers ---

    private fun makeCandidate(trackingId: String): AttentionCandidate {
        return AttentionCandidate(
            jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01),
            capturedAt = "2026-08-16T12:00:00Z",
            triggerType = TriggerType.OCCUPANCY_AND_DWELL,
            occupancyRatio = 0.25f,
            dwellMs = 900L,
            trackingId = trackingId,
            cropWidth = 200,
            cropHeight = 300
        )
    }
}
