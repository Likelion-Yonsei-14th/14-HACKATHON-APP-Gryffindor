package com.gryffindor.smartshopping.data.detection

import com.gryffindor.smartshopping.core.config.AppConfig
import com.gryffindor.smartshopping.domain.model.CameraFrame

/**
 * Time-based frame sampling gate.
 *
 * Combined with Flow.conflate() upstream, this achieves latest-frame-wins behavior:
 * - At most 1 buffered frame (Kotlin conflate = latest-frame-wins)
 * - Configurable minimum delivery interval
 * - Zero unbounded backlog
 *
 * Thread-safe via volatile last delivery timestamp.
 */
internal class FrameSampler(
    private val minIntervalMs: Long = AppConfig.DETECTION_FRAME_INTERVAL_MS
) {
    @Volatile
    private var lastDeliveryTimeMs: Long = 0L

    /**
     * Returns true if enough time has elapsed since last delivery.
     * If true, the caller should process this frame.
     */
    fun shouldProcess(frame: CameraFrame): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastDeliveryTimeMs < minIntervalMs) {
            return false
        }
        lastDeliveryTimeMs = now
        return true
    }

    /** Reset state (e.g., on pipeline restart). */
    fun reset() {
        lastDeliveryTimeMs = 0L
    }
}
