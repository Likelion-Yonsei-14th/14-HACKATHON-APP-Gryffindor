# Implementation Plan

- [ ] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - DAT Update Error Goes Undetected
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Scope the property to the concrete failing case: `DeviceSession.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` while `session.state` transitions to `STOPPED`
  - Create a test that mocks `DeviceSession` with:
    - `errors: SharedFlow` that emits `DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED`
    - `state: StateFlow` that transitions `IDLE → STARTING → STOPPED`
  - Call `startCamera()` on `MetaCameraSource` and observe resulting `cameraState`
  - Assert: `cameraState.value` is `CameraState.DatUpdateRequired` (expected behavior after fix)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS because current code sets `CameraState.RecoverableError("Session reached STOPPED during startup — device may have disconnected")` instead of `CameraState.DatUpdateRequired`
  - Document counterexample: the DAT update error is completely ignored, `session.errors` is never observed, and the user receives a generic unhelpful message
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2_

- [ ] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-DAT-Update Camera Pipeline Behavior
  - **IMPORTANT**: Follow observation-first methodology
  - Observe on UNFIXED code:
    - Successful start: mock session reaches `STARTED` → `cameraState` proceeds through `Connecting → Ready → Streaming`
    - Timeout: mock session never transitions past `STARTING` → `cameraState` becomes `RecoverableError` with timeout message
    - Disconnection (no DAT update error): mock session reaches `STOPPED` without emitting `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` → `cameraState` becomes `RecoverableError` with generic disconnection message
    - StopCamera: calling `stopCamera()` → `cameraState` returns to `NotConnected`
  - Write property-based tests:
    - For all mock sessions where `errors` does NOT emit `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED`: resulting `cameraState` must match the observed behavior (never `DatUpdateRequired`)
    - For all successful starts (session reaches `STARTED`): pipeline proceeds through permission → addCamera → stream → frames without interference from the error observer
    - For `stopCamera()` calls in any state: cleanup completes and state becomes `NotConnected`
  - Verify all tests PASS on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 3. Fix for DAT glasses update error detection and recovery

  - [ ] 3.1 Add `CameraState.DatUpdateRequired` variant
    - Add `data class DatUpdateRequired(val message: String) : CameraState()` to the sealed class in `domain/model/CameraState.kt`
    - This state represents a blocking condition that requires user action (glasses app update) before camera can function
    - _Requirements: 2.2_

  - [ ] 3.2 Add `GlassesUpdateResult` sealed class and `openGlassesUpdate()` to `CameraFrameProvider`
    - Define `sealed class GlassesUpdateResult` in `domain/camera/` with variants: `Success`, `Failed(reason: String)`, `Unsupported`
    - Add `suspend fun openGlassesUpdate(): GlassesUpdateResult` to `CameraFrameProvider` with default implementation returning `Unsupported`
    - No Meta SDK types in this interface — stays SDK-independent
    - _Requirements: 2.4, 2.6_

  - [ ] 3.3 Create `WearableUpdateRequester` interface in `data/meta/`
    - Create `data/meta/WearableUpdateRequester.kt`
    - Define `fun interface WearableUpdateRequester { fun openDatGlassesUpdate(): GlassesUpdateResult }`
    - Mirrors the existing `WearablePermissionRequester` pattern for Activity-bound SDK calls
    - _Requirements: 2.4_

  - [ ] 3.4 Implement DAT update error detection in `MetaCameraSource.awaitSessionStarted()`
    - Launch a side coroutine that collects `session.errors` SharedFlow
    - If `DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` is detected, set an atomic flag `datUpdateErrorDetected = true`
    - After `STOPPED` is detected in `session.state.first { ... }`, check the flag
    - If flag is true: throw a specific `DatUpdateRequiredException` (private exception class) instead of the generic RuntimeException
    - If `STARTED` is reached: cancel the error observer job (no leak)
    - _Bug_Condition: isBugCondition(input) where session.errors contains DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED AND session.state transitions to STOPPED_
    - _Expected_Behavior: Set CameraState.DatUpdateRequired instead of RecoverableError_
    - _Preservation: When errors does NOT emit DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED, behavior is identical to original_
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1_

  - [ ] 3.5 Handle `DatUpdateRequiredException` in `runCameraPipeline()` catch block
    - Add a catch clause for `DatUpdateRequiredException` before the generic `Exception` catch
    - Set `_cameraState.value = CameraState.DatUpdateRequired(message)` with a user-friendly Korean message indicating glasses app update is needed
    - Do NOT set `RecoverableError` for this case
    - _Requirements: 2.2, 2.3_

  - [ ] 3.6 Implement `openGlassesUpdate()` in `MetaCameraSource`
    - Add `var updateRequester: WearableUpdateRequester? = null` property (similar to `permissionRequester`)
    - Override `openGlassesUpdate()`: call `updateRequester?.openDatGlassesUpdate()` or return `Unsupported` if null
    - The actual SDK call `Wearables.openDATGlassesAppUpdate(activity)` is in the Activity-injected requester, not here
    - _Requirements: 2.4, 2.6_

  - [ ] 3.7 Inject `WearableUpdateRequester` in `MainActivity`
    - After injecting `permissionRequester`, also inject `updateRequester` on `appContainer.metaCameraSource`
    - Implementation: call `Wearables.openDATGlassesAppUpdate(this)` and map `DatResult<Unit, NavigationError>` to `GlassesUpdateResult`
    - Map `NavigationError.META_AI_NOT_INSTALLED` → `Failed("META_AI_NOT_INSTALLED")`
    - Map `NavigationError.NOT_REGISTERED` → `Failed("NOT_REGISTERED")`
    - Map success → `Success`
    - _Requirements: 2.4, 2.6_

  - [ ] 3.8 Expose DAT update state and action in `HomeViewModel`
    - Add `cameraState: StateFlow` observation from `cameraFrameProvider.cameraState`
    - Add `datUpdateRequired: StateFlow<Boolean>` (or expose cameraState directly to UI)
    - Add `fun requestGlassesUpdate()` that calls `cameraFrameProvider.openGlassesUpdate()` and maps result to UI state
    - Add `fun retryCamera()` that calls `cameraFrameProvider.startCamera()` after update
    - On `GlassesUpdateResult.Failed(reason)`: expose specific error message to UI
    - No Meta SDK types in ViewModel — only `CameraState` and `GlassesUpdateResult`
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 3.6_

  - [ ] 3.9 Show DAT update UI in `HomeScreen`
    - Observe `cameraState` from ViewModel
    - When `CameraState.DatUpdateRequired`: show explanatory text ("안경 앱 업데이트가 필요합니다") + "안경 앱 업데이트" button + "재시도" button
    - "안경 앱 업데이트" button → calls `viewModel.requestGlassesUpdate()`
    - "재시도" button → calls `viewModel.retryCamera()`
    - When update fails: show specific navigation error message (e.g., "Meta AI 앱이 설치되지 않았습니다")
    - Do NOT auto-launch update flow — only on user tap
    - Minimal Material 3 UI (message + buttons only)
    - _Requirements: 2.3, 2.4, 2.5, 2.6_

  - [ ] 3.10 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - DAT Update Error Correctly Detected
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior: when `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` is emitted, `cameraState` becomes `DatUpdateRequired`
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2_

  - [ ] 3.11 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-DAT-Update Camera Pipeline Behavior
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions introduced)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Run full test suite to verify no regressions
  - Verify bug condition test (Property 1) passes — DAT update error is detected and sets `DatUpdateRequired`
  - Verify preservation tests (Property 2) pass — all non-DAT-update flows behave identically to before
  - Verify no Meta SDK types leaked into domain/ or feature/ layers
  - Ensure all tests pass, ask the user if questions arise.
