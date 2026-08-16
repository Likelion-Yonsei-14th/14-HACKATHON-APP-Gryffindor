package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.domain.model.CameraFrame
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SourceFrameCacheTest {

    private lateinit var cache: SourceFrameCache

    @Before
    fun setUp() {
        cache = SourceFrameCache(maxSize = 5)
    }

    private fun makeFrame(timestampUs: Long): CameraFrame {
        return CameraFrame(
            data = byteArrayOf(timestampUs.toByte()),
            width = 504,
            height = 896,
            timestampUs = timestampUs,
            isCompressed = false
        )
    }

    @Test
    fun `exact timestamp lookup - hit`() {
        val frame = makeFrame(1000L)
        cache.put(frame)
        val result = cache.get(1000L)
        assertNotNull(result)
        assertEquals(1000L, result!!.timestampUs)
    }

    @Test
    fun `exact timestamp lookup - miss`() {
        val frame = makeFrame(1000L)
        cache.put(frame)
        val result = cache.get(2000L)
        assertNull(result)
    }

    @Test
    fun `bounded eviction - oldest evicted when capacity exceeded`() {
        // Cache maxSize = 5, insert 7 frames
        for (i in 1L..7L) {
            cache.put(makeFrame(i * 1000L))
        }
        assertEquals(5, cache.size())
        // Oldest (1000, 2000) should be evicted
        assertNull(cache.get(1000L))
        assertNull(cache.get(2000L))
        // Newest should remain
        assertNotNull(cache.get(3000L))
        assertNotNull(cache.get(7000L))
    }

    @Test
    fun `clear removes all entries`() {
        for (i in 1L..3L) {
            cache.put(makeFrame(i * 1000L))
        }
        assertEquals(3, cache.size())
        cache.clear()
        assertEquals(0, cache.size())
        assertNull(cache.get(1000L))
        assertNull(cache.get(2000L))
        assertNull(cache.get(3000L))
    }

    @Test
    fun `same timestamp overwrites existing entry`() {
        val frame1 = CameraFrame(
            data = byteArrayOf(1),
            width = 504, height = 896, timestampUs = 1000L, isCompressed = false
        )
        val frame2 = CameraFrame(
            data = byteArrayOf(2),
            width = 504, height = 896, timestampUs = 1000L, isCompressed = false
        )
        cache.put(frame1)
        cache.put(frame2)
        assertEquals(1, cache.size())
        val result = cache.get(1000L)
        assertNotNull(result)
        assertArrayEquals(byteArrayOf(2), result!!.data)
    }

    @Test
    fun `thread safety - concurrent put and get do not crash`() {
        val threads = (1..10).map { threadId ->
            Thread {
                for (i in 1..100) {
                    val ts = (threadId * 1000 + i).toLong()
                    cache.put(makeFrame(ts))
                    cache.get(ts)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // Should not crash; size should be bounded
        assertTrue(cache.size() <= 5)
    }
}
