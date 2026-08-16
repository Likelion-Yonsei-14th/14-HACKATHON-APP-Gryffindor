package com.gryffindor.smartshopping.data.detection

import android.util.Log
import com.gryffindor.smartshopping.core.config.AppConfig
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight detection pipeline performance metrics.
 *
 * Tracks frame counts, latency distributions, and effective FPS.
 * Outputs a periodic summary to Logcat every [AppConfig.DETECTION_METRICS_SUMMARY_INTERVAL_MS].
 *
 * Thread-safe via atomic counters and synchronized latency buffer.
 * No external monitoring library required.
 */
internal class DetectionMetrics {

    companion object {
        private const val TAG = "DetectionMetrics"
    }

    // --- Frame counters (atomic for thread-safety) ---

    private val framesReceived = AtomicLong(0L)
    private val framesSampled = AtomicLong(0L)
    private val framesProcessed = AtomicLong(0L)
    private val framesDropped = AtomicLong(0L)

    // --- Latency tracking (synchronized access) ---

    private val conversionLatencies = RingBuffer(AppConfig.DETECTION_METRICS_MAX_SAMPLES)
    private val inferenceLatencies = RingBuffer(AppConfig.DETECTION_METRICS_MAX_SAMPLES)
    private val totalLatencies = RingBuffer(AppConfig.DETECTION_METRICS_MAX_SAMPLES)

    // --- FPS tracking ---

    private val processedTimestamps = RingBuffer(AppConfig.DETECTION_METRICS_MAX_SAMPLES)

    // --- Summary timing ---

    @Volatile
    private var lastSummaryTimeMs: Long = 0L

    @Volatile
    private var windowStartTimeMs: Long = 0L

    // --- Snapshot counters for per-window delta ---

    private val windowFramesReceived = AtomicLong(0L)
    private val windowFramesSampled = AtomicLong(0L)
    private val windowFramesProcessed = AtomicLong(0L)
    private val windowFramesDropped = AtomicLong(0L)

    // --- Public recording API ---

    /** Called for every frame that enters the detection pipeline (post-conflate). */
    fun recordFrameReceived() {
        framesReceived.incrementAndGet()
        windowFramesReceived.incrementAndGet()
    }

    /** Called when FrameSampler accepts a frame for processing. */
    fun recordFrameSampled() {
        framesSampled.incrementAndGet()
        windowFramesSampled.incrementAndGet()
    }

    /** Called when a frame completes full processing (conversion + inference). */
    fun recordFrameProcessed(conversionMs: Long, inferenceMs: Long, totalMs: Long) {
        framesProcessed.incrementAndGet()
        windowFramesProcessed.incrementAndGet()

        synchronized(this) {
            conversionLatencies.add(conversionMs)
            inferenceLatencies.add(inferenceMs)
            totalLatencies.add(totalMs)
            processedTimestamps.add(System.currentTimeMillis())
        }

        maybePrintSummary()
    }

    /** Called when a frame is dropped (conversion failure, inference timeout, etc.). */
    fun recordFrameDropped() {
        framesDropped.incrementAndGet()
        windowFramesDropped.incrementAndGet()
    }

    /** Reset all counters (e.g., on pipeline restart). */
    fun reset() {
        framesReceived.set(0L)
        framesSampled.set(0L)
        framesProcessed.set(0L)
        framesDropped.set(0L)
        windowFramesReceived.set(0L)
        windowFramesSampled.set(0L)
        windowFramesProcessed.set(0L)
        windowFramesDropped.set(0L)

        synchronized(this) {
            conversionLatencies.clear()
            inferenceLatencies.clear()
            totalLatencies.clear()
            processedTimestamps.clear()
        }

        lastSummaryTimeMs = System.currentTimeMillis()
        windowStartTimeMs = lastSummaryTimeMs
    }

    // --- Summary output ---

    private fun maybePrintSummary() {
        val now = System.currentTimeMillis()
        if (now - lastSummaryTimeMs < AppConfig.DETECTION_METRICS_SUMMARY_INTERVAL_MS) return
        lastSummaryTimeMs = now

        val windowReceived = windowFramesReceived.getAndSet(0L)
        val windowSampled = windowFramesSampled.getAndSet(0L)
        val windowProcessed = windowFramesProcessed.getAndSet(0L)
        val windowDropped = windowFramesDropped.getAndSet(0L)

        val fps: Float
        val avgConversionMs: Long
        val avgInferenceMs: Long
        val avgTotalMs: Long
        val p95TotalMs: Long

        synchronized(this) {
            fps = calculateEffectiveFps()
            avgConversionMs = conversionLatencies.average()
            avgInferenceMs = inferenceLatencies.average()
            avgTotalMs = totalLatencies.average()
            p95TotalMs = totalLatencies.percentile(95)
        }

        windowStartTimeMs = now

        Log.i(TAG, buildString {
            append("\n")
            append("╔══════════════════════════════════════╗\n")
            append("║       DetectionMetrics Summary       ║\n")
            append("╠══════════════════════════════════════╣\n")
            append("║ fps=%.1f\n".format(fps))
            append("║ received=$windowReceived\n")
            append("║ sampled=$windowSampled\n")
            append("║ processed=$windowProcessed\n")
            append("║ dropped=$windowDropped\n")
            append("║ avgConversionMs=$avgConversionMs\n")
            append("║ avgInferenceMs=$avgInferenceMs\n")
            append("║ avgTotalMs=$avgTotalMs\n")
            append("║ p95TotalMs=$p95TotalMs\n")
            append("╠══════════════════════════════════════╣\n")
            append("║ cumulative: recv=${framesReceived.get()} proc=${framesProcessed.get()} drop=${framesDropped.get()}\n")
            append("╚══════════════════════════════════════╝")
        })
    }

    /** Calculate effective detection FPS from recent processed timestamps within the metrics window. */
    private fun calculateEffectiveFps(): Float {
        val timestamps = processedTimestamps.snapshot()
        if (timestamps.size < 2) return 0f

        val windowMs = AppConfig.DETECTION_METRICS_WINDOW_MS
        val now = System.currentTimeMillis()
        val windowStart = now - windowMs

        val recentTimestamps = timestamps.filter { it >= windowStart }
        if (recentTimestamps.size < 2) return 0f

        val duration = recentTimestamps.last() - recentTimestamps.first()
        if (duration <= 0) return 0f

        return (recentTimestamps.size - 1) * 1000f / duration
    }

    // --- Internal ring buffer for bounded latency tracking ---

    /**
     * Fixed-capacity ring buffer for Long values.
     * No heap allocation after initial capacity is filled.
     */
    private class RingBuffer(private val capacity: Int) {
        private val buffer = LongArray(capacity)
        private var head = 0
        private var count = 0

        fun add(value: Long) {
            buffer[head] = value
            head = (head + 1) % capacity
            if (count < capacity) count++
        }

        fun clear() {
            head = 0
            count = 0
        }

        fun average(): Long {
            if (count == 0) return 0L
            var sum = 0L
            for (i in 0 until count) {
                sum += buffer[i]
            }
            return sum / count
        }

        fun percentile(p: Int): Long {
            if (count == 0) return 0L
            val sorted = snapshot().sorted()
            val index = ((p / 100.0) * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
            return sorted[index]
        }

        fun snapshot(): List<Long> {
            if (count == 0) return emptyList()
            val result = mutableListOf<Long>()
            // Read in insertion order: from oldest to newest
            val start = if (count < capacity) 0 else head
            for (i in 0 until count) {
                result.add(buffer[(start + i) % capacity])
            }
            return result
        }
    }
}
