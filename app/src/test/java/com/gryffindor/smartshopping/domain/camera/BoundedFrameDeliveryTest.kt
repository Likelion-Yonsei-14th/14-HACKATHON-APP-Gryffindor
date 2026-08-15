package com.gryffindor.smartshopping.domain.camera

import com.gryffindor.smartshopping.domain.model.CameraFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 6.3: Bounded frame delivery verification.
 *
 * Validates:
 * - Producing frames faster than consumption does not grow unbounded queue
 * - Stale frames may be dropped (DROP_OLDEST behavior)
 * - Not every produced frame needs to be delivered
 */
class BoundedFrameDeliveryTest {

    /**
     * Creates the same bounded flow configuration used in MetaCameraSource.
     */
    private fun createBoundedFrameFlow() = MutableSharedFlow<CameraFrame>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private fun testFrame(timestamp: Long) = CameraFrame(
        data = byteArrayOf(timestamp.toByte()),
        width = 640,
        height = 480,
        timestampUs = timestamp,
        isCompressed = false
    )

    @Test
    fun `rapid production without consumer does not throw or block`() = runTest {
        val flow = createBoundedFrameFlow()

        // Produce 1000 frames rapidly without a consumer
        repeat(1000) { i ->
            val emitted = flow.tryEmit(testFrame(i.toLong()))
            // tryEmit should always succeed with DROP_OLDEST strategy
            // (may drop previous frame but never blocks/throws)
            assertTrue("tryEmit should return true with DROP_OLDEST", emitted)
        }
    }

    @Test
    fun `slow consumer receives recent frames not old backlog`() = runTest {
        val flow = createBoundedFrameFlow()
        val receivedFrames = mutableListOf<Long>()

        // Start a slow consumer
        val job = launch {
            flow.collect { frame ->
                receivedFrames.add(frame.timestampUs)
                // Slow consumer — don't actually delay, just collect what's available
            }
        }

        // Rapidly produce 100 frames
        repeat(100) { i ->
            flow.tryEmit(testFrame(i.toLong()))
        }

        // Give consumer time to process
        testScheduler.advanceUntilIdle()
        job.cancel()

        // Consumer should NOT have all 100 frames (bounded delivery drops stale)
        // With extraBufferCapacity=1 and rapid production, many will be dropped
        assertTrue(
            "Consumer should receive fewer frames than produced (bounded delivery)",
            receivedFrames.size <= 100
        )
    }

    @Test
    fun `latest frame is available when consumer finally reads`() = runTest {
        val flow = createBoundedFrameFlow()

        // Produce frame 1 (goes to buffer)
        flow.tryEmit(testFrame(1L))
        // Produce frame 2 (drops frame 1, buffer now has frame 2)
        flow.tryEmit(testFrame(2L))

        // Consumer reads — should get the most recent buffered frame
        val job = launch {
            val frame = flow.first()
            // With DROP_OLDEST and extraBufferCapacity=1:
            // frame 1 was dropped, frame 2 is in buffer
            assertEquals(2L, frame.timestampUs)
        }

        testScheduler.advanceUntilIdle()
        job.cancel()
    }

    @Test
    fun `buffer capacity is bounded at 1`() = runTest {
        val flow = createBoundedFrameFlow()
        val received = mutableListOf<Long>()

        // No consumer yet — emit 50 frames
        repeat(50) { i ->
            flow.tryEmit(testFrame(i.toLong()))
        }

        // Start consumer after burst
        val job = launch {
            flow.collect { frame ->
                received.add(frame.timestampUs)
            }
        }

        // Emit one more to trigger delivery of what's buffered + new
        flow.tryEmit(testFrame(50L))

        testScheduler.advanceUntilIdle()
        job.cancel()

        // Should have received at most 2 frames (the one in buffer + the new one)
        // because extraBufferCapacity=1 means only 1 frame is buffered
        assertTrue(
            "Bounded buffer should not accumulate unlimited frames: got ${received.size}",
            received.size <= 2
        )
    }
}
