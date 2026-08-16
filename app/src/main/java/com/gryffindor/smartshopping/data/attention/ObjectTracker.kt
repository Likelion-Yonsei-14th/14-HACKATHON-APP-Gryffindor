package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.core.config.AppConfig
import com.gryffindor.smartshopping.domain.attention.TrackedObject
import com.gryffindor.smartshopping.domain.model.DetectionResult
import java.util.UUID
import kotlin.math.sqrt

/**
 * Lightweight frame-to-frame object tracker.
 *
 * Uses greedy nearest-neighbor bbox center distance with one-to-one enforcement.
 * No Kalman Filter, Hungarian assignment, ByteTrack, DeepSORT, or appearance embeddings.
 *
 * Thread-safe via synchronized access.
 */
class ObjectTracker(
    private val maxCenterDistance: Float = AppConfig.TRACKING_MAX_CENTER_DISTANCE,
    private val gracePeriodMs: Long = AppConfig.TRACKING_GRACE_PERIOD_MS
) {
    private val activeTracks = mutableListOf<InternalTrack>()

    /**
     * Internal track state — not exposed outside this class.
     */
    private data class InternalTrack(
        val trackingId: String,
        var centerX: Float,
        var centerY: Float,
        var area: Float,
        var label: String,
        var lastSeenTimestampUs: Long,
        var left: Float,
        var top: Float,
        var right: Float,
        var bottom: Float
    )

    /**
     * Update tracker with new detections from a single frame.
     *
     * @param detections List of detections in the current frame.
     * @param frameTimestampUs Timestamp of the current frame in microseconds.
     * @return List of TrackedObjects with stable trackingIds for current frame's assignments.
     */
    @Synchronized
    fun update(detections: List<DetectionResult>, frameTimestampUs: Long): List<TrackedObject> {
        // 1. Remove expired tracks (grace period exceeded)
        val gracePeriodUs = gracePeriodMs * 1000L
        activeTracks.removeAll { track ->
            (frameTimestampUs - track.lastSeenTimestampUs) > gracePeriodUs
        }

        // 2. Greedy one-to-one association by nearest center distance
        val matchedDetectionIndices = mutableSetOf<Int>()
        val matchedTrackIndices = mutableSetOf<Int>()
        val assignments = mutableListOf<Pair<Int, Int>>() // (trackIdx, detectionIdx)

        // Build candidate pairs sorted by distance
        data class Candidate(val trackIdx: Int, val detIdx: Int, val distance: Float)

        val candidates = mutableListOf<Candidate>()
        for (trackIdx in activeTracks.indices) {
            val track = activeTracks[trackIdx]
            for (detIdx in detections.indices) {
                val det = detections[detIdx]
                val detCx = (det.left + det.right) / 2f
                val detCy = (det.top + det.bottom) / 2f
                val dx = track.centerX - detCx
                val dy = track.centerY - detCy
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist <= maxCenterDistance) {
                    candidates.add(Candidate(trackIdx, detIdx, dist))
                }
            }
        }

        // Sort by distance ascending — greedy assignment
        candidates.sortBy { it.distance }

        for (candidate in candidates) {
            if (candidate.trackIdx in matchedTrackIndices) continue
            if (candidate.detIdx in matchedDetectionIndices) continue
            assignments.add(candidate.trackIdx to candidate.detIdx)
            matchedTrackIndices.add(candidate.trackIdx)
            matchedDetectionIndices.add(candidate.detIdx)
        }

        // 3. Update matched tracks
        val result = mutableListOf<TrackedObject>()
        for ((trackIdx, detIdx) in assignments) {
            val track = activeTracks[trackIdx]
            val det = detections[detIdx]
            val cx = (det.left + det.right) / 2f
            val cy = (det.top + det.bottom) / 2f
            val area = (det.right - det.left) * (det.bottom - det.top)

            track.centerX = cx
            track.centerY = cy
            track.area = area
            track.label = det.label
            track.lastSeenTimestampUs = frameTimestampUs
            track.left = det.left
            track.top = det.top
            track.right = det.right
            track.bottom = det.bottom

            result.add(track.toTrackedObject())
        }

        // 4. Create new tracks for unmatched detections
        for (detIdx in detections.indices) {
            if (detIdx in matchedDetectionIndices) continue
            val det = detections[detIdx]
            val cx = (det.left + det.right) / 2f
            val cy = (det.top + det.bottom) / 2f
            val area = (det.right - det.left) * (det.bottom - det.top)

            val newTrack = InternalTrack(
                trackingId = UUID.randomUUID().toString(),
                centerX = cx,
                centerY = cy,
                area = area,
                label = det.label,
                lastSeenTimestampUs = frameTimestampUs,
                left = det.left,
                top = det.top,
                right = det.right,
                bottom = det.bottom
            )
            activeTracks.add(newTrack)
            result.add(newTrack.toTrackedObject())
        }

        return result
    }

    /**
     * Clear all tracks. Called on shopping session end.
     */
    @Synchronized
    fun reset() {
        activeTracks.clear()
    }

    /** Current number of active tracks (for testing/diagnostics). */
    @Synchronized
    fun trackCount(): Int = activeTracks.size

    private fun InternalTrack.toTrackedObject() = TrackedObject(
        trackingId = trackingId,
        centerX = centerX,
        centerY = centerY,
        area = area,
        label = label,
        lastSeenTimestampUs = lastSeenTimestampUs,
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}
