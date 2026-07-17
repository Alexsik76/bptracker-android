# Implementation Plan — Refresh Dashboard on Resume

The dashboard (Home screen) shows stale data because its `HomeViewModel` is retained in the back stack and does not automatically refresh when the user returns to it (e.g., after adding a new measurement in another screen). This plan adds a lifecycle-aware trigger to refresh the home data whenever the screen becomes visible (resumes).

## User Review Required

> [!IMPORTANT]
> This change will cause the `HomeViewModel.refresh()` function to be called every time the home screen is resumed. According to the current implementation of `HomeViewModel`, this will not show a loading spinner if content is already present, ensuring a smooth user experience.

## Proposed Changes

### Android App

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)

- Add imports for `androidx.lifecycle.Lifecycle` and `androidx.lifecycle.compose.LifecycleEventEffect`.
- In the `composable("home")` block, use `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` to call `homeVm.refresh()`.

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to ensure the project still builds correctly.

### Manual Verification
1. Launch the app and stay on the Dashboard.
2. Navigate to another screen (e.g., History or Manual Entry).
3. Add a new measurement.
4. Return to the Dashboard (e.g., via back button or bottom bar).
5. Verify that the Dashboard now shows the newly added measurement in the hero tile and recent readings list.
