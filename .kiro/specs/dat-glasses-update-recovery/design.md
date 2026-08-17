# DAT Glasses Update Recovery Bugfix Design

## Overview

When Meta Ray-Ban Gen 2 glasses have an outdated DAT app, `DeviceSession.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` and the session transitions to `STOPPED`. The current `MetaCameraSource` only observes `session.state`, not `session.errors`, so the specific update-required cause is lost and the user sees a generic "device may have disconnected" error with no recovery path.

This fix adds concurrent observation of `DeviceSession.errors` during pipeline startup to detect the DAT update error, maps it to a new `CameraState.DatUpdateRequired` state, and exposes a user-initiated action to open the official SDK update flow via `Wearables.openDATGlassesAppUpdate(activity)`.

## Glossary

- **Bug_Condition (C)**: The condition where `DeviceSession.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` during camera pipeline startup, but `MetaCameraSource` does not observe it
- **Property (P)**: When the DAT update error is emitted, the system shall detect it, set `CameraState.DatUpdateRequired`, and provide a user-actionable update path
- **Preservation**: All existing camera pipeline behavior for non-DAT-update errors, successful startups, permission flows, and cleanup must remain unchanged
- **MetaCameraSource**: The adapter in `data/meta/` that owns the Meta DAT camera lifecycle and converts SDK frames to app-owned `CameraFrame`
- **CameraState**: SDK-independent sealed class in `domain/model/` representing camera pipeline state, observed by ViewModels
- **CameraFrameProvider**: Domain-level interface that `MetaCameraSource` implements; exposes `cameraState`, `frames`, `startCamera()`, `stopCamera()`
- **WearablePermissionRequester**: Existing pattern where Activity injects a callback into `MetaCameraSource` for permission requests

## Bug Details

### Bug Condition

The bug manifests when the glasses DAT app is outdated and the SDK emits `DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` via `DeviceSession.errors`. Since `MetaCameraSource.awaitSessionStarted()` only observes `session.state` (waiting for `STARTED` or `STOPPED`), the specific error cause is never captured. When `STOPPED` arrives, it throws a generic RuntimeException that becomes `CameraState.RecoverableError` — losing the actionable information.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type DeviceSessionLifecycleEvent
  OUTPUT: boolean

  RETURN input.sessionErrors CONTAINS DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED
         AND input.sessionState TRANSITIONS_TO STOPPED
         AND system.errorObserver IS_NOT observing session.errors
         AND system.cameraState IS_SET_TO RecoverableError (generic)
