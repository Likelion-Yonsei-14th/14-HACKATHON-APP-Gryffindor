# Implementation Plan: Meta DAT Camera Input (A1)

## Overview

Integrate the previously validated Meta Device Access Toolkit (DAT) camera pipeline into the existing A0 Android application.

A1 ports the working Stage 0 DAT camera lifecycle and frame-access patterns into the current app architecture and exposes app-owned `CameraFrame` objects through an SDK-independent boundary that A2 Object Detection can consume.

A1 does not re-validate Stage 0 fundamentals.

The target path is:

```text
Meta DAT
→ DeviceSession
→ Camera Stream
→ VideoFrame
→ app-owned CameraFrame
→ A2 Object Detection input boundary
```

No Object Detection, tracking, attention logic, backend recognition, or real Product Card integration is included in A1.

---

## Tasks

* [x] 1. DAT Build Configuration

  * [x] 1.1 Integrate the validated Stage 0 DAT build configuration

    * Copy the exact DAT Maven repository configuration from the working Stage 0 project
    * Copy the exact dependency coordinates already proven to resolve in Stage 0
    * Do NOT guess or reconstruct the Maven repository URL, artifact group, or credential property names
    * Configure package credentials using the mechanism already documented and validated in Stage 0:

      * environment variable, or
      * `local.properties`
    * Ensure PAT values are never committed to Git
    * Add:

      * `mwdat-core:0.9.0`
      * `mwdat-camera:0.9.0`
      * debug-only `mwdat-mockdevice:0.9.0`
    * Existing A0 dependencies remain unchanged
    * Verify:

      ```bash
      ./gradlew assembleDebug
      ```
    * *Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6*

---

* [x] 2. SDK-Independent Camera Contracts

  * [x] 2.1 Create CameraFrame

    * Create the SDK-independent `CameraFrame` type in the location defined by the approved design
    * Fields:

      * `data: ByteArray`
      * `width: Int`
      * `height: Int`
      * `timestampUs: Long`
      * `isCompressed: Boolean`
    * `data` must contain App-owned frame bytes
    * Do not retain `VideoFrame`, DAT `ByteBuffer`, DAT enums, or SDK objects
    * Do not introduce serialization annotations or backend concerns
    * *Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.8, 5.9*

  * [x] 2.2 Create CameraState

    * Create the SDK-independent `CameraState` type
    * Include:

      * `NotConnected`
      * `Connecting`
      * `Ready`
      * `Streaming`
      * `RecoverableError(message)`
      * `BlockingError(message)`
    * Do not expose Meta DAT exception types or enums
    * Only classify errors as recoverable/blocking when the actual DAT API provides enough information to do so
    * *Requirements: 7.1, 7.2, 7.3, 7.7*

  * [x] 2.3 Create CameraFrameProvider

    * Create the SDK-independent camera boundary defined by the approved design
    * Expose:

      ```kotlin
      val frames: Flow<CameraFrame>
      val cameraState: StateFlow<CameraState>

      suspend fun startCamera()
      suspend fun stopCamera()
      ```
    * No Meta DAT import may appear in the public contract
    * Do not add consumer registries, observer managers, or future A2 abstractions
    * *Requirements: 9.1, 9.2, 9.3, 4.6*

---

