# Implementation Plan: Attention Candidate Pipeline (A3)

## Overview

A3 sits between A2 (Object Detection) and A4 (Backend Recognition). It consumes the `DetectionFrameResult` stream (from `DetectionResultProvider`) and original `CameraFrame` stream (from `CameraFrameProvider`), determines which detected object the user is meaningfully attending to via lightweight tracking + center/occupancy/dwell evaluation, and produces a cropped JPEG `AttentionCandidate` ready for Backend recognition in A4.

**Critical path:**

```text
Domain models + config (Task 1)
→ Source Frame Cache (Task 2)
→ Object Tracker (Task 3)
→ Attention Evaluator (Task 4)
→ Crop + JPEG (Task 5)
→ Checkpoint (Task 6)
→ AttentionPipeline orchestrator (Task 7)
→ AppContainer + lifecycle wiring (Task 8)
→ Checkpoint (Task 9)
→ Real Gen2 Acceptance (Task 10)
```

**A3 boundary:**

```text
DetectionFrameResult + exact original CameraFrame
→ Lightweight Tracking
→ Center ROI + Occupancy + Dwell
→ Attention Trigger
→ Crop from original Gen2 frame
→ JPEG encoding
→ AttentionCandidate emission
```

**NOT in A3 scope:** Backend HTTP calls, Retrofit, multipart /recognize, Backend DTOs, product card rendering, MATCHED/UNKNOWN/AMBIGUOUS handling.

---

## Tasks

* [ ] 1. Domain Models, Interfaces, and Configuration

  * [ ] 1.1 Create A3 domain models and AttentionCandidateProvider interface

    * Create `domain/model/AttentionCandidate.kt`:

      ```kotlin
      data class AttentionCandidate(
          val jpegBytes: ByteArray,
          val capturedAt: String,       // ISO 8601 UTC wall-clock
          val triggerType: TriggerType,
          val occupancyRatio: Float,
          val dwellMs: Long,
          val trackingId: String?,
          val cropWidth: Int?,
          val cropHeight: Int?
      )
      ```

    * Create `domain/attention/AttentionCandidateProvider.kt`:

      ```kotlin
      interface AttentionCandidateProvider {
          val candidates: Flow<AttentionCandidate>
      }
      ```

    * Create tracking domain models in `domain/attention/`:

      * `TrackedObject` — trackingId, bbox center, area, label, last-seen timestamp
      * `ActiveTrack` — tracked object + dwell state
      * `DwellState` — accumulated dwell, start timestamp, whether center/occupancy satisfied

    * Reuse existing `TriggerType` from `domain/model/Observation.kt`.

    * No Meta DAT, TFLite, or Retrofit types in any domain file.

    * **Key files:**

      * `domain/model/AttentionCandidate.kt` (NEW)
      * `domain/attention/AttentionCandidateProvider.kt` (NEW)
      * `domain/attention/TrackedObject.kt` (NEW)
      * `domain/attention/ActiveTrack.kt` (NEW)
      * `domain/attention/DwellState.kt` (NEW)

    * **Done when:**

      ```bash
      ./gradlew clean assembleDebug
      ```

      succeeds. Domain models compile with no SDK imports.

    * *Requirements: 8.1–8.10, 11.4*

  * [ ] 1.2 Add A3 configuration values to AppConfig

    * Extend `core/config/AppConfig.kt` with named constants:

      ```kotlin
      // Center ROI
      ATTENTION_CENTER_ROI_LEFT = 0.15f
      ATTENTION_CENTER_ROI_RIGHT = 0.85f
      ATTENTION_CENTER_ROI_TOP = 0.10f
      ATTENTION_CENTER_ROI_BOTTOM = 0.90f

      // Attention thresholds
      ATTENTION_MIN_OCCUPANCY_RATIO = 0.12f
      ATTENTION_MIN_DWELL_MS = 800L

      // Tracking
      TRACKING_MAX_CENTER_DISTANCE = 0.20f
      TRACKING_GRACE_PERIOD_MS = 500L

      // Crop
      ATTENTION_CROP_PADDING_RATIO = 0.15f
      ATTENTION_MIN_CROP_SHORT_SIDE = 160
      ATTENTION_MAX_CROP_LONG_SIDE = 1024

      // JPEG
      ATTENTION_JPEG_QUALITY = 85

      // Source frame cache
      SOURCE_FRAME_CACHE_MAX_SIZE = 15

      // Dwell timestamp gap ceiling
      ATTENTION_MAX_DWELL_GAP_MS = 2000L
      ```

    * No magic numbers in logic code.

    * **Key files:**

      * `core/config/AppConfig.kt` (MODIFIED)

    * **Done when:**

      ```bash
      ./gradlew clean assembleDebug
      ```

      succeeds. All values compile as named constants.

    * *Requirements: 1.10, 2.6, 3.7, 4.2, 6.3, 7.2–7.3, 11.6, 12.3*

  * [ ]* 1.3 Write unit tests for AttentionCandidate model

    * Verify `equals` / `hashCode` (ByteArray handling)
    * Verify fields are correctly stored
    * *Requirements: 8.1–8.8*