END FUNCTION
```

### Examples

- **Example 1**: User taps "쇼핑 시작" → session created → `session.start()` → `session.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` → `session.state` becomes `STOPPED` → current code throws generic RuntimeException → user sees "Session reached STOPPED during startup — device may have disconnected" → no update button shown
- **Example 2**: User retries → same failure repeats indefinitely with same unhelpful message → user cannot discover that a glasses app update is needed
- **Example 3**: User has updated glasses → session starts normally → `STARTED` reached → no error emitted → pipeline proceeds correctly (not a bug case)
- **Edge case**: `session.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` racing with `session.state` reaching `STOPPED` — both arrive nearly simultaneously, the error observer must win the race to set the correct state

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- When session reaches `STOPPED` due to actual device disconnection (no `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` in errors), the system continues to set `CameraState.RecoverableError` with the generic message
- When session starts successfully and reaches `STARTED`, the pipeline continues through permission check → addCamera → stream → collect frames without modification
- When camera encounters non-DAT-update errors (permission denied, stream start failure), existing `RecoverableError`/`BlockingError` handling remains unchanged
- Normal shopping flow (session creation → camera start → navigation to Shopping screen) is unaffected
- `stopCamera()` cleanup order (camera close → session stop → `NotConnected`) is preserved
- All Meta SDK types remain confined within `data/meta/` — no Meta SDK types leak to Domain or ViewModel

**Scope:**
All inputs that do NOT involve the `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` error emission should be completely unaffected by this fix. This includes:
- Successful session startups (STARTED reached without errors)
- Session timeouts (no error emitted, just no state transition)
- Device disconnection (STOPPED without the specific update error)
- Permission denial errors
- Camera/stream initialization errors
- Normal frame collection and stopCamera flows

## Hypothesized Root Cause

Based on the bug description and code inspection, the root cause is clear:

1. **Missing error stream observation**: `MetaCameraSource.awaitSessionStarted()` uses `session.state.first { ... }` to wait for `STARTED` or `STOPPED`, but never observes `session.errors`. The DAT update error is emitted on the `errors` SharedFlow, which is completely unsubscribed.

2. **Generic exception on STOPPED**: When `STOPPED` is detected, the code throws `RuntimeException("Session reached STOPPED during startup — device may have disconnected")` regardless of cause. The specific error that caused `STOPPED` is never captured.

3. **No CameraState for update requirement**: `CameraState` sealed class has no variant that represents "glasses app update required" — only generic `RecoverableError` and `BlockingError` exist.

4. **No Activity reference for update action**: Even if the error were detected, there's no mechanism to call `Wearables.openDATGlassesAppUpdate(activity)` from `MetaCameraSource`. The existing `permissionRequester` pattern provides a model, but no equivalent exists for update invocation.

## Correctness Properties

Property 1: Bug Condition - DAT Update Error Detection and State Mapping

_For any_ camera pipeline startup where `DeviceSession.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED`, the fixed `MetaCameraSource` SHALL detect the specific error via concurrent observation of the errors SharedFlow and set `CameraState.DatUpdateRequired` (not generic `RecoverableError`), providing the user with an actionable path to invoke the glasses app update.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

Property 2: Preservation - Non-DAT-Update Error Behavior

_For any_ camera pipeline startup where `DeviceSession.errors` does NOT emit `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` (including successful starts, timeouts, disconnections, and other errors), the fixed `MetaCameraSource` SHALL produce exactly the same `CameraState` transitions and behavior as the original code, preserving all existing error handling, cleanup, and navigation flows.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File 1**: `domain/model/CameraState.kt`

**Change**: Add `DatUpdateRequired` variant to the sealed class

**Specific Changes**:
1. Add `data class DatUpdateRequired(val message: String) : CameraState()` — a distinct state that UI can match on to show update-specific UI with a button

---

**File 2**: `domain/camera/CameraFrameProvider.kt`

**Change**: Add an optional method for triggering the DAT glasses update

**Specific Changes**:
1. Add `suspend fun openGlassesUpdate(): GlassesUpdateResult` to the interface (with a default implementation that returns `Unsupported` for non-Meta providers)
2. Define a simple sealed result type: `sealed class GlassesUpdateResult { object Success; data class Failed(val reason: String); object Unsupported }`

---

**File 3**: `data/meta/MetaCameraSource.kt`

**Function**: `awaitSessionStarted()` and new `openGlassesUpdate()`

**Specific Changes**:
1. **Add error observer in `awaitSessionStarted()`**: Launch a side coroutine that collects `session.errors`. If `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` is detected, set an atomic flag (e.g., `datUpdateRequired = true`).
2. **Modify STOPPED handling**: After `STOPPED` is detected, check the flag. If `datUpdateRequired == true`, throw a specific exception (e.g., `DatUpdateRequiredException`) instead of the generic RuntimeException.
3. **Catch specific exception in `runCameraPipeline()`**: In the catch block, check for `DatUpdateRequiredException` and set `_cameraState.value = CameraState.DatUpdateRequired(...)` instead of `RecoverableError`.
4. **Cancel error observer on success**: If `STARTED` is reached, cancel the error observer job — it's no longer needed.
5. **Add Activity reference pattern**: Add `var updateRequester: WearableUpdateRequester?` (similar to existing `permissionRequester`) to hold an Activity reference.
6. **Implement `openGlassesUpdate()`**: Call `Wearables.openDATGlassesAppUpdate(activity)` using the injected requester. Map `DatResult<Unit, NavigationError>` to `GlassesUpdateResult`.

---

**File 4**: `data/meta/WearableUpdateRequester.kt` (new file)

**Change**: Define a functional interface for Activity-bound update invocation

**Specific Changes**:
1. Create `fun interface WearableUpdateRequester { fun openDatGlassesUpdate(): GlassesUpdateResult }` — implemented by the Activity to call `Wearables.openDATGlassesAppUpdate(this)`

---

**File 5**: `feature/home/HomeViewModel.kt` (or a shared CameraViewModel)

**Change**: Observe `CameraState.DatUpdateRequired` and expose update action

**Specific Changes**:
1. Add observation of `cameraFrameProvider.cameraState` to detect `DatUpdateRequired`
2. Expose a `fun requestGlassesUpdate()` that calls `cameraFrameProvider.openGlassesUpdate()`
3. Map the result to UI state (success → prompt retry, failure → show navigation error message)

---

**File 6**: `feature/home/HomeScreen.kt`

**Change**: Show DAT update UI when `CameraState.DatUpdateRequired` is observed

**Specific Changes**:
1. Observe `cameraState` from ViewModel
2. When `DatUpdateRequired`: show explanatory text + "안경 앱 업데이트" button + "재시도" button
3. When update fails with `META_AI_NOT_INSTALLED` or `NOT_REGISTERED`: show specific error message

---

**File 7**: `app/SmartShoppingActivity.kt` (or wherever Activity sets up MetaCameraSource)

**Change**: Inject the `WearableUpdateRequester` implementation

**Specific Changes**:
1. After injecting `permissionRequester`, also inject `updateRequester` that calls `Wearables.openDATGlassesAppUpdate(this)` and maps the result

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code (error goes undetected), then verify the fix correctly detects the error and preserves existing behavior for non-update scenarios.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm that `MetaCameraSource` currently ignores `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` and produces a generic error.

**Test Plan**: Create a mock `DeviceSession` that emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` on its `errors` SharedFlow and transitions `state` to `STOPPED`. Run `startCamera()` and observe the resulting `CameraState`.