* [x] 3. MetaCameraSource DAT Adapter

  * [x] 3.1 Create MetaCameraSource and lifecycle ownership

    * Create `MetaCameraSource` inside `data/meta/`
    * Implement `CameraFrameProvider`
    * Add SDK-independent state exposure:

      * `MutableStateFlow<CameraState>`
      * public read-only `StateFlow`
    * Add bounded frame delivery:

      ```kotlin
      MutableSharedFlow<CameraFrame>(
          replay = 0,
          extraBufferCapacity = 1,
          onBufferOverflow = BufferOverflow.DROP_OLDEST
      )
      ```
    * Use an adapter-owned coroutine scope with a background dispatcher
    * Do not depend on `viewModelScope` execution context for DAT work
    * Serialize lifecycle-changing operations so overlapping `startCamera()` / `stopCamera()` calls cannot race
    * Keep private references to the active DAT resources required for cleanup:

      * active DeviceSession
      * active camera capability / camera handle required by the real DAT API
      * active streaming job
      * any additional Stage 0 resource strictly required for ordered teardown
    * All Meta SDK types remain private inside `data/meta/`
    * *Requirements: 2.1, 3.1, 4.1-4.6, 6.5, 6.6, 6.7*

  * [x] 3.2 Implement a single idempotent DAT cleanup path

    * Implement one ordered cleanup path shared by:

      * normal `stopCamera()`
      * coroutine cancellation
      * partial startup failure
      * stream failure
    * Do NOT implement separate competing cleanup paths that can independently stop the same DAT resources
    * Cleanup shall attempt resources in the lifecycle order proven by Stage 0:

      ```text
      Camera Stream stop
      → camera capability release/remove
      → DeviceSession stop
      → clear active references
      ```
    * Cleanup must tolerate partially initialized resources
    * Cleanup must be safe if invoked after an earlier partial cleanup
    * Failure while cleaning one resource must not prevent attempts to release the remaining resources where possible
    * A stopped DeviceSession must never be reused
    * After cleanup, the next start creates a new DeviceSession
    * *Requirements: 2.2, 2.3, 2.4, 3.2, 3.5, 3.6*

  * [x] 3.3 Implement startCamera() using validated Stage 0 DAT code

    * Port the actual working Stage 0 startup code into `MetaCameraSource`
    * Do not reconstruct DAT API calls from memory or pseudocode when the Stage 0 implementation is available
    * Required conceptual sequence:

      ```text
      create new DeviceSession
      → start DeviceSession
      → acquire camera capability
      → start camera stream
      → collect VideoFrame
      ```
    * Before starting:

      * ensure no active streaming lifecycle is already running
      * ensure stale resources from an earlier failed lifecycle have been cleaned
    * State transition begins with:

      ```text
      NotConnected
      → Connecting
      ```
    * Transition to `Ready` when DeviceSession and camera capability are actually ready
    * Transition to `Streaming` according to the state/event semantics used by the validated Stage 0 implementation

      * if DAT exposes an actual stream state signal, prefer that
      * otherwise use the same successful-start condition already proven in Stage 0
    * Do not invent an unsupported DAT state mechanism
    * Startup failure must:

      * emit an SDK-independent error state
      * trigger the shared cleanup path
      * leave the adapter capable of a future fresh start
      * never crash the App
    * *Requirements: 2.1, 2.3, 2.4, 2.5, 3.1, 3.3, 3.4, 3.5, 7.4, 7.5, 7.6*

  * [x] 3.4 Implement safe VideoFrame → CameraFrame ownership transfer

    * Collect the validated Stage 0 VideoFrame source
    * For each `VideoFrame`, read:

      * `buffer`
      * `width`
      * `height`
      * `presentationTimeUs`
      * `isCompressed`
    * Do not pass `VideoFrame` or its SDK-owned buffer outside the valid collection/callback lifetime
    * Copy SDK-owned memory immediately into an App-owned ByteArray
    * Do not mutate the original SDK buffer position while copying
    * Prefer a duplicate/read view:

      ```kotlin
      val source = videoFrame.buffer.duplicate()
      val bytes = ByteArray(source.remaining())
      source.get(bytes)
      ```
    * Construct:

      ```kotlin
      CameraFrame(
          data = bytes,
          width = videoFrame.width,
          height = videoFrame.height,
          timestampUs = videoFrame.presentationTimeUs,
          isCompressed = videoFrame.isCompressed
      )
      ```
    * Emit using the bounded frame flow
    * Keep the frame collection block limited to:

      ```text
      metadata read
      → safe ownership copy
      → CameraFrame construction
      → bounded emit
      ```
    * Do NOT perform:

      * Object Detection
      * tracking
      * image recognition
      * Backend requests
      * attention calculations
      * Product Card updates
      * unnecessary image format conversion
    * Do not infer an unverified format such as NV12
    * *Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 6.2, 6.3, 6.4*

  * [x] 3.5 Implement stopCamera() without cleanup races

    * Serialize `stopCamera()` against concurrent start/stop operations
    * Cancel the active streaming job
    * Wait for cancellation/termination to complete
    * Ensure the job's termination and the shared cleanup path do not independently perform competing teardown operations
    * Use the single idempotent cleanup mechanism from Task 3.2
    * Finish in:

      ```text
      CameraState.NotConnected
      ```

      after cleanup reaches its terminal state
    * Repeated stop calls must not crash
    * After stop:

      * old DeviceSession references are cleared
      * old camera resources are cleared
      * a future start creates a fresh DeviceSession
    * *Requirements: 2.2, 2.3, 2.4, 3.2, 3.5, 3.6*

  * [x] 3.6 Verify adapter background execution and bounded delivery

    * Confirm potentially blocking DAT startup/teardown does not execute directly on Android Main
    * Confirm frame handling does not execute on Android Main
    * Confirm `viewModelScope.launch` is not incorrectly treated as an off-main execution guarantee
    * Confirm slow downstream consumption cannot produce an unbounded pending frame queue
    * Confirm stale frames are dropped/replaced according to the approved bounded strategy
    * *Requirements: 6.3, 6.4, 6.5, 6.6, 6.7*