---

* [ ] 2. Source Frame Cache

  * [ ] 2.1 Implement SourceFrameCache

    * Create `data/attention/SourceFrameCache.kt`.

    * Responsibilities:

      * Store recent `CameraFrame` instances indexed by `timestampUs`
      * Bounded to `SOURCE_FRAME_CACHE_MAX_SIZE` entries (evict oldest)
      * Lookup by exact `timestampUs` match
      * `clear()` for session lifecycle reset
      * Thread-safe (synchronized or mutex)

    * Do NOT drop frames with conflate(). Accept all frames up to the bounded limit.

    * **Key files:**

      * `data/attention/SourceFrameCache.kt` (NEW)

    * **Done when:**

      * Cache stores and retrieves frames by exact timestamp.
      * Evicts oldest when capacity exceeded.
      * `clear()` removes all entries.
      * Compile succeeds.

    * *Requirements: 5.1–5.8, 12.3*

  * [ ]* 2.2 Write unit tests for SourceFrameCache

    * Test bounded eviction (insert > maxSize, oldest evicted)
    * Test exact timestamp lookup (hit and miss)
    * Test clear() resets all entries
    * Test thread safety (concurrent put/get)
    * *Requirements: 5.3–5.4, 5.6*

---

* [ ] 3. Lightweight Object Tracker

  * [ ] 3.1 Implement ObjectTracker

    * Create `data/attention/ObjectTracker.kt`.

    * Responsibilities:

      * Accept a `List<DetectionResult>` + `frameTimestampUs`
      * Associate each detection with at most one existing track (one-to-one)
      * Association metric: bbox center distance (Euclidean, normalized coords)
      * Threshold: `TRACKING_MAX_CENTER_DISTANCE`
      * One-to-one constraint: each detection assigned to at most one track; each track assigned to at most one detection per frame
      * Create new track with new `trackingId` for unmatched detections
      * Grace period: retain unmatched tracks for `TRACKING_GRACE_PERIOD_MS`
      * Remove tracks exceeding grace period
      * `reset()` for session lifecycle
      * Return `List<TrackedObject>` with current frame's assignments

    * Do NOT use Kalman Filter, Hungarian, ByteTrack, DeepSORT, or appearance embeddings.

    * Lightweight greedy nearest-neighbor with one-to-one enforcement.

    * **Key files:**

      * `data/attention/ObjectTracker.kt` (NEW)

    * **Done when:**

      * Tracks persist across frames for similar bbox positions.
      * New trackingId assigned for clearly new objects.
      * Grace period eviction works.
      * One-to-one constraint enforced.
      * `reset()` clears all tracks.

    * *Requirements: 1.1–1.11*

  * [ ]* 3.2 Write unit tests for ObjectTracker

    * **Property: one-to-one track assignment** — no two detections in the same frame share the same trackingId
    * Test stable tracking: detection at similar position across frames keeps same trackingId
    * Test new track creation: detection far from existing tracks gets new trackingId
    * Test grace period: track retained for configured period, removed after expiration
    * Test reset: all tracks cleared
    * Test one-to-one: when two detections are close to the same track, only one gets matched
    * *Requirements: 1.1–1.8, 1.10–1.11*

