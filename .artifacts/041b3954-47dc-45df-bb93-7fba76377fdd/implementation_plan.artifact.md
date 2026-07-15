# Implementation Plan — Prompt 03b: Prescriptions UI Fixes

This plan addresses several UI issues and cleanups in the prescriptions feature, including better input validation, localized enum labels, and improved date-time selection for medication courses.

## User Review Required

> [!IMPORTANT]
> - `courseStart` selection will now involve both a DatePicker and a TimePicker to ensure valid ISO format.
> - Enum labels in UI will be fully localized.
> - Validation for limited-course medications is now stricter (requires intakes > 0).

## Proposed Changes

### [MODIFY] Resources
#### [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml) & [strings.xml (uk)](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)
Add localized strings for:
- `DoseUnit` (tablet, mg, ml, drop, mcg, IU)
- `FreqPeriodUnit` (hour, day, week)
- `WhenSlot` (Morning, Day, Evening)
- `CourseType` (Ongoing, Course)

---

### [NEW] [PrescriptionUiMappers.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionUiMappers.kt)
Helper functions to map `WhenSlot`, `DoseUnit`, `FreqPeriodUnit`, and `CourseType` to their localized string resources.

---

### [MODIFY] Prescriptions List

#### [MODIFY] [PrescriptionsViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionsViewModel.kt)
- Add `_error` `MutableStateFlow`.
- Update `refresh()` to catch exceptions and populate `_error`.
- Combine `_error` into the public `state`.

#### [MODIFY] [PrescriptionsScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionsScreen.kt)
- Display the error message if `state.error` is present (using an inline message or `ErrorState`).

---

### [MODIFY] Prescription Detail

#### [MODIFY] [PrescriptionDetailViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionDetailViewModel.kt)
- Remove dead `isDeleted` field from `PrescriptionDetailState`.

#### [MODIFY] [PrescriptionDetailScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionDetailScreen.kt)
- Use localized labels for `WhenSlot`, `DoseUnit`, and `FreqPeriodUnit` in medication rows.

---

### [MODIFY] Medication Item Form

#### [MODIFY] [MedicationItemFormViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormViewModel.kt)
- Fix `onDoseAmountChange` to allow only one decimal point.
- Update `isValid` to require `courseIntakes > 0` and `courseStart.isNotBlank()` when `courseType == Course`.

#### [MODIFY] [MedicationItemFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormScreen.kt)
- Replace `courseStart` text field with a button that triggers a combined Date and Time picker.
- Use localized labels for all enum-based selections (Chips, Dropdowns).

## Verification Plan

### Automated Tests
- Build project: `./gradlew app:assembleDebug`.

### Manual Verification
- Test `doseAmount` input with multiple dots (e.g. `1.2.3` should be prevented).
- Verify "Save" button state for Course medications (should remain disabled if intakes or start date are missing).
- Verify Date+Time picker for `courseStart` produces a valid ISO string.
- Verify all enum labels are localized in both English and Ukrainian.
- Trigger a network error during `refresh()` and verify the error message appears on the list screen.