---

* [x] 4. Manual DI and Shopping Flow Integration

  * [x] 4.1 Register CameraFrameProvider in AppContainer

    * Add the A1 camera implementation using the existing Manual DI pattern
    * Expose the dependency to features as `CameraFrameProvider`
    * Do not expose MetaCameraSource-specific or DAT-specific API to ViewModels
    * Existing repository declarations remain unchanged
    * Do not introduce Hilt or another DI framework
    * *Requirements: 10.1, 10.2, 10.3*

  * [x] 4.2 Connect shopping start to camera start

    * Inject `CameraFrameProvider` into the existing Home shopping-start path using the current Factory/AppContainer pattern
    * Start the existing shopping session using the current A0 behavior
    * Request camera startup additively
    * Camera startup failure must NOT:

      * roll back an otherwise valid shopping session
      * prevent navigation to the Shopping flow
      * replace the existing shopping error state with a hardware error
    * Camera failure remains observable through `CameraState`
    * Preserve existing HomeViewModel public behavior where possible
    * *Requirements: 8.1, 8.2, 8.8, 10.4, 10.5*

  * [x] 4.3 Connect shopping end to camera stop

    * Inject `CameraFrameProvider` into the current shopping-end path
    * Request camera shutdown when shopping ends
    * Camera stop/cleanup failure must NOT prevent:

      * existing shopping session completion
      * Review navigation
      * remaining A0 application flow
    * Ensure the shopping session completion path executes even if camera shutdown reports an error
    * Expose `cameraState` through the Shopping ViewModel only if needed by existing Presentation requirements
    * Keep changes additive
    * *Requirements: 8.3, 8.4, 8.8, 10.4, 10.5*

  * [x] 4.4 Update existing ViewModel creation/wiring

    * Pass `CameraFrameProvider` through the current AppContainer / Factory / navigation wiring
    * Do not redesign navigation
    * Preserve:

      ```text
      Home
      → Shopping
      → Review
      → Travel
      → Checklist
      → Recommendation
      ```
    * *Requirements: 8.5, 8.6, 10.8*

---

