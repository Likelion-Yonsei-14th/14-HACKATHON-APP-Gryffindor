# Requirements Document

## Introduction

This document specifies the requirements for Stage A3: Attention Candidate Pipeline.

A3 consumes the SDK-independent `DetectionFrameResult` stream produced by A2 together with the original `CameraFrame` stream from A1, determines which detected object the user is meaningfully attending to, and produces a cropped JPEG image with metadata ready for Backend recognition in A4.

A3 ends at the following boundary:

```text
DetectionFrameResult (from DetectionResultProvider.detections)
+ CameraFrame (from CameraFrameProvider.frames)
→ Lightweight Object Tracking
→ Center ROI Evaluation
→ Occupancy Ratio Evaluation
→ Dwell Time Accumulation
→ Attention Trigger
→ Matching Original CameraFrame lookup
→ Crop from Original CameraFrame
→ JPEG Encoding
→ AttentionCandidate output
```

A3 does NOT call any HTTP endpoint.

Backend recognition requests, multipart upload, product response handling, product card rendering, and shopping session Backend APIs are outside the scope of A3.

---

## Glossary

* **App**: The Android application targeting Meta Ray-Ban Gen 2 smart glasses shopping assistance.
* **DetectionFrameResult**: An SDK-independent A2 result containing zero or more `DetectionResult` instances for a processed camera frame.
* **DetectionResult**: A single detected object containing normalized bounding box coordinates, label, and confidence.
* **DetectionResultProvider**: The domain interface exposing A2 detection results.
* **CameraFrame**: An app-owned, SDK-independent frame produced by the A1 camera pipeline containing original Gen2 frame data.
* **CameraFrameProvider**: The domain interface exposing original Gen2 camera frames.
* **Attention_Pipeline**: The A3 component coordinating tracking, attention evaluation, source-frame matching, crop generation, and candidate emission.
* **Object_Tracker**: A lightweight frame-to-frame object association component.
* **Center_ROI**: A configurable rectangular region near the center of the camera frame.
* **Occupancy_Ratio**: The ratio of a detection bounding-box area to total frame area.
* **Dwell_Time**: The duration for which the same tracked object continuously satisfies the attention-region condition.
* **Attention_Trigger**: The policy determining when an object qualifies for recognition.
* **AttentionCandidate**: The final A3 output containing JPEG crop bytes and recognition metadata.
* **Shopping_Session**: The active shopping period during which the user is observing products; all A3 state is scoped to a single session

## Requirements
# Requirements

## Requirement 1: Lightweight Object Tracking

**User Story:** As a developer, I want lightweight frame-to-frame object association so that dwell time can be accumulated for the same observed object.

### Acceptance Criteria

1. WHEN a new `DetectionFrameResult` arrives, THE Object_Tracker SHALL attempt to associate each current `DetectionResult` with an active tracked object using lightweight bounding-box spatial similarity.

2. THE tracking implementation SHALL use simple characteristics available from the existing detection result, such as:

   * bounding-box center distance
   * bounding-box overlap or size similarity
   * detection label when useful for avoiding clearly invalid associations

3. THE Object_Tracker SHALL assign a stable `Tracking_Id` to detections considered to represent the same observed object across consecutive processed frames.

4. WHEN a detection cannot reasonably be associated with an existing tracked object, THE Object_Tracker SHALL create a new tracked object with a new `Tracking_Id`.

5. THE Object_Tracker MAY retain temporarily missing tracks for a short configurable grace period to tolerate occasional missed detections.

6. WHEN a tracked object remains unmatched beyond the configured grace period, THE Object_Tracker SHALL remove the track.

7. THE tracking implementation SHALL remain intentionally lightweight for the hackathon MVP.

8. THE Object_Tracker SHALL NOT require Kalman Filter, Hungarian assignment, ByteTrack, DeepSORT, appearance embeddings, or another complex tracking framework.

9. THE Object_Tracker SHALL NOT modify the existing `DetectionResult`, `DetectionFrameResult`, or `DetectionResultProvider` contracts.

10. Configurable tracking thresholds SHALL use named configuration values rather than unexplained hardcoded literals.

11. Tracking SHALL execute without blocking the Android main UI thread.

---

## Requirement 2: Center ROI and Occupancy Evaluation

