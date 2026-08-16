# Requirements Document

## Introduction

This document specifies the requirements for Stage A2: On-Device Object Detection.

The goal of A2 is to consume the SDK-independent `CameraFrame` stream produced by A1, perform on-device object detection, and expose detection results (bounding box, class label, confidence) through an SDK-independent boundary that downstream Attention Policy (A3) can consume.

A1 already established the `CameraFrameProvider` / `CameraFrame` contract. A2 SHALL consume this contract as-is without redesigning A0 or A1 components.

A2 ends at the following boundary:

```text
CameraFrame (from CameraFrameProvider.frames)
→ Frame Sampling / Backpressure
→ Format Verification & Conversion
→ Object Detection (on-device)
→ SDK-independent DetectionResult (bbox, class, confidence)
```

Tracking, dwell time, attention trigger, candidate selection, backend recognition, and product identification are outside the scope of A2.

---

## Glossary

* **App**: The Android application targeting Meta Ray-Ban Gen 2 smart glasses shopping assistance, established in A0 and extended in A1
* **CameraFrame**: An app-owned, SDK-independent representation of a camera frame produced by the A1 camera pipeline
* **CameraFrameProvider**: The SDK-independent domain interface providing a `Flow<CameraFrame>` and observable camera state
* **Detection_Pipeline**: The A2 component responsible for receiving sampled frames, converting them to a detector-compatible format, and executing object detection
* **Object_Detector**: An on-device machine learning model that accepts image input and outputs bounding boxes with class labels and confidence scores
* **Detector_Adapter**: A component inside the `data/detection/` boundary responsible for interacting with the chosen ML SDK and converting SDK-specific results into app-owned domain types
* **DetectionResult**: An SDK-independent domain model representing a single detected object with bounding box coordinates, class label, and confidence score
* **Frame_Sampler**: A component responsible for selecting which incoming `CameraFrame` instances are forwarded to the Detection_Pipeline, applying backpressure and sampling strategies
* **ML_SDK**: The chosen machine learning inference library (e.g., TensorFlow Lite, ML Kit) used internally by the Detector_Adapter
* **BoundingBox**: A rectangular region within a frame, expressed in normalized coordinates (0.0–1.0) relative to frame dimensions

---

## Requirements

### Requirement 1: CameraFrame Format Verification on Gen2

**User Story:**
As a developer, I want to empirically determine the actual byte format of `CameraFrame.data` on real Gen2 hardware, so that the detection pipeline can be designed around verified data rather than assumptions.

#### Acceptance Criteria

1. WHEN a `CameraFrame` with `isCompressed = false` is received on real Gen2 hardware, THE Detection_Pipeline SHALL log the frame width, height, data byte length, bytes-per-pixel ratio (data length divided by width times height), and stride alignment (data length divided by height).

2. WHEN a CameraFrame with isCompressed = true is received on real Gen2 hardware,
THE Detection_Pipeline SHALL inspect the frame data and available metadata
to determine the actual compression codec or format without assuming a specific codec.

3. THE App SHALL include a diagnostic mechanism that classifies `CameraFrame.data` against the candidate pixel formats (NV21, NV12, YUV_420_888, RGBA) by verifying that the data byte length equals the expected length for the frame's width and height under each format's byte-per-pixel formula (e.g., width × height × 1.5 for NV21/NV12/YUV_420_888, width × height × 4 for RGBA).

4. IF the diagnostic mechanism cannot match `CameraFrame.data` to any candidate pixel format, THEN THE Detection_Pipeline SHALL log the frame dimensions, actual byte length, computed bytes-per-pixel ratio, and label the format as "UNKNOWN".

5. IF real Gen2 hardware is unavailable during initial development, THEN THE Detection_Pipeline SHALL use a configurable format assumption (defaulting to NV21) annotated in the configuration with a `verified = false` flag, and log a warning at startup indicating the format is unverified.

6. THE format verification mechanism SHALL execute outside the Android main UI thread.

7. WHEN format verification completes for the first successfully classified frame, THE Detection_Pipeline SHALL emit a structured diagnostic record containing the determined format label, frame dimensions, byte length, and bytes-per-pixel ratio, accessible to automated tests.

---

### Requirement 2: Frame Sampling and Backpressure

