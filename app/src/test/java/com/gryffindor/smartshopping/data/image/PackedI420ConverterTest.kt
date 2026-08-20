package com.gryffindor.smartshopping.data.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PackedI420ConverterTest {

    @Test
    fun `Gen2 medium frame has tightly packed I420 size`() {
        assertEquals(677_376, PackedI420Converter.expectedSize(width = 504, height = 896))
    }

    @Test
    fun `converts I420 planes to NV21 VU interleave`() {
        // 4x2: Y=8 bytes, U=2 bytes, V=2 bytes.
        val i420 = byteArrayOf(
            0, 1, 2, 3, 4, 5, 6, 7,
            10, 11,
            20, 21
        )

        assertArrayEquals(
            byteArrayOf(
                0, 1, 2, 3, 4, 5, 6, 7,
                20, 10, 21, 11
            ),
            PackedI420Converter.toNv21(i420, width = 4, height = 2)
        )
    }

    @Test
    fun `rejects non packed or odd-sized frames`() {
        assertNull(PackedI420Converter.toNv21(ByteArray(12), width = 3, height = 2))
        assertNull(PackedI420Converter.toNv21(ByteArray(11), width = 4, height = 2))
        assertNull(PackedI420Converter.toNv21(ByteArray(13), width = 4, height = 2))
    }

    @Test
    fun `copy keeps reusable destination and reports size validity`() {
        val destination = ByteArray(12)
        val source = ByteArray(12) { it.toByte() }

        assertTrue(PackedI420Converter.copyToNv21(source, 4, 2, destination))
        assertFalse(PackedI420Converter.copyToNv21(source, 4, 2, ByteArray(11)))
    }
}
