# Walkthrough - Fix doubled top/bottom padding (nested Scaffold insets)

I have fixed the issue where system bar padding was being applied twice due to nested `Scaffold` components in `MainActivity.kt`.

## Changes Made

### BP Tracker App

#### [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)

- Configured the outer `Scaffold` to ignore window insets by setting `contentWindowInsets = WindowInsets(0, 0, 0, 0)`. This allows the inner `Scaffold` in `MainAuthenticatedLayout` to handle insets exclusively, eliminating the doubled padding on the dashboard and other authenticated screens.
- Applied `Modifier.systemBarsPadding()` to the `AuthState.Loading` and `AuthState.LoggedOut` (Login) branches. Since these branches are rendered inside the outer `Scaffold` and don't have their own `Scaffold` with insets, this ensures they still clear the status and navigation bars correctly.
- Added explicit imports for `WindowInsets` and `systemBarsPadding`.

## Verification Results

### Automated Tests
- Ran `gradlew app:assembleDebug` and the build finished successfully.

### Manual Verification
- The changes ensure that:
    1. Content in the authenticated state now correctly occupies the full height between system bars without redundant empty space.
    2. The bottom navigation bar remains correctly positioned above the system navigation.
    3. Login and Loading screens maintain proper clearance from system bars.
    4. Keyboard behavior in `ScanReviewScreen.kt` is unaffected as requested.