**User Story:**
As a developer, I want to control how many camera frames enter the detection pipeline, so that the detector operates within its processing budget without accumulating unbounded backlog.

#### Acceptance Criteria

1. THE Frame_Sampler SHALL receive `CameraFrame` instances from `CameraFrameProvider.frames`.

2. THE Frame_Sampler SHALL NOT forward every incoming `CameraFrame` to the Detection_Pipeline.

3. THE Frame_Sampler SHALL maintain an internal buffer of at most 1 `CameraFrame`, replacing any buffered frame with the newest arrival so that unprocessed frames never accumulate beyond that single-slot buffer.

4. WHILE the Detection_Pipeline is still processing a previous frame, THE Frame_Sampler SHALL replace the buffered frame with the most recently received `CameraFrame` and discard all intermediate frames.

5. WHEN the Detection_Pipeline becomes ready for the next detection cycle, THE Frame_Sampler SHALL deliver the most recently buffered `CameraFrame` to the Detection_Pipeline.

6. THE Frame_Sampler SHALL expose a configurable parameter for the minimum interval between consecutive frame deliveries to the Detection_Pipeline, with a default value defined in a configuration source and a valid range of 66 ms to 1000 ms (approximately 1 to 15 deliveries per second).

7. THE Frame_Sampler SHALL execute all frame collection and delivery logic on a background coroutine dispatcher and SHALL NOT block the Android main UI thread.

8. THE Frame_Sampler SHALL NOT modify or redesign the existing `CameraFrameProvider` interface or `CameraFrame` model.

9. IF `CameraFrameProvider.frames` completes or throws an exception, THEN THE Frame_Sampler SHALL stop delivering frames to the Detection_Pipeline and propagate the termination signal without crashing.

---

### Requirement 3: Frame Format Conversion

**User Story:**
As a developer, I want camera frame bytes converted into a format the object detector can accept, so that detection can proceed regardless of the raw frame format.

#### Acceptance Criteria

1. WHEN a sampled `CameraFrame` is ready for detection, THE Detection_Pipeline SHALL convert `CameraFrame.data` into the input format required by the Object_Detector and produce the result within 50 ms per frame on the target device.

2. IF `CameraFrame.isCompressed` is true, THEN THE Detection_Pipeline SHALL decode the compressed data into raw pixel data before passing it to the Object_Detector.

3. IF `CameraFrame.isCompressed` is false, THEN THE Detection_Pipeline SHALL convert the raw pixel data from its verified format into the detector-required input format without an intermediate decoding step.

4. THE format conversion logic SHALL handle the empirically verified Gen2 frame format determined by Requirement 1.

5. THE format conversion SHALL execute on a background thread, never on the Android main UI thread.

6. IF the format conversion encounters unrecognized or corrupt frame data that cannot be converted, THEN THE Detection_Pipeline SHALL discard the frame without crashing the App and record a diagnostic entry containing the frame timestamp and failure reason.

7. THE format conversion SHALL NOT require changes to the existing `CameraFrame` model or `data/meta/` package.

8. THE Detection_Pipeline SHALL reuse or pool conversion buffers across frames rather than allocating unbounded memory per conversion, limiting concurrent buffer allocation to a maximum of 3 buffers.

---

### Requirement 4: On-Device Object Detection

**User Story:**
As a developer, I want on-device object detection that identifies generic objects in camera frames, so that downstream components can evaluate which objects the user is looking at.

#### Acceptance Criteria

1. WHEN the Object_Detector receives a converted frame, THE Object_Detector SHALL run inference and produce zero or more detection results for that frame.

2. THE Object_Detector SHALL represent each detection result as a bounding box in normalized coordinates (0.0 to 1.0 relative to frame dimensions), a class label string, and a confidence score ranging from 0.0 to 1.0.

3. THE Object_Detector SHALL operate entirely on-device without network calls.

4. THE Object_Detector SHALL support detecting multiple objects in a single frame up to a configured maximum of 20 detections per frame.

5. THE Object_Detector inference SHALL execute outside the Android main UI thread.

6. THE Object_Detector SHALL complete inference for a single frame within 200 ms on the target device.

7. IF inference fails or exceeds the 200 ms time budget for a given frame, THEN THE Detection_Pipeline SHALL discard that frame without producing results and continue processing subsequent frames without crashing the App.