**User Story:** As a developer, I want to determine whether a detected object is sufficiently central and sufficiently large in the user's field of view so that distant or peripheral objects are filtered before recognition.

### Acceptance Criteria

1. THE Attention_Pipeline SHALL define a configurable Center_ROI with the initial hackathon values:

```text
horizontal central region = 70%
vertical central region = 80%
```

Equivalent normalized boundaries:

```text
left   = 0.15
right  = 0.85
top    = 0.10
bottom = 0.90
```

2. WHEN evaluating a detection, THE Attention_Pipeline SHALL compute the bounding-box center using the normalized coordinates.

3. A detection SHALL be considered inside the Center_ROI only when its bounding-box center is inside both the configured horizontal and vertical ranges.

4. THE Attention_Pipeline SHALL compute `Occupancy_Ratio` from the normalized bounding-box area.

5. THE initial minimum occupancy threshold SHALL be:

```text
minOccupancyRatio = 0.12
```

6. Center ROI boundaries and occupancy threshold SHALL be configurable using named application configuration values.

7. IF normalized bbox coordinates are invalid or unusable, THE Attention_Pipeline SHALL safely ignore the detection rather than crashing.

8. Center and occupancy evaluation SHALL not block the Android main UI thread.

---

## Requirement 3: Dwell Time Accumulation

**User Story:** As a developer, I want to measure how long the same tracked object remains meaningfully observed so that transient detections do not cause Backend recognition requests.

### Acceptance Criteria

1. THE Attention_Pipeline SHALL accumulate Dwell_Time for a tracked object while that object continues to satisfy the Center_ROI condition.

2. Dwell_Time SHALL use the existing `frameTimestampUs` associated with detection processing.

3. `frameTimestampUs` SHALL be used only for relative pipeline timing such as tracking and dwell calculation.

4. `frameTimestampUs` SHALL NOT be assumed to represent UTC wall-clock time.

5. WHEN a tracked object leaves the Center_ROI, loses its valid tracking association, or remains undetected beyond the configured grace period, its current dwell accumulation SHALL be reset.

6. WHEN the object later becomes eligible again, dwell accumulation SHALL begin again from zero.

7. The initial dwell threshold SHALL be:

```text
minDwellMs = 800
```

8. An abnormally large timestamp gap caused by pause, delayed collection, or lifecycle interruption SHALL NOT incorrectly cause an immediate attention trigger.

9. Dwell timing logic SHALL remain independent of the effective detection FPS.

10. Dwell processing SHALL not block the Android main UI thread.

---

## Requirement 4: Attention Trigger Policy

**User Story:** As a developer, I want recognition to trigger only when an object is central, sufficiently large, and observed long enough so that wide-angle background objects do not produce unnecessary Backend requests.

### Acceptance Criteria

1. THE Attention_Trigger SHALL require all of the following conditions:

```text
object center is within Center_ROI
AND Occupancy_Ratio >= minOccupancyRatio
AND Dwell_Time >= minDwellMs
```

2. The initial trigger thresholds SHALL be:

```text
minOccupancyRatio = 0.12
minDwellMs = 800
```

3. The production A3 attention path SHALL therefore represent the trigger as:

```text
OCCUPANCY_AND_DWELL
```

4. THE trigger type model MAY preserve the Backend-compatible values:

```text
OCCUPANCY
DWELL
OCCUPANCY_AND_DWELL
```

but A3 SHALL NOT be required to implement three separate attention policies.

5. IF multiple tracked objects satisfy the complete trigger policy in the same evaluation cycle, THE Attention_Pipeline SHALL emit at most one candidate.

6. Candidate selection SHALL prefer the object whose bounding-box center is closest to the frame center.

7. IF an additional tie-breaker is necessary, THE larger Occupancy_Ratio SHOULD be preferred.

8. THE Attention_Trigger SHALL NOT perform Backend communication.

9. Attention policy logic SHALL remain SDK-independent.

---

## Requirement 5: Original CameraFrame Association

**User Story:** As a developer, I want a detection to be associated with the original frame from which it was produced so that its bounding box is applied to the correct image.

### Acceptance Criteria

1. THE Attention_Pipeline SHALL NOT apply a detection bbox to an arbitrary latest camera frame solely because that frame was received recently.