---

* [ ] 4. Attention Evaluator (Center ROI + Occupancy + Dwell)

  * [ ] 4.1 Implement AttentionEvaluator

    * Create `data/attention/AttentionEvaluator.kt`.

    * Responsibilities:

      * Accept a tracked object + frame timestamp
      * **Center ROI check:** bbox center within configured horizontal/vertical range
      * **Occupancy check:** bbox area ratio ≥ `ATTENTION_MIN_OCCUPANCY_RATIO`
      * **Dwell accumulation:** track continuous time that center+occupancy are satisfied
      * **Dwell reset:** reset when center/occupancy NOT satisfied, or track lost beyond grace
      * **Trigger check:** all three (center AND occupancy AND dwell) simultaneously satisfied
      * **Timestamp gap protection:** if gap between consecutive frames exceeds `ATTENTION_MAX_DWELL_GAP_MS`, do NOT count the gap as dwell time (avoid false trigger after pause/lifecycle interrupt)
      * Track per-trackingId dwell state
      * Return evaluation result with: triggered (boolean), occupancyRatio, dwellMs, trackingId
      * `reset()` for session lifecycle

    * When multiple objects qualify in the same evaluation cycle, prefer the one closest to frame center. Tie-break by larger occupancy.

    * Safely ignore detections with invalid bbox coordinates.

    * **Key files:**

      * `data/attention/AttentionEvaluator.kt` (NEW)

    * **Done when:**

      * Center ROI correctly evaluated.
      * Occupancy correctly calculated from normalized bbox.
      * Dwell accumulates across frames for same trackingId.
      * Dwell resets when conditions fail.
      * Trigger fires only when all three conditions met.
      * Gap protection prevents false triggers.
      * Multiple candidates: prefer closest-to-center.
      * `reset()` clears all dwell state.

    * *Requirements: 2.1–2.8, 3.1–3.10, 4.1–4.9*

  * [ ]* 4.2 Write unit tests for AttentionEvaluator

    * **Property: trigger requires all three conditions** — trigger never fires unless center AND occupancy AND dwell are ALL satisfied
    * Test center ROI boundary: detection inside → accepted, outside → rejected
    * Test occupancy threshold: exactly at threshold, below, above
    * Test dwell accumulation: same trackingId across N frames → dwell increases
    * Test dwell reset: object leaves center → dwell resets to zero
    * Test timestamp gap protection: gap > ATTENTION_MAX_DWELL_GAP_MS does not count
    * Test multi-object selection: prefer closest to center, then larger occupancy
    * Test invalid bbox handling: gracefully ignored
    * *Requirements: 2.1–2.8, 3.1–3.10, 4.1–4.9*

---

