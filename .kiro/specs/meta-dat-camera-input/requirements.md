# Requirements Document

## Introduction

This document specifies the requirements for Stage A1: Meta DAT Camera Input Integration.

The goal of A1 is to integrate the previously validated Meta Device Access Toolkit (DAT) camera pipeline into the existing Android application and expose app-owned `CameraFrame` objects through an SDK-independent boundary that downstream Object Detection (A2) can consume.

Stage 0 already validated DAT camera access, `VideoFrame` data extraction, frame ownership transfer, and stream lifecycle behavior using the Mock Device environment. A1 SHALL integrate those verified patterns into the Android architecture established in A0 rather than re-validating Stage 0 from the beginning.

A1 ends at the following boundary:

```text
Meta DAT
→ DeviceSession
→ Camera Stream
→ VideoFrame
→ Meta camera adapter
→ app-owned CameraFrame
→ A2 Object Detection input boundary
```

Object Detection, tracking, attention logic, backend recognition, and product identification are outside the scope of A1.

---

## Glossary

* **App**: The Android application targeting Meta Ray-Ban Gen 2 smart glasses shopping assistance, established in A0
* **DAT**: Meta Device Access Toolkit, the SDK providing camera access to supported Meta wearable devices
* **DeviceSession**: A DAT session object managing the connection and lifecycle of a Meta wearable device
* **Camera_Stream**: The DAT camera stream that delivers `VideoFrame` objects
* **VideoFrame**: A Meta DAT SDK type containing SDK-owned frame data and associated metadata
* **CameraFrame**: An app-owned, SDK-independent representation of a camera frame used by downstream application components
* **Meta_Camera_Adapter**: A component inside the `data/meta/` boundary responsible for interacting with DAT and converting SDK-owned frame data into app-owned data
* **Camera_State**: An SDK-independent representation of the current camera pipeline state
* **Frame_Consumer**: The SDK-independent boundary through which downstream components such as A2 Object Detection can consume `CameraFrame` instances
* **Shopping_Session**: The existing application flow in which the user starts and ends a shopping observation session

---

## Requirements

### Requirement 1: DAT Dependency Integration

**User Story:**
As a developer, I want the Meta DAT SDK dependencies integrated into the existing Android project, so that the application can access the Meta wearable camera stream.

#### Acceptance Criteria

1. THE App SHALL declare the `mwdat-core` and `mwdat-camera` dependencies required for DAT camera access.

2. THE DAT dependencies SHALL resolve and compile successfully alongside the existing A0 dependencies.

3. THE App SHALL declare `mwdat-mockdevice` only for the debug build environment.

4. THE App SHALL resolve DAT artifacts from the required package repository using credentials managed outside application source code.

5. THE App SHALL NOT hardcode or commit a Personal Access Token or equivalent package credential.

6. THE App SHALL compile successfully using:

```text
./gradlew assembleDebug
```

after DAT dependencies are integrated.

---

### Requirement 2: DeviceSession Lifecycle

**User Story:**
As a developer, I want the application to manage a DAT `DeviceSession`, so that wearable resources are acquired when shopping begins and released when shopping ends.

#### Acceptance Criteria

1. WHEN a shopping session starts, THE Meta_Camera_Adapter SHALL create and start a `DeviceSession`.

2. WHEN the shopping session ends, THE Meta_Camera_Adapter SHALL stop the active `DeviceSession` and release associated resources.

3. WHEN a stopped shopping session is started again, THE Meta_Camera_Adapter SHALL create a new `DeviceSession` rather than attempting to reuse an invalid stopped session.

4. THE DeviceSession lifecycle SHALL support the sequence:

```text
START
→ STOP
→ START
```

without crashing the App.

5. IF DeviceSession creation or startup fails, THEN THE Meta_Camera_Adapter SHALL expose an SDK-independent error state rather than allowing the failure to crash the App.

---

### Requirement 3: Camera Stream Lifecycle

**User Story:**
As a developer, I want the camera stream lifecycle tied to the active shopping session, so that camera resources are used only while shopping observation is active.

#### Acceptance Criteria

1. WHEN the active DeviceSession becomes capable of camera access, THE Meta_Camera_Adapter SHALL acquire the required camera capability and start the Camera_Stream.

2. WHEN the shopping session ends, THE Meta_Camera_Adapter SHALL stop the Camera_Stream and release the associated camera capability before final DeviceSession cleanup.

3. WHILE the Camera_Stream is active, THE Meta_Camera_Adapter SHALL receive DAT `VideoFrame` instances continuously according to the available stream frame rate.

4. WHEN a previously stopped camera pipeline is started again, THE Meta_Camera_Adapter SHALL resume producing `CameraFrame` instances.

5. THE camera lifecycle SHALL support repeated:

```text
start
→ stop
→ start
```

cycles without application crashes.