2. THE pipeline SHALL associate the triggering `DetectionFrameResult.frameTimestampUs` with the corresponding original `CameraFrame.timestampUs`.

3. THE pipeline SHALL maintain only a small bounded set of recent original `CameraFrame` instances required for matching processed detections back to their source frame.

4. THE frame association mechanism SHALL remain bounded and SHALL NOT allow camera frames to accumulate indefinitely.

5. WHEN an attention trigger fires, THE pipeline SHALL retrieve the original source frame matching the triggering detection timestamp.

6. IF the correct source `CameraFrame` is unavailable, expired, or cannot be reliably matched, THE pipeline SHALL discard that candidate rather than crop a different frame.

7. Frame association SHALL NOT require modifications to the existing `CameraFrame` or `DetectionFrameResult` public/domain contracts.

8. Any frame cache SHALL be cleared when the shopping session ends.

---

## Requirement 6: Crop Generation from Original CameraFrame

**User Story:** As a developer, I want to crop the attended object from the original Gen2 frame so that Backend recognition receives the highest-quality available product image rather than the detector's resized input.

### Acceptance Criteria

1. WHEN an Attention_Trigger fires, THE crop SHALL be generated from the matched original Gen2 `CameraFrame`.

2. THE crop SHALL NOT be generated from the 300×300 TFLite detector input.

3. THE initial crop padding policy SHALL use:

```text
cropPaddingRatio = 0.15
```

4. Padding SHALL be applied relative to the detected bbox size.

5. Padded crop coordinates SHALL be clamped safely to the original frame boundaries.

6. The initial minimum crop short-side requirement SHALL be:

```text
minCropShortSide = 160 px
```

7. IF the valid padded crop has a shorter side below the configured minimum, THE Attention_Pipeline SHALL discard the candidate.

8. The initial maximum output crop long side SHALL be:

```text
maxCropLongSide = 1024 px
```

9. IF a future camera source produces a crop larger than the configured maximum, THE pipeline MAY proportionally downscale it while preserving aspect ratio.

10. With the current Gen2 504×896 frame, the maximum-long-side rule SHOULD NOT require unnecessary upscaling.

11. Crop generation failure SHALL NOT crash the App.

12. Crop generation SHALL execute outside the Android main UI thread.

---

## Requirement 7: JPEG Encoding

**User Story:** As a developer, I want a triggered crop converted to JPEG so that A4 can send it directly to the Backend recognition endpoint.

### Acceptance Criteria

1. A valid crop SHALL be encoded as JPEG.

2. The initial JPEG quality SHALL be:

```text
jpegQuality = 85
```

3. JPEG quality SHALL be a named configurable value.

4. THE encoded result SHALL be a non-empty `ByteArray`.

5. JPEG encoding failure SHALL cause only the affected candidate to be discarded.

6. JPEG encoding SHALL execute outside the Android main UI thread.

7. A3 SHALL NOT invent a Backend upload-size constraint that is not defined by the Backend API contract.

8. Any Backend-defined payload size constraint SHALL be handled during A4 integration if required.

---

## Requirement 8: AttentionCandidate Output Contract

**User Story:** As a developer, I want an SDK-independent candidate object containing the crop and trigger metadata required by the next Backend integration stage.

### Acceptance Criteria

1. THE `AttentionCandidate` SHALL contain at least:

```text
jpegBytes
capturedAt
triggerType
occupancyRatio
dwellMs
trackingId (optional)
```

2. Additional SDK-independent crop context such as output dimensions MAY be included when useful to the application.

3. `jpegBytes` SHALL contain the JPEG representation produced by A3.

4. `occupancyRatio` SHALL represent the occupancy value used by the attention policy.

5. `dwellMs` SHALL represent the accumulated dwell value at trigger time.

6. `trackingId` SHALL identify the current lightweight track when available.

7. `capturedAt` SHALL be generated from an Android wall-clock source and represented in ISO 8601 UTC format suitable for the Backend contract.

8. `capturedAt` SHALL NOT be produced by directly interpreting `frameTimestampUs` as UTC time.

9. THE `AttentionCandidate` domain model SHALL NOT reference:

   * Meta DAT types
   * TensorFlow Lite types
   * Retrofit types
   * Backend request DTO types

