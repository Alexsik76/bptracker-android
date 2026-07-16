# Implementation Plan — Fix keyboard (IME) covering input fields

This plan addresses the issue where the soft keyboard covers input fields and action buttons in various forms. It also includes a default value for the dose unit in the medication-item form.

## Proposed Changes

### [Component] Core configuration

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
- Confirm `enableEdgeToEdge()` is used (already present).
- The current implementation uses `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` and `Modifier.systemBarsPadding()` in some places. I will ensure consistency in how insets are handled.

### [Component] Prescriptions Feature

#### [MODIFY] [MedicationItemFormViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormViewModel.kt)
- Default `doseUnit` to `DoseUnit.Mg` in `init` when creating a new item (`itemId == null`).

#### [MODIFY] [MedicationItemFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormScreen.kt)
- Add `Modifier.imePadding()` to the main container.
- It already has `Modifier.verticalScroll(rememberScrollState())`.

#### [MODIFY] [PrescriptionFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormScreen.kt)
- Add `Modifier.verticalScroll(rememberScrollState())` and `Modifier.imePadding()` to the main container.
- Replace `Spacer(Modifier.weight(1f))` with a fixed spacer or remove it to allow scrolling.

### [Component] Reminders Feature

#### [MODIFY] [ReminderConfigScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigScreen.kt)
- Add `Modifier.imePadding()` to the main container.
- It already has `Modifier.verticalScroll(rememberScrollState())`.

### [Component] Home Feature (Manual Entry & Scan Review)

#### [MODIFY] [ManualEntryScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ManualEntryScreen.kt)
- Add `Modifier.verticalScroll(rememberScrollState())` and `Modifier.imePadding()` to the main container.
- Replace `Spacer(Modifier.weight(1f))` with a fixed spacer.

#### [MODIFY] [ScanReviewScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScanReviewScreen.kt)
- Ensure `Modifier.imePadding()` is correctly applied (it's currently on `Scaffold`, which might be correct if it lifts the whole UI including the bottom bar).
- Confirm the `verticalScroll` works as expected with the bottom bar.

### [Component] Auth Feature

#### [MODIFY] [LoginScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/login/LoginScreen.kt)
- Add `Modifier.verticalScroll(rememberScrollState())` and `Modifier.imePadding()` to the main container.

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to ensure the project still builds.

### Manual Verification
- Deploy the app to a device/emulator.
- Open each modified screen and focus on a text field (especially those at the bottom).
- Verify that the keyboard does not cover the focused field or the "Save" button.
- Verify that the user can scroll to see all fields and the "Save" button while the keyboard is open.
- For `MedicationItemFormScreen`, verify that "mg" is selected by default for new items.