**Test Cases**:
1. **DAT Update Error Test**: Mock session emits update-required error → verify current code sets `RecoverableError` (not `DatUpdateRequired`) — demonstrates the bug
2. **Error Message Test**: Verify the error message is generic "device may have disconnected" rather than mentioning the update requirement
3. **No Recovery UI Test**: Verify no `DatUpdateRequired` state is ever emitted by current code

**Expected Counterexamples**:
- `CameraState` becomes `RecoverableError("Session reached STOPPED during startup — device may have disconnected")` instead of `DatUpdateRequired`
- The specific `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` cause is completely lost in the output state

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := MetaCameraSource_fixed.startCamera()
  observedState := cameraState.value
  ASSERT observedState IS CameraState.DatUpdateRequired
  ASSERT observedState.message CONTAINS "update" OR "업데이트"

  updateResult := MetaCameraSource_fixed.openGlassesUpdate()
  ASSERT updateResult IS GlassesUpdateResult.Success OR GlassesUpdateResult.Failed(specificReason)
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT MetaCameraSource_original.startCamera().cameraState = MetaCameraSource_fixed.startCamera().cameraState
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many combinations of session state transitions (STARTED, STOPPED-without-update-error, timeout) automatically
- It catches edge cases where the error observer might interfere with normal flow
- It provides strong guarantees that the new error observation job does not affect non-buggy paths

**Test Plan**: Observe behavior on UNFIXED code for successful starts, timeouts, and disconnections, then write property-based tests capturing that exact behavior continues after the fix.

**Test Cases**:
1. **Successful Start Preservation**: Mock session reaches STARTED → verify pipeline proceeds to permission → addCamera → stream as before
2. **Timeout Preservation**: Mock session never transitions → verify timeout exception and `RecoverableError` as before
3. **Disconnection Preservation**: Mock session reaches STOPPED without emitting DAT update error → verify generic `RecoverableError` as before
4. **StopCamera Preservation**: After any state, calling `stopCamera()` → verify cleanup order and `NotConnected` state
5. **Permission Flow Preservation**: After STARTED, permission denied → verify `BlockingError` behavior unchanged

### Unit Tests

- Test `awaitSessionStarted()` with mock session emitting `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` → expect specific exception
- Test `runCameraPipeline()` catching the specific exception → expect `CameraState.DatUpdateRequired`
- Test `openGlassesUpdate()` with mock Activity → expect correct `GlassesUpdateResult`
- Test `openGlassesUpdate()` when `updateRequester` is null → expect `Unsupported`
- Test `openGlassesUpdate()` when SDK returns `NavigationError.META_AI_NOT_INSTALLED` → expect `Failed("META_AI_NOT_INSTALLED")`
- Test error observer cancellation when `STARTED` is reached (no leak)

### Property-Based Tests

- Generate random sequences of `DeviceSessionState` transitions and verify `DatUpdateRequired` is only set when the specific error is emitted
- Generate random combinations of errors from `DeviceSessionError` enum and verify only `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` triggers the new path
- Generate random timing between error emission and state transition to verify race condition handling

### Integration Tests

- Full pipeline test: inject mock DAT session with update error → verify `DatUpdateRequired` state → call `openGlassesUpdate()` → simulate success → call `startCamera()` again → verify recovery
- UI integration: verify HomeScreen shows update button when ViewModel reports `DatUpdateRequired`
- Lifecycle test: verify `stopCamera()` during `DatUpdateRequired` state cleanly resets to `NotConnected`