10. A3 SHALL expose candidates through an SDK-independent observable interface such as a Kotlin `Flow<AttentionCandidate>`.

11. A3 SHALL NOT construct multipart HTTP requests.

---

## Requirement 9: Duplicate Trigger Suppression

**User Story:** As a developer, I want to avoid repeatedly producing candidates for an object that is continuously being tracked so that recognition requests are not unnecessarily duplicated.

### Acceptance Criteria

1. A tracked object SHALL emit at most one AttentionCandidate while it remains continuously associated with the same active attention event.

2. AFTER a tracked object emits a candidate, continued frames for the same active `Tracking_Id` SHALL NOT repeatedly emit additional candidates merely because the attention thresholds remain satisfied.

3. A new candidate MAY become eligible when the previous tracking or attention association has genuinely ended and the object is later observed as a new attention event.

4. Losing a track beyond the tracking grace period and creating a new track SHALL be considered a new attention event.

5. Shopping session restart SHALL reset duplicate suppression state.

6. Duplicate suppression SHALL use session-local in-memory state only.

7. A3 SHALL NOT require a fixed ten-second periodic re-trigger for an object that remains continuously tracked.

8. A3 SHALL NOT persist duplicate-suppression state to disk.

---

## Requirement 10: Shopping Lifecycle and State Reset

**User Story:** As a developer, I want A3 state scoped to the current shopping lifecycle so that stale tracks and attention events do not survive between shopping sessions.

### Acceptance Criteria

1. WHEN shopping stops, THE A3 pipeline SHALL clear session-scoped state including:

```text
active tracks
dwell state
duplicate-trigger state
recent source-frame matching state
```

2. WHEN shopping restarts, THE Attention_Pipeline SHALL start with fresh A3 state.

3. WHILE shopping is inactive, THE Attention_Pipeline SHALL NOT produce `AttentionCandidate` outputs.

4. IF shopping stops while a candidate is being processed, THE pipeline SHALL avoid emitting stale results after the session has ended.

5. A3 lifecycle behavior SHALL integrate with the existing shopping/camera lifecycle rather than introducing an unrelated lifecycle system.

6. Lifecycle integration SHALL NOT require manual per-frame control from the ViewModel.

7. Repeated shopping start/stop/restart operation SHALL remain stable on the real Gen2 path.

---

## Requirement 11: Architecture Boundary Preservation

**User Story:** As a developer, I want A3 integrated with minimal changes to the validated A0/A1/A2 architecture.

### Acceptance Criteria

1. A3 SHALL preserve the existing public/domain behavior of:

```text
CameraFrame
CameraFrameProvider
DetectionResult
DetectionFrameResult
DetectionResultProvider
```

unless an unexpected blocker is explicitly identified.

2. A3 SHALL NOT introduce Meta DAT SDK types outside the existing `data/meta/**` boundary.

3. A3 SHALL NOT introduce TensorFlow Lite or other ML SDK types outside the existing `data/detection/**` boundary.

4. A3 attention and tracking domain models SHALL remain SDK-independent.

5. A3-specific implementation MAY use a dedicated package such as:

```text
domain/attention/**
data/attention/**
```

or an equivalent structure consistent with the existing project architecture.

6. A3 MAY extend `AppConfig` with attention, tracking, crop, and encoding configuration.

7. A3 MAY minimally modify `AppContainer` and existing application/lifecycle wiring required to register and connect the new A3 pipeline.

8. A3 MAY minimally modify other existing integration files only when required to connect A3 to the established A1/A2 flows.

9. A3 SHALL avoid unnecessary modifications to validated A1/A2 implementation internals.

10. Changes inside `data/meta/**` or the TFLite detection implementation SHOULD be avoided unless a concrete integration blocker is discovered.

11. A3 SHALL use the existing manual dependency-injection approach.

12. A3 SHALL NOT introduce Hilt, Koin, or another DI framework.

13. A3 SHALL NOT define Backend request/response DTOs.

14. A3 SHALL NOT implement Retrofit recognition calls.

15. The completed implementation SHALL continue to build successfully with the project's existing Gradle verification flow.

---

## Requirement 12: Threading, Backpressure, and MVP Performance

