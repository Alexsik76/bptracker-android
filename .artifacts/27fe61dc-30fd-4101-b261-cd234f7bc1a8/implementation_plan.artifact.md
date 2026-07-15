# Implementation Plan - Fix doubled top/bottom padding (nested Scaffold insets)

The goal is to prevent doubled system bar padding in `MainActivity.kt` caused by nested `Scaffold` components. The outer `Scaffold` will be configured to ignore window insets, as the inner `Scaffold` in `MainAuthenticatedLayout` already handles them correctly. Non-authenticated states will be updated to handle their own system bar padding.

## User Review Required

> [!IMPORTANT]
> This change focuses solely on `MainActivity.kt` and ensures that the dashboard and other authenticated screens use the full screen height correctly while maintaining proper clearance for system bars.

## Proposed Changes

### BP Tracker App

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)

- Add imports for `androidx.compose.foundation.layout.WindowInsets` and `androidx.compose.foundation.layout.systemBarsPadding`.
- Update the outer `Scaffold` to use `contentWindowInsets = WindowInsets(0, 0, 0, 0)`.
- Wrap `AuthState.Loading` content in a `Box` with `Modifier.systemBarsPadding()`.
- Wrap `LoginScreen` in a `Box` with `Modifier.systemBarsPadding()` and `Modifier.fillMaxSize()`.

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors or import issues.
- `gradlew assembleDebug`

### Manual Verification
- Deploy to a device/emulator.
- Verify that the Dashboard (LoggedIn state) no longer has large empty bands at the top and bottom.
- Verify that the bottom navigation bar is fully visible and correctly positioned above the system navigation bar.
- Verify that the Login screen and Loading indicator still clear the status bar.
- Verify that the Scan review screen keyboard behavior remains unchanged (as it's not touched).