8. THE Object_Detector SHALL detect generic object categories (e.g., "handbag", "bottle", "watch") rather than specific product SKUs or brand identifiers.

9. THE Object_Detector SHALL NOT call Backend endpoints, OpenAI APIs, or any external recognition service.

10. THE Object_Detector SHALL discard detection results with a confidence score below a configured minimum threshold (initial value: 0.3) before returning results to downstream components.

---

### Requirement 5: ML SDK Type Isolation

**User Story:**
As a developer, I want ML SDK types isolated behind a dedicated data boundary, so that domain and presentation layers remain independent of the chosen inference library.

#### Acceptance Criteria

1. ALL production ML SDK usage SHALL remain inside the `data/detection/` boundary.

2. THE App SHALL NOT import or expose ML SDK types outside the `data/detection/` boundary.

3. Domain, ViewModel, and Presentation components SHALL NOT depend directly on ML SDK classes.

4. THE Detector_Adapter SHALL convert ML SDK-specific detection outputs into app-owned `DetectionResult` instances before exposing results outside the `data/detection/` boundary.

5. Public APIs exposed outside `data/detection/` SHALL use only SDK-independent application types.

6. WHEN the ML SDK choice changes in the future, THEN only code inside `data/detection/` SHALL require modification.

---

### Requirement 6: DetectionResult Contract

**User Story:**
As a developer, I want an SDK-independent detection result model, so that downstream Attention Policy and UI components can consume detection output without knowledge of the inference library.

#### Acceptance Criteria

1. THE DetectionResult SHALL include a bounding box expressed in normalized coordinates (0.0–1.0) relative to the source frame dimensions, represented as four separate float fields: left, top, right, bottom.

2. THE DetectionResult SHALL include a class label as a non-empty string identifying the detected object category.

3. THE DetectionResult SHALL include a confidence score as a floating-point value in the range [0.0, 1.0] inclusive.

4. THE DetectionFrameResult SHALL include the timestamp of the source CameraFrame,
expressed in microseconds.

5. THE DetectionFrameResult SHALL contain zero or more DetectionResult instances
produced from that CameraFrame.

6. THE DetectionResult type SHALL exist outside the `data/detection/` boundary in the `domain/model/` package accessible to downstream components.

7. THE DetectionResult SHALL NOT reference ML SDK classes, annotations, constants, or enums.

8. THE DetectionResult bounding box SHALL use a coordinate system where (0.0, 0.0) represents the top-left corner and (1.0, 1.0) represents the bottom-right corner of the frame.

---

### Requirement 7: Detection Output Delivery

**User Story:**
As a developer, I want detection results delivered through an observable stream, so that downstream components can react to detections as they arrive.

#### Acceptance Criteria

1. THE Detection_Pipeline SHALL expose detection output as a Kotlin
Flow<DetectionFrameResult>.

2. THE Detection_Pipeline SHALL emit exactly one DetectionFrameResult
for each processed CameraFrame.

3. WHEN no objects are detected in a processed frame,
THE Detection_Pipeline SHALL emit a DetectionFrameResult containing
the source frame timestamp and an empty detections list.

4. THE detection output Flow SHALL use only domain-layer types (DetectionResult) with no transitive compile dependency on the ML SDK module, so that domain-level and presentation-level components can collect the Flow without importing ML SDK packages.

5. THE detection output boundary SHALL be expressed as an SDK-independent interface defined in the domain layer.

6. THE interface SHALL NOT expose start, stop, close, or any other detector lifecycle methods to downstream consumers.

7. IF the downstream collector is slower than the Detection_Pipeline emission rate, THEN THE Detection_Pipeline SHALL drop the oldest unconsumed emission and deliver the most recent result, so that the buffer never exceeds 1 unconsumed frame.

8. WHILE the shopping session is inactive, THE Detection_Pipeline SHALL NOT emit detection results.

---

### Requirement 8: Detection Pipeline Lifecycle

**User Story:**
As a developer, I want the detection pipeline lifecycle tied to the camera stream, so that detection resources are acquired when frames are available and released when the camera stops.

#### Acceptance Criteria

1. WHEN `CameraFrameProvider` transitions to STREAMING state, THE Detection_Pipeline SHALL begin consuming frames within 500 ms of the first frame emission.