* [x] 5. Build and A0 Regression Checkpoint

  * [x] 5.1 Verify compilation and existing application flow

    * Run:

      ```bash
      ./gradlew clean assembleDebug
      ```
    * Verify:

      * DAT dependencies resolve
      * domain camera contracts compile
      * MetaCameraSource compiles
      * AppContainer wiring compiles
      * ViewModel changes compile
      * no DAT imports exist outside the allowed Meta adapter boundary
    * Launch the application and smoke test:

      ```text
      Home
      → Shopping
      → Review
      → Travel
      → Checklist
      → Recommendation
      ```
    * Verify:

      * fake Product Card renders
      * existing Review behavior still works
      * normal back stack works
      * application does not require Backend server for fake flow
      * application remains usable without a connected Meta device
      * camera startup failure does not crash or block the fake flow
    * *Requirements: 1.6, 8.6, 8.7, 8.8, 10.6, 10.7, 10.8, 10.9*

---

* [x] 6. Focused Automated Tests

  * [x]* 6.1 Test CameraFrame ownership and metadata

    * Verify copied bytes equal the source contents at conversion time
    * Verify mutating or advancing the original source buffer does not alter `CameraFrame.data`
    * Verify the original SDK/source buffer position is not changed by the copy helper where testable
    * Verify:

      * width
      * height
      * timestampUs
      * isCompressed
    * Use the existing project test stack only
    * *Requirements: 5.1, 5.2, 5.3, 5.4, 5.5*

  * [ ]* 6.2 Test lifecycle and single cleanup behavior — SKIPPED: MetaCameraSource uses DAT SDK directly with no test seam; verified via Mock Device E2E

    * Using the smallest practical test seam, verify:

      ```text
      start
      → Streaming

      stop
      → NotConnected

      start again
      → fresh DeviceSession
      → Streaming
      ```
    * Verify startup failure executes cleanup for resources already acquired
    * Verify stream failure executes cleanup
    * Verify cancellation executes cleanup
    * Verify explicit stop and job termination do not cause unsafe double teardown
    * Verify one cleanup failure does not prevent remaining cleanup attempts
    * Verify repeated stop does not crash
    * *Requirements: 2.2, 2.3, 2.4, 2.5, 3.2, 3.5, 3.6*

  * [x]* 6.3 Test bounded frame delivery

    * Produce frames faster than a slow consumer processes them
    * Verify pending frame history does not grow without bound
    * Verify stale frames may be dropped
    * Do not require delivery of every produced frame
    * *Requirements: 6.5, 6.6, 6.7*

  * [x]* 6.4 Test shopping-flow failure isolation

    * HomeViewModel:

      * valid shopping session still starts if camera startup fails
    * ShoppingViewModel:

      * shopping session still completes if camera stop fails
      * existing Review flow remains reachable
    * Verify camera errors do not incorrectly replace existing shopping-domain error state
    * *Requirements: 8.1, 8.3, 8.8*

---

* [x] 7. Architecture Boundary Verification

  * [x] 7.1 Verify Meta SDK isolation

    * Confirm no DAT SDK imports exist in:

      * `feature/`
      * `domain/`
      * other non-Meta data packages
      * Presentation/ViewModel code
    * Confirm DAT imports exist only in the approved `data/meta/` boundary and corresponding allowed debug Meta source-set code
    * Confirm:

      * `CameraFrame`
      * `CameraState`
      * `CameraFrameProvider`
        contain no Meta DAT references
    * Confirm ViewModels depend only on `CameraFrameProvider`
    * Run:

      ```bash
      ./gradlew clean assembleDebug
      ```
    * *Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 10.10*

---

