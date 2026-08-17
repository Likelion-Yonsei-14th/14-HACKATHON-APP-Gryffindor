package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.core.config.AppConfig
import com.gryffindor.smartshopping.domain.model.CameraFrame

/**
 * Bounded cache of recent [CameraFrame] instances indexed by [CameraFrame.timestampUs].
 *
 * Used by the Attention Pipeline to retrieve the original source frame matching a
 * detection's frameTimestampUs for high-quality crop generation.
 *
 * Thread-safe via synchronized access.
 * Evicts oldest entries when capacity ([AppConfig.SOURCE_FRAME_CACHE_MAX_SIZE]) is exceeded.
 */
class SourceFrameCache(
    private val maxSize: Int = AppConfig.SOURCE_FRAME_CACHE_MAX_SIZE
) {
    // LinkedHashMap with accessOrder=false preserves insertion order for oldest-eviction
    private val cache = LinkedHashMap<Long, CameraFrame>(maxSize + 1, 0.75f, false)

    /**
     * Store a frame. If cache exceeds [maxSize], the oldest entry is evicted.
     */
    @Synchronized
    fun put(frame: CameraFrame) {
        cache[frame.timestampUs] = frame
        if (cache.size > maxSize) {
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
        }
    }

    /**
     * Retrieve a frame by exact [timestampUs] match.
     * Returns null if not found (expired or never cached).
     */
    @Synchronized
    fun get(timestampUs: Long): CameraFrame? {
        return cache[timestampUs]
    }

    /**
     * Clear all cached frames. Called on shopping session end.
     */
    @Synchronized
    fun clear() {
        cache.clear()
    }

    /** Current number of cached frames (for testing/diagnostics). */
    @Synchronized
    fun size(): Int = cache.size
}