2. WHEN `CameraFrameProvider` stops emitting frames (camera stops or enters error state), THE Detection_Pipeline SHALL discard any in-flight frames, cease processing, and release ML resources within 2000 ms.

3. THE Detection_Pipeline lifecycle SHALL support at least 10 consecutive start/stop cycles without crashes and without cumulative memory growth exceeding 5 MB above the post-first-cycle baseline.

4. THE Detection_Pipeline SHALL NOT start detection before the camera is in STREAMING state.

5. IF the Detection_Pipeline encounters a fatal initialization error (e.g., model file missing or model load failure), THEN THE Detection_Pipeline SHALL transition to an error state that exposes an SDK-independent error description observable by the ViewModel, without crashing the App and without requiring an App restart to retry.

6. WHEN detection stops, THE Detection_Pipeline SHALL release the ML model and associated native memory before reporting its state as IDLE.

7. IF `CameraFrameProvider` transitions to STREAMING while the Detection_Pipeline is still releasing resources from a previous cycle, THEN THE Detection_Pipeline SHALL complete the release before starting a new detection cycle.

---

### Requirement 9: Performance Measurement on Real Gen2

**User Story:**
As a developer, I want measurable performance metrics from the real Gen2 detection pipeline, so that I can verify the system operates within acceptable real-time bounds.

#### Acceptance Criteria

1. THE App SHALL log the end-to-end latency in milliseconds from `CameraFrame` receipt to `DetectionResult` emission for each processed frame.

2. THE App SHALL log the effective detection frames-per-second (detection FPS) calculated over a rolling 5-second window during continuous operation.

3. THE App SHALL log the number of frames dropped by the Frame_Sampler during each 5-second measurement window.

4. THE App SHALL log the inference-only duration in milliseconds (excluding format conversion) for each detection cycle.

5. THE performance measurement mechanism SHALL NOT increase mean end-to-end frame latency by more than 5% and SHALL NOT reduce detection FPS by more than 1 frame compared to operation without measurement enabled.

6. WHILE running in a debug build, THE App SHALL emit performance metrics via standard Android logging (Logcat) with a consistent tag prefix identifiable by filtering.

7. THE measurements SHALL be collected on real Gen2 hardware to validate actual pipeline throughput.

8. IF real Gen2 hardware is unavailable during initial development, THEN THE App SHALL support collecting the same metrics defined in criteria 1–4 using Mock Device frames as a provisional baseline.

---

### Requirement 10: Existing Architecture and Functionality Preservation

**User Story:**
As a developer, I want A2 to integrate object detection without destabilizing the architecture completed in A0 and A1.

#### Acceptance Criteria

1. THE existing architecture SHALL remain layered as Presentation → Domain ← Data, where no source file in `domain/` or `feature/` imports any class from `data/` except through interfaces defined in `domain/`.

2. A2 SHALL NOT introduce Hilt or another dependency injection framework; all dependency wiring SHALL remain in the manual `AppContainer` structure.

3. A2 SHALL NOT modify the existing `CameraFrameProvider` interface; its source file SHALL be byte-identical to its state at A1 completion.

4. A2 SHALL NOT modify the existing `CameraFrame` model; its source file SHALL be byte-identical to its state at A1 completion.

5. A2 SHALL NOT modify any source file inside the `data/meta/` package; all files in that package SHALL be byte-identical to their state at A1 completion.

6. A2 SHALL register all new detection-related components in the existing `AppContainer` manual DI structure so that they are accessible to consuming layers without framework annotations.

7. WHEN A2 is complete, THE existing ViewModel classes SHALL retain all public method signatures, public property names, and exposed StateFlow/SharedFlow types unchanged from their A1 definitions.

8. WHEN A2 is complete, THE existing screen navigation SHALL allow launching each of the six screens (Home, Shopping, Review, Travel, Checklist, Recommendation) via their defined navigation routes without runtime crash or route resolution failure.

9. WHEN A2 is complete, THE existing fake repositories and fake Product Card functionality SHALL compile without modification and produce the same observable UI output as before A2 integration.

10. THE final A2 code SHALL pass `./gradlew clean assembleDebug` with zero errors and zero unresolved references.

