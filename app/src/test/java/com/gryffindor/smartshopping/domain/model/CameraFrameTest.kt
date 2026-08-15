package com.gryffindor.smartshopping.domain.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Task 6.1: CameraFrame ownership and metadata verification.
 *
 * Validates:
 * - Copied bytes equal source contents at conversion time
 * - Mutating or advancing the original source buffer does not alter CameraFrame.data
 * - width, height, timestampUs, isCompressed are preserved
 */
class CameraFrameTest {

    @Test
    fun `CameraFrame data equals source bytes at copy time`() {
        val sourceBytes = byteArrayOf(1, 2, 3, 4, 5, 10, 20, 30)
        val buffer = ByteBuffer.wrap(sourceBytes)

        // Simulate the ownership transfer pattern from MetaCameraSource
        val source = buffer.duplicate()
        val bytes = ByteArray(source.remaining())
        source.get(bytes)

        val frame = CameraFrame(
            data = bytes,
            width = 640,
            height = 480,
            timestampUs = 12345L,
            isCompressed = false
        )

        assertArrayEquals(sourceBytes, frame.data)
    }

    @Test
    fun `mutating original buffer does not alter CameraFrame data`() {
        val sourceBytes = byteArrayOf(10, 20, 30, 40, 50)
        val buffer = ByteBuffer.wrap(sourceBytes)

        val source = buffer.duplicate()
        val bytes = ByteArray(source.remaining())
        source.get(bytes)

        val frame = CameraFrame(
            data = bytes,
            width = 320,
            height = 240,
            timestampUs = 99999L,
            isCompressed = true
        )

        // Mutate the original buffer contents
        sourceBytes[0] = 0
        sourceBytes[1] = 0
        sourceBytes[2] = 0

        // CameraFrame data remains unchanged
        assertEquals(10.toByte(), frame.data[0])
        assertEquals(20.toByte(), frame.data[1])
        assertEquals(30.toByte(), frame.data[2])
    }

    @Test
    fun `advancing original buffer position does not affect CameraFrame data`() {
        val sourceBytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = ByteBuffer.wrap(sourceBytes)

        val source = buffer.duplicate()
        val bytes = ByteArray(source.remaining())
        source.get(bytes)

        val frame = CameraFrame(
            data = bytes,
            width = 100,
            height = 100,
            timestampUs = 555L,
            isCompressed = false
        )

        // Advance the original buffer
        buffer.get()
        buffer.get()
        buffer.get()

        // CameraFrame still has full 5 bytes
        assertEquals(5, frame.data.size)
        assertArrayEquals(sourceBytes, frame.data)
    }

    @Test
    fun `width is preserved`() {
        val frame = CameraFrame(
            data = byteArrayOf(0),
            width = 1920,
            height = 1080,
            timestampUs = 0L,
            isCompressed = false
        )
        assertEquals(1920, frame.width)
    }

    @Test
    fun `height is preserved`() {
        val frame = CameraFrame(
            data = byteArrayOf(0),
            width = 1920,
            height = 1080,
            timestampUs = 0L,
            isCompressed = false
        )
        assertEquals(1080, frame.height)
    }

    @Test
    fun `timestampUs is preserved`() {
        val frame = CameraFrame(
            data = byteArrayOf(0),
            width = 640,
            height = 480,
            timestampUs = 1234567890L,
            isCompressed = false
        )
        assertEquals(1234567890L, frame.timestampUs)
    }

    @Test
    fun `isCompressed true is preserved`() {
        val frame = CameraFrame(
            data = byteArrayOf(0),
            width = 640,
            height = 480,
            timestampUs = 0L,
            isCompressed = true
        )
        assertTrue(frame.isCompressed)
    }

    @Test
    fun `isCompressed false is preserved`() {
        val frame = CameraFrame(
            data = byteArrayOf(0),
            width = 640,
            height = 480,
            timestampUs = 0L,
            isCompressed = false
        )
        assertFalse(frame.isCompressed)
    }

    @Test
    fun `duplicate buffer preserves original buffer position`() {
        val sourceBytes = byteArrayOf(1, 2, 3, 4, 5)
        val buffer = ByteBuffer.wrap(sourceBytes)
        val originalPosition = buffer.position()

        // Use duplicate — same pattern as MetaCameraSource.transferFrameOwnership
        val source = buffer.duplicate()
        val bytes = ByteArray(source.remaining())
        source.get(bytes)

        // Original buffer position is unchanged
        assertEquals(originalPosition, buffer.position())
    }
}