6. IF the Camera_Stream encounters an error, THEN THE Meta_Camera_Adapter SHALL transition to an appropriate SDK-independent Camera_State and perform safe resource cleanup.

---

### Requirement 4: Meta SDK Type Isolation

**User Story:**
As a developer, I want Meta DAT SDK types isolated behind a dedicated data boundary, so that the rest of the application remains independent of the hardware SDK.

#### Acceptance Criteria

1. ALL production Meta DAT SDK usage SHALL remain inside the `data/meta/` boundary.

2. Debug-only DAT or Mock Device integration code, if required, SHALL remain inside the corresponding `data/meta` source-set boundary.

3. THE App SHALL NOT import or expose Meta DAT SDK types outside the `data/meta/` boundary.

4. Domain, ViewModel, Presentation, and downstream A2 Object Detection components SHALL NOT depend directly on Meta DAT SDK classes.

5. THE Meta_Camera_Adapter SHALL convert SDK-owned `VideoFrame` data into app-owned `CameraFrame` instances before exposing frame data outside the `data/meta/` boundary.

6. Public APIs exposed outside `data/meta/` SHALL use only SDK-independent application types.

---

### Requirement 5: CameraFrame Contract

**User Story:**
As a developer, I want an application-owned `CameraFrame` representation, so that A2 Object Detection can process camera input without knowledge of Meta DAT.

#### Acceptance Criteria

1. THE CameraFrame SHALL contain image data owned by the App.

2. THE CameraFrame SHALL NOT retain a reference to SDK-owned `VideoFrame`, SDK-managed buffers, or other Meta DAT objects.

3. THE CameraFrame SHALL include the frame width in pixels.

4. THE CameraFrame SHALL include the frame height in pixels.

5. THE CameraFrame SHALL include a timestamp derived from the DAT frame presentation timestamp.

6. THE CameraFrame SHALL contain any SDK-independent format or encoding metadata required for downstream code to correctly interpret the image data.

7. A1 SHALL NOT require a specific final representation such as JPEG, PNG, Bitmap, ImageProxy, HEVC, or YUV unless that representation is already required by an existing source-of-truth document.

8. THE CameraFrame type SHALL exist outside the `data/meta/` boundary in an SDK-independent shared contract location accessible to downstream A2 components.

9. THE CameraFrame SHALL NOT reference Meta DAT classes, annotations, constants, or enums.

---

### Requirement 6: Frame Ownership, Threading, and Backpressure

**User Story:**
As a developer, I want camera frame handling to preserve valid frame ownership while avoiding UI blocking and unbounded frame accumulation.

#### Acceptance Criteria

1. THE DAT frame callback SHALL perform only the minimum work required to safely transfer SDK-owned frame data into App-owned memory.

2. SDK-owned frame data SHALL NOT be used outside its valid lifetime.

3. AFTER frame ownership has been transferred to the App, format conversion and downstream processing SHALL execute outside the DAT callback.

4. Frame format conversion and downstream processing SHALL NOT execute on the Android main UI thread.

5. THE camera frame delivery pipeline SHALL enforce a bounded processing strategy.

6. THE pipeline SHALL NOT allow unprocessed camera frames to accumulate without limit.

7. WHEN downstream processing is slower than incoming camera frames, THE pipeline SHALL drop or replace stale frames rather than growing an unbounded queue.

8. The exact queue capacity, buffering primitive, coroutine primitive, or frame-dropping algorithm SHALL be decided during the design stage rather than fixed by this requirements document.

---

### Requirement 7: Camera State Exposure

**User Story:**
As a developer, I want the camera pipeline to expose an SDK-independent state, so that application components can respond to connection and streaming conditions without depending on Meta DAT.

#### Acceptance Criteria

1. THE Meta_Camera_Adapter SHALL expose an observable SDK-independent Camera_State.

2. THE Camera_State SHALL represent at minimum the following logical states:

* NOT_CONNECTED
* CONNECTING
* READY
* STREAMING
* RECOVERABLE_ERROR
* BLOCKING_ERROR

3. Exact type names MAY follow existing project naming conventions.

4. WHEN the camera pipeline changes between meaningful lifecycle states, THE exposed Camera_State SHALL be updated accordingly.

5. IF a recoverable error occurs, THEN the camera lifecycle SHALL allow an appropriate retry or restart operation where supported by DAT.

6. Camera errors SHALL NOT crash the App.

7. THE Camera_State type SHALL NOT reference Meta DAT SDK types.

---

### Requirement 8: Shopping Session Integration

**User Story:**
As a developer, I want the camera lifecycle connected to the existing shopping session flow, so that the Meta camera operates only during active shopping observation.

#### Acceptance Criteria

1. WHEN the user starts shopping through the existing application flow, THE App SHALL initiate the DAT camera startup sequence.

2. The startup sequence SHALL eventually perform:

```text
Shopping Start
→ DeviceSession Start
→ Camera Capability Acquisition
→ Camera Stream Start
```

3. WHEN the user ends shopping, THE App SHALL initiate the camera shutdown sequence.

4. The shutdown sequence SHALL eventually perform:

```text
Shopping End
→ Camera Stream Stop
→ Camera Capability Release
→ DeviceSession Stop
```

5. A1 integration SHALL NOT unnecessarily redesign the existing A0 navigation architecture.

6. The existing six-screen navigation flow SHALL remain functional:

```text
Home
→ Shopping
→ Review
→ Travel
→ Checklist
→ Recommendation
```

7. Existing fake repository functionality and fake Product Card rendering SHALL remain usable during A1 development.

8. The App SHALL remain operable when no Meta wearable device is connected.

---

### Requirement 9: A2 Camera Input Boundary

**User Story:**
As a developer, I want a clean `CameraFrame` delivery boundary, so that A2 Object Detection can consume Meta camera frames without depending on the Meta SDK.

#### Acceptance Criteria

1. THE camera input layer SHALL expose app-owned `CameraFrame` instances to downstream A2 components through an SDK-independent boundary.

2. Downstream A2 components SHALL be able to consume CameraFrame without importing Meta DAT artifacts.

3. THE boundary SHALL NOT expose `VideoFrame`, DeviceSession, camera capability objects, DAT buffers, or other SDK-specific types.

4. A1 SHALL stop at the CameraFrame delivery boundary and SHALL NOT implement Object Detection.

---

### Requirement 10: Existing Architecture and Functionality Preservation

**User Story:**
As a developer, I want A1 to integrate Meta camera input without destabilizing the Android architecture completed in A0.

#### Acceptance Criteria

1. THE existing architecture SHALL remain based on:

```text
Presentation
→ Domain
← Data
```

2. A1 SHALL NOT introduce an unnecessary new application architecture.

3. A1 SHALL NOT introduce Hilt or another dependency injection framework solely for Meta DAT integration.

4. Existing ViewModel public contracts SHALL be preserved where possible.

5. IF camera integration requires Presentation-visible camera state or actions, THEN those changes SHALL be additive and limited to the shopping-related flow.

6. Existing fake repositories SHALL remain available.

7. Existing fake Product Card functionality SHALL continue to work.

8. Existing screen navigation SHALL continue to work.

9. THE final A1 code SHALL pass:

```text
./gradlew clean assembleDebug
```

10. Architecture validation SHALL confirm that Meta DAT SDK imports do not leak outside the intended `data/meta/` boundary.

---

# A1 Completion Criteria

A1 application integration SHALL be considered functionally complete when the following sequence is observable:

```text
DAT camera source
→ current Android App
→ DeviceSession starts
→ Camera Stream starts
→ VideoFrame is received
→ SDK-owned frame data is transferred to App-owned memory
→ CameraFrame is produced
→ CameraFrame reaches the A2 input boundary
→ Camera Stream stops
→ DeviceSession stops
→ camera pipeline starts again
→ CameraFrame reception resumes
```

A1 completion SHALL additionally require:

* DAT SDK types remain isolated inside `data/meta/`
* Domain, ViewModel, Presentation, and A2-facing contracts remain SDK-independent
* frame processing does not block the Android main thread
* frame backlog cannot grow without bound
* existing A0 fake application flow continues to work
* existing fake Product Card continues to render
* `./gradlew clean assembleDebug` succeeds

---

# Real Device Acceptance Criterion

The final hardware acceptance path is:

```text
Meta Ray-Ban Gen 2
→ DAT
→ current Android App
→ CameraFrame observed
```

If an actual Gen2 device is unavailable during A1 implementation, successful Mock Device integration SHALL NOT be treated as proof that this real-device acceptance criterion has passed.

The application integration may be considered implementation-complete while the real-device acceptance criterion remains explicitly pending.

---

# Explicitly Out of Scope

A1 SHALL NOT implement:

* Object Detection
* Object Tracking
* Bounding Box processing
* Center ROI calculation
* Occupancy calculation
* Dwell calculation
* Attention Trigger
* Candidate selection
* Recognition crop generation
* Backend `/recognize`
* OpenAI recognition
* Product ID resolution
* Real Product Card integration
* Tracking ID policies
* Detection threshold calibration UI
* Eye Tracking
* Hand Tracking
* Background Service
* Wake Lock
* 15-minute continuous streaming as an A1 blocking requirement
* backend streaming of the complete camera feed
* WebSocket or SSE camera transport
* later Review / Travel / Checklist / Recommendation implementation changes
* unrelated architecture redesign

A1 exists only to establish the reliable input boundary required for the next stage:

```text
CameraFrame
→ Object Detection
→ Center / Occupancy / Dwell
→ Trigger
```
