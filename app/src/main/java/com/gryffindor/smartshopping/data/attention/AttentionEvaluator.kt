package com.gryffindor.smartshopping.data.attention

import com.gryffindor.smartshopping.core.config.AppConfig
import com.gryffindor.smartshopping.domain.attention.DwellState
import com.gryffindor.smartshopping.domain.attention.TrackedObject
import kotlin.math.sqrt

/**
 * Evaluates whether tracked objects satisfy the attention trigger policy:
 *
 *   Center ROI AND Occupancy >= threshold AND Dwell >= threshold
 *
 * Manages per-trackingId dwell state. Uses frameTimestampUs for dwell calculation
 * (NOT wall-clock time). Protects against large timestamp gaps.
 *
 * Thread-safe via synchronized access.
 */
class AttentionEvaluator(
    private val centerRoiLeft: Float = AppConfig.ATTENTION_CENTER_ROI_LEFT,
    private val centerRoiRight: Float = AppConfig.ATTENTION_CENTER_ROI_RIGHT,
    private val centerRoiTop: Float = AppConfig.ATTENTION_CENTER_ROI_TOP,
    private val centerRoiBottom: Float = AppConfig.ATTENTION_CENTER_ROI_BOTTOM,
    private val minOccupancyRatio: Float = AppConfig.ATTENTION_MIN_OCCUPANCY_RATIO,
    private val minDwellMs: Long = AppConfig.ATTENTION_MIN_DWELL_MS,
    private val maxDwellGapMs: Long = AppConfig.ATTENTION_MAX_DWELL_GAP_MS
) {
    /** Per-trackingId dwell accumulation state. */
    private val dwellStates = mutableMapOf<String, DwellState>()

    /**
     * Result of evaluating a single frame's tracked objects.
     */
    data class EvaluationResult(
        /** Whether any object triggered this frame. */
        val triggered: Boolean,
        /** The trackingId of the triggered object, if any. */
        val trackingId: String?,
        /** Occupancy ratio of the triggered object. */
        val occupancyRatio: Float,
        /** Accumulated dwell time (ms) of the triggered object. */
        val dwellMs: Long,
        /** The TrackedObject that triggered, if any. */
        val trackedObject: TrackedObject?
    )

    /**
     * Evaluate a list of tracked objects for attention trigger eligibility.
     *
     * @param trackedObjects Current frame's tracked objects from ObjectTracker.
     * @param frameTimestampUs Current frame timestamp in microseconds.
     * @return EvaluationResult. If triggered=true, contains the best candidate's info.
     */
    @Synchronized
    fun evaluate(
        trackedObjects: List<TrackedObject>,
        frameTimestampUs: Long
    ): EvaluationResult {
        val qualifiedCandidates = mutableListOf<CandidateInfo>()

        // Track which trackingIds are present this frame (for cleanup later)
        val presentIds = mutableSetOf<String>()

        for (obj in trackedObjects) {
            presentIds.add(obj.trackingId)

            // Validate bbox coordinates
            if (!isValidBbox(obj)) continue

            val inCenter = isInCenterRoi(obj.centerX, obj.centerY)
            val occupancy = obj.area
            val occupancySatisfied = occupancy >= minOccupancyRatio
            val centerAndOccupancy = inCenter && occupancySatisfied

            val currentDwell = dwellStates[obj.trackingId] ?: DwellState()
            val updatedDwell = updateDwell(currentDwell, centerAndOccupancy, frameTimestampUs)
            dwellStates[obj.trackingId] = updatedDwell

            // Check full trigger: center AND occupancy AND dwell
            if (centerAndOccupancy && updatedDwell.accumulatedDwellMs >= minDwellMs) {
                qualifiedCandidates.add(
                    CandidateInfo(
                        trackedObject = obj,
                        occupancyRatio = occupancy,
                        dwellMs = updatedDwell.accumulatedDwellMs
                    )
                )
            }
        }

        // Reset dwell for tracks that are no longer present
        // (This handles tracks that disappeared without grace — the tracker handles grace itself,
        //  but if a track doesn't appear in this evaluation, its dwell should not advance.)
        // Note: We don't remove from dwellStates here because the track might still be in grace.
        // Dwell reset happens naturally via wasSatisfied=false path.

        if (qualifiedCandidates.isEmpty()) {
            return EvaluationResult(
                triggered = false,
                trackingId = null,
                occupancyRatio = 0f,
                dwellMs = 0L,
                trackedObject = null
            )
        }

        // Select best candidate: closest to center, then largest occupancy
        val best = qualifiedCandidates.minWith(
            compareBy<CandidateInfo> { distanceToCenter(it.trackedObject.centerX, it.trackedObject.centerY) }
                .thenByDescending { it.occupancyRatio }
        )

        return EvaluationResult(
            triggered = true,
            trackingId = best.trackedObject.trackingId,
            occupancyRatio = best.occupancyRatio,
            dwellMs = best.dwellMs,
            trackedObject = best.trackedObject
        )
    }

    /**
     * Clear all dwell state. Called on shopping session end.
     */
    @Synchronized
    fun reset() {
        dwellStates.clear()
    }

    /**
     * Remove dwell state for a specific trackingId (e.g., after track expiration).
     */
    @Synchronized
    fun removeTrack(trackingId: String) {
        dwellStates.remove(trackingId)
    }

    // --- Private helpers ---

    private fun isInCenterRoi(cx: Float, cy: Float): Boolean {
        return cx in centerRoiLeft..centerRoiRight && cy in centerRoiTop..centerRoiBottom
    }

    private fun isValidBbox(obj: TrackedObject): Boolean {
        return obj.left in 0f..1f &&
            obj.top in 0f..1f &&
            obj.right in 0f..1f &&
            obj.bottom in 0f..1f &&
            obj.right > obj.left &&
            obj.bottom > obj.top
    }

    private fun distanceToCenter(cx: Float, cy: Float): Float {
        val dx = cx - 0.5f
        val dy = cy - 0.5f
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    /**
     * Update dwell state for a tracked object.
     *
     * - If centerAndOccupancy is now satisfied and was previously satisfied,
     *   accumulate the time delta (unless gap is too large).
     * - If centerAndOccupancy just became satisfied, start a new period.
     * - If centerAndOccupancy is NOT satisfied, reset accumulation.
     */
    private fun updateDwell(
        current: DwellState,
        centerAndOccupancySatisfied: Boolean,
        frameTimestampUs: Long
    ): DwellState {
        if (!centerAndOccupancySatisfied) {
            // Conditions not met → reset dwell
            return DwellState(
                accumulatedDwellMs = 0L,
                periodStartTimestampUs = 0L,
                lastQualifyingTimestampUs = 0L,
                wasSatisfied = false
            )
        }

        // Conditions ARE satisfied
        if (!current.wasSatisfied) {
            // Transition from not-satisfied → satisfied: start new period
            return DwellState(
                accumulatedDwellMs = 0L,
                periodStartTimestampUs = frameTimestampUs,
                lastQualifyingTimestampUs = frameTimestampUs,
                wasSatisfied = true
            )
        }

        // Was already satisfied: accumulate dwell
        val gapUs = frameTimestampUs - current.lastQualifyingTimestampUs
        val gapMs = gapUs / 1000L
        val maxGapUs = maxDwellGapMs * 1000L

        return if (gapUs > maxGapUs) {
            // Gap too large — do NOT count gap as dwell; restart period
            DwellState(
                accumulatedDwellMs = 0L,
                periodStartTimestampUs = frameTimestampUs,
                lastQualifyingTimestampUs = frameTimestampUs,
                wasSatisfied = true
            )
        } else {
            // Normal accumulation
            DwellState(
                accumulatedDwellMs = current.accumulatedDwellMs + gapMs,
                periodStartTimestampUs = current.periodStartTimestampUs,
                lastQualifyingTimestampUs = frameTimestampUs,
                wasSatisfied = true
            )
        }
    }

    private data class CandidateInfo(
        val trackedObject: TrackedObject,
        val occupancyRatio: Float,
        val dwellMs: Long
    )
}