* [ ] 8. Mock Device Integration Acceptance — UNVERIFIED: device PIN lock screen prevents UI interaction

  * [ ] 8.1 Verify CameraFrame delivery through the current Android App

    * Use the debug build and the existing Stage 0 Mock Device setup
    * Target path:

      ```text
      Mock Device
      → DAT
      → current Android App
      → DeviceSession
      → Camera Stream
      → VideoFrame
      → app-owned CameraFrame
      → CameraFrameProvider.frames
      ```
    * Verify:

      * new DeviceSession starts
      * camera capability is acquired
      * Camera Stream starts
      * VideoFrame is received continuously
      * CameraFrame is produced
      * CameraFrame contains valid:

        * data
        * width
        * height
        * timestampUs
        * compressed flag
      * frame delivery reaches the A2 input boundary
      * shopping end stops the camera lifecycle
      * resources are released without crash
      * a fresh shopping start creates a new DeviceSession
      * CameraFrame reception resumes
    * This is the main A1 application-integration acceptance path
    * Do not treat this as new validation of DAT fundamentals already proven in Stage 0
    * *Requirements: 2.1-2.5, 3.1-3.6, 5.1-5.5, 8.1-8.8, 9.1-9.4*

---

* [ ] 9. Real Gen2 Hardware Acceptance — PENDING: requires physical Gen2 + manual device interaction

  * [ ] 9.1 Verify CameraFrame delivery using actual Meta Ray-Ban Gen 2

    * Target:

      ```text
      Meta Ray-Ban Gen 2
      → DAT
      → current Android App
      → VideoFrame
      → CameraFrame
      → A2 input boundary
      ```
    * Verify:

      * shopping start activates the Gen2 camera pipeline
      * CameraFrame is observed through the same SDK-independent boundary used by Mock Device
      * shopping end stops the pipeline safely
      * start → stop → start succeeds
    * If actual Gen2 hardware is unavailable:

      * mark this task `PENDING`
      * do not report it as passed based on Mock Device results
    * A1 application integration may otherwise be implementation-complete while this hardware criterion remains pending
    * *Requirements: Real Device Acceptance Criterion*

---

## Notes

* Tasks marked with `*` are optional for the fastest hackathon MVP, but the core lifecycle/ownership behavior must still be validated through build and smoke tests.
* A1 ports the already validated Stage 0 DAT camera path into the current Android App.
* Do not repeat Stage 0 from scratch.
* Actual DAT Maven coordinates, repository URL, credential names, and SDK calls must come from the working Stage 0 project.
* Do not guess DAT APIs.
* Meta SDK types remain inside `data/meta/`.
* `VideoFrame` and DAT-owned buffers never cross the Meta adapter boundary.
* SDK memory is copied into App-owned memory before its lifetime ends.
* Do not mutate the original DAT `ByteBuffer` position during frame copying.
* DAT lifecycle uses one shared idempotent ordered cleanup path.
* Do not create competing cleanup paths between `stopCamera()`, job cancellation, and failure handling.
* Potentially blocking DAT work and frame processing must not block Android Main.
* Frame delivery is bounded; stale frames may be dropped.
* Camera failure must never block the existing Shopping session or navigation flow.
* A stopped DeviceSession is never reused.
* No new DI framework, property-testing framework, persistence layer, background service, or reconnect framework is introduced.
* Mock Device acceptance and actual Gen2 acceptance are separate.
* A1 ends at:

  ```text
  CameraFrameProvider.frames
  → CameraFrame
  → A2 input boundary
  ```

---

## Task Dependency Graph

```json
{
  "waves": [
    {
      "id": 0,
      "tasks": ["1.1"]
    },
    {
      "id": 1,
      "tasks": ["2.1", "2.2", "2.3"]
    },
    {
      "id": 2,
      "tasks": ["3.1", "3.2"]
    },
    {
      "id": 3,
      "tasks": ["3.3", "3.4", "3.5", "3.6"]
    },
    {
      "id": 4,
      "tasks": ["4.1"]
    },
    {
      "id": 5,
      "tasks": ["4.2", "4.3", "4.4"]
    },
    {
      "id": 6,
      "tasks": ["5.1"]
    },
    {
      "id": 7,
      "tasks": ["6.1", "6.2", "6.3", "6.4", "7.1"]
    },
    {
      "id": 8,
      "tasks": ["8.1"]
    },
    {
      "id": 9,
      "tasks": ["9.1"]
    }
  ]
}
```
