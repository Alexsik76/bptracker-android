# Implementation Plan — Optimize IME Insets and Padding

The user reported that the keyboard padding is too large. This is caused by overlapping inset handling between `Scaffold` and manual `imePadding()` modifiers.

## Proposed Changes

### [Component] Core Layout

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
- Set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` for the `Scaffold` in `MainAuthenticatedLayout`. This prevents the `Scaffold` from automatically adding padding for system bars, allowing us to control it precisely in individual screens.

### [Component] Screens Audit

For each screen where `imePadding()` was added, I will ensure it's not fighting with `Scaffold` padding.

#### [MODIFY] [MedicationItemFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormScreen.kt)
- Set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` in its `Scaffold`.
- Ensure `Modifier.imePadding()` is applied correctly to the main `Column`.

#### [MODIFY] [ScanReviewScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScanReviewScreen.kt)
- Remove `imePadding()` from the main `Column` because it's already handled by the `bottomBar` lift.
- Set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` in its `Scaffold`.

#### [MODIFY] [PrescriptionFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormScreen.kt)
#### [MODIFY] [ManualEntryScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ManualEntryScreen.kt)
#### [MODIFY] [ReminderConfigScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigScreen.kt)
- Set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` in their `Scaffold` components.

## Verification Plan

### Manual Verification
- Verify the gap between the keyboard and the "Course" section in `MedicationItemFormScreen` is reduced.
- Verify `ScanReviewScreen` bottom bar doesn't have double padding.
- Ensure top bar doesn't overlap with status bar (may need to add `statusBarsPadding()` to top bars if `Scaffold` insets are cleared).