**User Story:** As a developer, I want A3 to remain bounded and responsive so that it does not interfere with the already validated camera and object-detection pipeline.

### Acceptance Criteria

1. CPU-heavy A3 work including crop generation and JPEG encoding SHALL NOT execute on the Android main UI thread.

2. A3 SHALL NOT introduce an unbounded queue of detection results or camera frames.

3. Recent source frames retained for timestamp matching SHALL use a bounded structure.

4. Candidate emission SHALL use bounded buffering or equivalent backpressure behavior suitable for recognition requests.

5. IF a downstream consumer is slower than candidate production, A3 SHALL prioritize bounded operation rather than accumulating unlimited candidates.

6. A3 SHALL avoid concurrent crop/encoding work growing without bound.

7. A3 SHOULD provide lightweight diagnostic logging sufficient to verify:

```text
trackingId
occupancyRatio
dwellMs
trigger
source frame match success/failure
crop dimensions
JPEG byte size
```

8. Image binary or base64 contents SHALL NOT be logged.

9. A3 SHALL preserve the responsiveness and lifecycle stability already demonstrated by A1/A2.

10. Exact performance optimization beyond what is required for stable real-device operation SHALL be deferred unless measurements demonstrate a blocker.

---

# A3 Completion Criteria

A3 SHALL be considered functionally complete when the following sequence is observable:

```text
Gen2 CameraFrame
→ A2 DetectionFrameResult
→ lightweight tracking
→ Center ROI evaluation
→ Occupancy evaluation
→ Dwell accumulation
→ center AND occupancy AND dwell condition satisfied
→ Attention Trigger
→ matching original CameraFrame located by source timestamp
→ padded bbox crop from original frame
→ minimum crop size validation
→ JPEG encoding at quality 85
→ AttentionCandidate emitted
```

The candidate SHALL contain the information required for later A4 recognition integration:

```text
JPEG crop
capturedAt
triggerType
occupancyRatio
dwellMs
trackingId (optional)
```

A3 completion SHALL additionally verify:

* The same continuously tracked attention event does not repeatedly emit candidates.
* Missing or mismatched original source frames do not result in crops from unrelated frames.
* Shopping stop clears A3 state.
* Shopping restart begins with clean state.
* No Backend HTTP call is made.
* No Backend DTO is introduced.
* Existing A1/A2 SDK isolation remains intact.
* CPU-heavy work remains off the main thread.
* Frame and candidate buffering remain bounded.
* Existing app navigation and shopping lifecycle remain functional.
* The project builds successfully.

---

# Initial Android Attention Heuristics

The following are the initial real-device values for the B3 integration path:

```text
minOccupancyRatio = 0.12
minDwellMs = 800 ms

cropPaddingRatio = 0.15
minCropShortSide = 160 px
jpegQuality = 85
maxCropLongSide = 1024 px

Center ROI:
horizontal central 70%
vertical central 80%
```

These values are Android-side attention heuristics.

They SHALL NOT be treated as Backend request-validation constraints.

In particular:

```text
occupancyRatio >= 0.12
dwellMs >= 800
```

are conditions controlling when Android produces an attention candidate, not generic validity rules for the Backend API.

---

# Explicitly Out of Scope

A3 SHALL NOT implement:

* Backend `/recognize` HTTP calls
* Multipart request construction
* Backend recognition request DTOs
* Backend recognition response DTOs
* Product ID resolution
* Product data mapping
* Pricing response handling
* Product Card rendering
* Shopping session Backend creation/completion calls
* OpenAI recognition
* Backend Object Detection
* Backend bbox processing
* Backend occupancy calculation
* Backend dwell calculation
* Kalman Filter tracking
* Hungarian assignment
* ByteTrack
* DeepSORT
* appearance embedding tracking
* Eye Tracking
* Hand Tracking
* detection calibration UI
* new DI frameworks
* UI redesign
* navigation redesign
* persistent candidate storage
* Background Service
* Wake Lock
* WebSocket
* SSE
* push notifications

A3 exists only to establish the Android attention boundary needed by the next stage:

```text
AttentionCandidate
(JPEG crop + metadata)
→ A4 Backend /recognize integration
→ productId / product / pricing
→ Shopping UI
```