* [ ] 5. Crop Generation and JPEG Encoding

  * [ ] 5.1 Implement CropGenerator

    * Create `data/attention/CropGenerator.kt`.

    * Responsibilities:

      * Accept: original `CameraFrame` + normalized bbox (left, top, right, bottom)
      * Apply padding: expand bbox by `ATTENTION_CROP_PADDING_RATIO` relative to bbox size
      * Clamp padded coordinates to frame boundaries [0, width/height]
      * Convert the full original CameraFrame to Bitmap using the existing YUV conversion path (reuse logic from `FrameConverter` for NV21 → YuvImage → JPEG → Bitmap — but do NOT resize to 300×300)
      * Crop the region from the full-resolution Bitmap
      * Capture `width` and `height` from cropped Bitmap BEFORE recycle
      * Validate: short side ≥ `ATTENTION_MIN_CROP_SHORT_SIDE`
      * Proportional downscale if long side > `ATTENTION_MAX_CROP_LONG_SIDE` (preserve aspect ratio)
      * Return cropped Bitmap + dimensions, or null on failure

    * Crop is from original Gen2 frame (504×896), NOT from the 300×300 detector input.

    * Recycle intermediate Bitmaps after use.

    * **Key files:**

      * `data/attention/CropGenerator.kt` (NEW)

    * **Done when:**

      * Padded crop coordinates correctly computed.
      * Clamping prevents out-of-bounds.
      * Min short-side check rejects too-small crops.
      * Max long-side downscale works.
      * Bitmap width/height captured before recycle.
      * Full-res YUV conversion (not 300×300 resize) works.

    * *Requirements: 6.1–6.12*

  * [ ] 5.2 Implement JpegEncoder

    * Create `data/attention/JpegEncoder.kt`.

    * Responsibilities:

      * Accept a Bitmap
      * Encode to JPEG at `ATTENTION_JPEG_QUALITY`
      * Return non-empty `ByteArray`, or null on failure
      * Recycle the input Bitmap after encoding (caller's contract or internal)

    * No Backend upload size constraint invented by A3.

    * **Key files:**

      * `data/attention/JpegEncoder.kt` (NEW)

    * **Done when:**

      * JPEG encoding produces non-empty ByteArray.
      * Encoding failure returns null without crash.
      * Quality configurable via AppConfig.

    * *Requirements: 7.1–7.8*

  * [ ]* 5.3 Write unit tests for CropGenerator and JpegEncoder

    * Test padding calculation with various bbox sizes
    * Test clamping at frame boundaries
    * Test min short-side rejection
    * Test max long-side downscale preserves aspect ratio
    * Test JPEG encoding produces valid non-empty output
    * Test encoding failure returns null
    * *Requirements: 6.1–6.12, 7.1–7.6*

---

* [ ] 6. Checkpoint — Components Compile and Pass Unit Tests

  * Ensure all tests pass, ask the user if questions arise.

  * Verify:

    ```bash
    ./gradlew clean assembleDebug
    ```

  * All A3 domain models, SourceFrameCache, ObjectTracker, AttentionEvaluator, CropGenerator, JpegEncoder compile independently.

  * *Requirements: 11.15, 12.9*

---

* [ ] 7. AttentionPipeline Orchestrator

  * [ ] 7.1 Implement AttentionPipeline with parallel collection and duplicate suppression

    * Create `data/attention/AttentionPipeline.kt` implementing `AttentionCandidateProvider`.

    * Orchestration:

      ```text
      Parallel Job 1: CameraFrameProvider.frames → SourceFrameCache.put()
      Parallel Job 2: DetectionResultProvider.detections → processDetectionFrame()
      ```

    * `processDetectionFrame()` flow:

      ```text
      DetectionFrameResult
      → ObjectTracker.update(detections, frameTimestampUs)
      → AttentionEvaluator.evaluate(trackedObjects, frameTimestampUs)
      → IF triggered:
          → check duplicate suppression (trackingId already emitted?)
          → SourceFrameCache.get(frameTimestampUs)
          → IF source frame found:
              → CropGenerator.crop(sourceFrame, bbox)
              → IF crop valid:
                  → JpegEncoder.encode(croppedBitmap)
                  → IF jpeg valid:
                      → Build AttentionCandidate (capturedAt = wall-clock ISO 8601 UTC)
                      → Emit candidate
                      → Commit suppression for this trackingId
      ```

    * **Suppression rules:**
      * Suppression committed ONLY after successful candidate emission
      * Source frame miss → no suppression commit → same track can retry
      * Crop failure → no suppression commit → retry possible
      * JPEG failure → no suppression commit → retry possible
      * One candidate per continuous attention event per trackingId

    * **Lifecycle:**
      * Auto-start when `CameraState.Streaming` (same pattern as DetectionPipeline)
      * Auto-stop otherwise
      * On stop: clear ObjectTracker, AttentionEvaluator, SourceFrameCache, suppression set
      * While inactive: no candidate emission

    * **Threading:**
      * CPU work on `Dispatchers.Default`
      * Source frame collection on dedicated coroutine
      * Do NOT add conflate() on the source frame collector
      * Candidate output: `MutableSharedFlow(buffer=1, DROP_OLDEST)`

    * **capturedAt:** `Instant.now().toString()` or equivalent ISO 8601 UTC from wall-clock

    * **Key files:**

      * `data/attention/AttentionPipeline.kt` (NEW)

    * **Done when:**

      * Pipeline orchestrates all components end-to-end.
      * Parallel collection of frames and detections.
      * Duplicate suppression works correctly (commit only on success).
      * Lifecycle start/stop clears all state.
      * No Main thread blocking.
      * Candidate Flow emits `AttentionCandidate` when trigger fires.

    * *Requirements: 1.1–1.11, 2.1–2.8, 3.1–3.10, 4.1–4.9, 5.1–5.8, 6.1–6.12, 7.1–7.8, 8.1–8.11, 9.1–9.8, 10.1–10.7, 12.1–12.10*

  * [ ]* 7.2 Write unit/integration tests for AttentionPipeline

    * **Property: suppression commit only after emission** — failed crop/JPEG/cache-miss does NOT commit suppression
    * **Property: exact source-frame timestamp match** — candidate's crop comes from CameraFrame with matching timestampUs
    * **Property: one candidate per continuous attention event** — same trackingId does not emit twice while continuously tracked
    * Test end-to-end: detection → tracking → attention → trigger → crop → JPEG → candidate
    * Test source frame miss: trigger fires but no matching frame → no candidate, no suppression
    * Test crop failure: crop returns null → no candidate, no suppression
    * Test JPEG failure: encode returns null → no candidate, no suppression
    * Test duplicate suppression: second trigger for same trackingId → no emission
    * Test lifecycle stop: clears tracking, dwell, suppression, frame cache
    * Test lifecycle restart: starts fresh state
    * *Requirements: 5.6, 9.1–9.8, 10.1–10.7*

---

* [ ] 8. AppContainer Wiring and Lifecycle Integration

  * [ ] 8.1 Wire AttentionPipeline into AppContainer and ShoppingViewModel

    * Modify `app/AppContainer.kt`:

      * Add `AttentionPipeline` instantiation (lazy, same pattern as DetectionPipeline)
      * Expose as `AttentionCandidateProvider`
      * Pass `cameraFrameProvider` + `detectionResultProvider` to constructor

    * Modify `feature/shopping/ShoppingViewModel.kt`:

      * Accept `AttentionCandidateProvider` dependency
      * Collect `candidates` flow in `viewModelScope` (log for now)
      * Verify detection collection still works alongside attention collection

    * Modify `ShoppingViewModel.Factory` to accept new parameter.

    * Minimal changes only — preserve existing A1/A2 behavior.

    * **Key files:**

      * `app/AppContainer.kt` (MODIFIED)
      * `feature/shopping/ShoppingViewModel.kt` (MODIFIED)

    * **Done when:**

      ```bash
      ./gradlew clean assembleDebug
      ```

      succeeds. AttentionPipeline is instantiated and connected. Existing camera + detection flow still functions.

    * *Requirements: 10.5–10.7, 11.7–11.11*

  * [ ]* 8.2 Write integration test verifying lifecycle wiring

    * Test: shopping start → camera streaming → attention pipeline starts
    * Test: shopping stop → attention pipeline stops, state cleared
    * Test: restart → fresh state, pipeline resumes
    * *Requirements: 10.1–10.7*

---

* [ ] 9. Checkpoint — Full A3 Build and Integration

  * Ensure all tests pass, ask the user if questions arise.

  * Verify:

    ```bash
    ./gradlew clean assembleDebug
    ```

  * Verify architecture boundary:

    ```bash
    grep -r "org.tensorflow" app/src/main/java/ --include="*.kt" | grep -v "data/detection/"
    grep -r "com.meta" app/src/main/java/ --include="*.kt" | grep -v "data/meta/"
    ```

    Expected: no output.

  * Verify no modification to:
    * `CameraFrame.kt`
    * `CameraFrameProvider.kt`
    * `DetectionResult.kt`
    * `DetectionFrameResult.kt`
    * `DetectionResultProvider.kt`

  * Verify existing navigation remains functional.

  * *Requirements: 11.1–11.15, 12.9*

---

* [ ] 10. Real Gen2 Acceptance

  * [ ] 10.1 Execute complete A3 acceptance test on real Gen2

    * Run the full pipeline continuously for at least 60 seconds with generic objects.

    * Verify end-to-end path:

      ```text
      Gen2 CameraFrame
      → A2 DetectionFrameResult
      → Lightweight tracking (stable trackingId)
      → Center ROI evaluation
      → Occupancy evaluation
      → Dwell accumulation
      → center AND occupancy AND dwell trigger
      → Matching original CameraFrame by exact timestamp
      → Padded bbox crop from original 504×896 frame
      → Min crop size validation
      → JPEG encoding at quality 85
      → AttentionCandidate emitted
      ```

    * Verify via Logcat diagnostics:

      ```text
      trackingId assignment/persistence
      occupancyRatio values
      dwellMs accumulation
      trigger event
      source frame match success
      crop dimensions
      JPEG byte size
      capturedAt ISO 8601 UTC
      ```

    * Verify duplicate suppression:

      * Hold an object in center → first trigger → candidate emitted
      * Continue holding → no second candidate for same trackingId
      * Remove and re-present → new trackingId → new candidate

    * Verify lifecycle:

      ```text
      Shopping start → camera → detection → attention pipeline active
      Shopping stop → attention pipeline stops, state cleared
      Shopping restart → fresh state, pipeline resumes
      Repeated start/stop cycles stable
      ```

    * Verify source frame correctness:

      * No crop from unrelated frame
      * Source frame miss → candidate discarded, no suppression commit
      * Retry possible for same track on next qualifying frame

    * Architecture verification:

      ```bash
      grep -r "org.tensorflow" app/src/main/java/ --include="*.kt" | grep -v "data/detection/"
      grep -r "com.meta" app/src/main/java/ --include="*.kt" | grep -v "data/meta/"
      ```

      Expected: no output.

    * Verify no modification to existing domain contracts.

    * Verify no Backend HTTP calls made.

    * Verify no Backend DTOs introduced.

    * Final build:

      ```bash
      ./gradlew clean assembleDebug
      ```

    * **Done when:**

      * Real Gen2 end-to-end attention path produces valid AttentionCandidate.
      * Duplicate suppression confirmed.
      * Source-frame exact-match confirmed.
      * Stop/restart confirmed stable.
      * Architecture boundary clean.
      * No Backend integration in A3.

    * *Requirements: 1–12 (all)*

---

## Notes

* **A0/A1/A2 preservation:** `CameraFrame`, `CameraFrameProvider`, `DetectionResult`, `DetectionFrameResult`, `DetectionResultProvider`, `data/meta/`, and `data/detection/` MUST remain unchanged unless an explicit blocker is discovered.
* **Source frame cache:** Do NOT use `conflate()` on the source frame collector. Cache is bounded by `SOURCE_FRAME_CACHE_MAX_SIZE` with oldest-eviction.
* **One-to-one tracking:** Each detection maps to at most one track per frame. No multi-assignment.
* **Suppression commit order:** Suppression is committed ONLY AFTER candidate is successfully emitted. Any failure before emission leaves the track eligible for retry.
* **Crop source:** Always from original Gen2 `CameraFrame` (504×896), never from the 300×300 detector input.
* **YUV conversion for crop:** Reuse the existing NV21 → YuvImage → JPEG → Bitmap path. Do not resize to 300×300.
* **capturedAt vs frameTimestampUs:** `capturedAt` = wall-clock ISO 8601 UTC for Backend. `frameTimestampUs` = SDK-relative monotonic for tracking/dwell/source-frame matching only.
* **Bitmap safety:** Always capture width/height before calling `recycle()`.
* **Out of scope (A4):** Remote repositories, Retrofit, `/recognize` endpoint, product data, pricing, product card rendering.
* **Hackathon priority:** Reach the first real Gen2 attention candidate as quickly as possible, then verify duplicate suppression and lifecycle.
* Tasks marked with `*` are optional and can be skipped for faster MVP.

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "2.1"] },
    { "id": 2, "tasks": ["2.2", "3.1"] },
    { "id": 3, "tasks": ["3.2", "4.1"] },
    { "id": 4, "tasks": ["4.2", "5.1", "5.2"] },
    { "id": 5, "tasks": ["5.3", "6"] },
    { "id": 6, "tasks": ["7.1"] },
    { "id": 7, "tasks": ["7.2", "8.1"] },
    { "id": 8, "tasks": ["8.2", "9"] },
    { "id": 9, "tasks": ["10.1"] }
  ]
}
```