11. WHEN A2 is complete, THE architecture SHALL confirm that no source file outside the `data/detection/` package contains import statements referencing ML model or ML SDK packages (e.g., `org.tensorflow`, `com.google.mlkit`); verification SHALL be performed by running a grep search across all source files excluding `data/detection/`.

---

### Requirement 11: Confidence Filtering

**User Story:**
As a developer, I want low-confidence detections filtered before they reach downstream components, so that only meaningful detections trigger further processing.

#### Acceptance Criteria

1. THE Detection_Pipeline SHALL apply a minimum confidence threshold to filter detection results before emitting them to downstream consumers.

2. DetectionResult instances with confidence strictly below the threshold SHALL NOT be emitted to downstream consumers; DetectionResult instances with confidence equal to or above the threshold SHALL be emitted.

3. THE confidence threshold SHALL be stored in the `AppConfig` configuration object rather than hardcoded as a literal in the detection logic.

4. THE initial default confidence threshold SHALL be 0.3 and SHALL be adjustable without code changes to the detection pipeline.

5. THE filtering SHALL occur inside the `data/detection/` boundary before results cross into domain layer.

---

# A2 Completion Criteria

A2 application integration SHALL be considered functionally complete when the following sequence is observable on real Gen2 hardware:

```text
Meta Gen2 camera active
→ CameraFrame emitted by CameraFrameProvider
→ Frame_Sampler selects frame for processing
→ Format verification / conversion executes
→ Object_Detector performs inference
→ DetectionResult (bbox, class, confidence) produced
→ DetectionResult observable at domain boundary
→ Latency and FPS metrics logged
→ Camera stops → detection stops → camera restarts → detection resumes
```

A2 completion SHALL additionally require:

* ML SDK types remain isolated inside `data/detection/`
* Domain, ViewModel, Presentation contracts remain ML-SDK-independent
* Frame processing does not block the Android main thread
* Frame backlog cannot grow without bound
* Existing A0/A1 functionality continues to work
* `data/meta/` has zero modifications
* `./gradlew clean assembleDebug` succeeds

---

# Real Device Acceptance Criterion

The final hardware acceptance path is:

```text
Meta Ray-Ban Gen 2
→ DAT Camera Stream
→ CameraFrame
→ Object Detection
→ DetectionResult with bbox, class, confidence visible in Logcat
```

If an actual Gen2 device is unavailable during A2 implementation, successful Mock Device detection SHALL NOT be treated as proof that this real-device acceptance criterion has passed.

The application integration may be considered implementation-complete while the real-device acceptance criterion remains explicitly pending.

---

# CameraFrame Format Verification Prerequisite

Before the final format conversion strategy is locked, the following MUST be empirically determined on real Gen2:

1. What is `CameraFrame.isCompressed` on actual Gen2 frames?
2. If `isCompressed = false`, what pixel format are the raw bytes? (NV21, NV12, YUV_420_888, RGBA, other?)
3. If `isCompressed = true`, what codec? (HEVC, H.264, other?)
4. Does the byte length match expectations for the reported width × height and format?
5. Can the ML SDK consume the format directly, or is explicit conversion required?

These findings SHALL be documented and used to finalize the format conversion implementation.

---

# Explicitly Out of Scope

A2 SHALL NOT implement:

* Object Tracking / trackingId persistence across frames
* Center ROI calculation
* Occupancy ratio calculation
* Dwell time calculation
* Attention Trigger logic
* Candidate selection or prioritization
* Recognition crop generation
* Backend `/recognize` calls
* OpenAI recognition
* Product ID resolution
* Real Product Card data from backend
* Product-specific detection (brand, SKU)
* Tracking ID fallback policies
* Detection threshold calibration UI
* Eye Tracking
* Hand Tracking
* New DI framework (Hilt, Koin, etc.)
* Changes to `data/meta/` package
* Changes to `CameraFrameProvider` or `CameraFrame`
* Navigation changes
* UI redesign
* Background Service or Wake Lock
* Backend streaming of camera feed
* WebSocket or SSE transport

A2 exists only to establish the reliable detection boundary required for the next stage:

```text
DetectionResult (bbox, class, confidence)
→ Tracking
→ Center / Occupancy / Dwell
→ Attention Trigger
→ Candidate Crop
→ Backend /recognize
```
