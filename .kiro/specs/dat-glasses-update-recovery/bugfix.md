# Bugfix Requirements Document

## Introduction

When the Meta Ray-Ban Gen 2 glasses have an outdated DAT app, `DeviceSession` emits `DeviceSessionError.DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` and transitions to `STOPPED`. The current code treats this as a generic device disconnection, showing a vague `RecoverableError` message with no user-actionable recovery. The actual cause (glasses firmware update required) is lost, and the user has no way to invoke the official SDK update flow (`Wearables.openDATGlassesAppUpdate(activity)`).

This fix adds a specific detection path for the DAT update error, maps it to a distinguishable `CameraState`, and exposes a user-initiated action to open the glasses update screen — without auto-launching the update flow or adding infinite retry loops.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN the DAT glasses app is outdated and `DeviceSession.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` THEN the system ignores the error flow and only observes `DeviceSessionState.STOPPED`

1.2 WHEN the session reaches `STOPPED` due to `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` THEN the system throws a generic RuntimeException("Session reached STOPPED during startup — device may have disconnected") that loses the specific cause

1.3 WHEN the generic RuntimeException is caught in `runCameraPipeline()` THEN the system sets `CameraState.RecoverableError` with a message that does not indicate a DAT update is needed

1.4 WHEN the user sees the generic error message THEN the system provides no actionable UI element (button or link) to trigger the official `Wearables.openDATGlassesAppUpdate(activity)` flow

1.5 WHEN the user retries camera start without updating the glasses app THEN the system repeats the same failure cycle indefinitely with the same unhelpful error message

### Expected Behavior (Correct)

2.1 WHEN the DAT glasses app is outdated and `DeviceSession.errors` emits `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` THEN the system SHALL detect this specific error by observing the `DeviceSession.errors` SharedFlow concurrently with session state

2.2 WHEN `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` is detected THEN the system SHALL set a distinct `CameraState` that clearly identifies the cause as a required DAT glasses app update (e.g., `CameraState.DatUpdateRequired` or `CameraState.BlockingError` with a recognizable tag)

2.3 WHEN the DAT-update-required `CameraState` is active THEN the system SHALL display a user-facing message explaining that the glasses app needs to be updated before camera streaming can work

2.4 WHEN the DAT-update-required `CameraState` is active THEN the system SHALL present a button that allows the user to manually invoke `Wearables.openDATGlassesAppUpdate(activity)` — the system SHALL NOT auto-launch the update flow

2.5 WHEN the user completes (or dismisses) the update flow THEN the system SHALL allow the user to retry camera start manually without requiring an app restart

2.6 WHEN `Wearables.openDATGlassesAppUpdate(activity)` returns a `NavigationError` (e.g., `META_AI_NOT_INSTALLED` or `NOT_REGISTERED`) THEN the system SHALL show the specific navigation error to the user rather than silently failing

### Unchanged Behavior (Regression Prevention)

3.1 WHEN the session reaches `STOPPED` due to an actual device disconnection (not DAT update error) THEN the system SHALL CONTINUE TO throw RuntimeException and set `CameraState.RecoverableError` as it does today

3.2 WHEN the session starts successfully and reaches `STARTED` THEN the system SHALL CONTINUE TO proceed through the camera pipeline (permission check → addCamera → stream → collect frames) without modification

3.3 WHEN the camera pipeline encounters errors other than `DAT_APP_ON_THE_GLASSES_UPDATE_REQUIRED` (e.g., permission denied, stream start failure) THEN the system SHALL CONTINUE TO handle them with existing `RecoverableError` / `BlockingError` logic unchanged

3.4 WHEN the user taps "쇼핑 시작" and the session/camera starts normally THEN the system SHALL CONTINUE TO navigate to the Shopping screen with no additional UI interruptions

3.5 WHEN `stopCamera()` is called THEN the system SHALL CONTINUE TO clean up resources in the existing order (camera close → session stop) and return to `CameraState.NotConnected`

3.6 WHEN Meta SDK types are used for error detection and update invocation THEN the system SHALL CONTINUE TO confine all Meta SDK types within `data/meta/` — no Meta SDK types shall leak into Domain or ViewModel layers
