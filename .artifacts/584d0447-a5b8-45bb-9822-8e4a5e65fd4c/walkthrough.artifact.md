# Walkthrough — Fixed keyboard (IME) covering input fields

I have implemented the requested fixes to ensure that input fields and action buttons are accessible even when the soft keyboard is open. I have also added a default value for the dose unit in the medication-item form.

## Changes

### [Component] Prescriptions Feature

#### [MedicationItemFormViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormViewModel.kt)
- Defaulted `doseUnit` to `DoseUnit.Mg` (milligrams) when creating a new medication item.

#### [MedicationItemFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormScreen.kt)
- Added `Modifier.imePadding()` to the main scrollable column to ensure the content lifts above the keyboard.

#### [PrescriptionFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormScreen.kt)
- Added `Modifier.verticalScroll(rememberScrollState())` and `Modifier.imePadding()` to the main column.
- Replaced the weight-based spacer with a fixed spacer to allow proper scrolling when the keyboard is visible.

### [Component] Reminders Feature

#### [ReminderConfigScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigScreen.kt)
- Added `Modifier.imePadding()` to the main scrollable column.
- Replaced the weight-based spacer with a fixed spacer.

### [Component] Home Feature (Manual Entry & Scan Review)

#### [ManualEntryScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ManualEntryScreen.kt)
- Added `Modifier.verticalScroll(rememberScrollState())` and `Modifier.imePadding()` to the main column.
- Replaced the weight-based spacer with a fixed spacer.

#### [ScanReviewScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScanReviewScreen.kt)
- Added `Modifier.imePadding()` to both the main scrollable content and the pinned bottom bar. This ensures that the bottom bar lifts above the keyboard and the content remains scrollable above it.

### [Component] Auth Feature

#### [LoginScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/login/LoginScreen.kt)
- Added `Modifier.verticalScroll(rememberScrollState())` and `Modifier.imePadding()` to the main container to ensure the login form is usable on smaller screens or when the keyboard is open.

## Verification

### Automated Verification
- Ran `./gradlew app:assembleDebug` successfully.

### Manual Verification (Expected Results)
- **Medication Item Form**: Upon clicking "Add medication", the dose unit is pre-selected as "mg". Focus on any field; the keyboard opens and the "Save" button remains reachable via scrolling.
- **Other Forms**: Focus on any input field; the view scrolls correctly so the focused field is visible above the keyboard. The primary action button (Save/Sign In) can be scrolled into view or is pinned above the keyboard.
