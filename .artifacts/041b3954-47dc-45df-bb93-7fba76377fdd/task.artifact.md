# Tasks — Prescriptions UI Fixes

- [x] UI Enum Mappers & String Resources
    - [x] Update `strings.xml` (EN & UK) with missing enum labels
    - [x] Create `PrescriptionUiMappers.kt`
- [x] Prescriptions List (Refresh Errors)
    - [x] Update `PrescriptionsViewModel.kt`
    - [x] Update `PrescriptionsScreen.kt`
- [x] Prescription Detail (Cleanup)
    - [x] Update `PrescriptionDetailViewModel.kt` (remove `isDeleted`)
    - [x] Update `PrescriptionDetailScreen.kt` (use localized enums)
- [x] Medication Item Form (Validation & Pickers)
    - [x] Update `MedicationItemFormViewModel.kt` (validation, dose filter)
    - [x] Update `MedicationItemFormScreen.kt` (DatePicker + TimePicker for `courseStart`, localized labels)
- [x] Verification
    - [x] Build project (`./gradlew app:assembleDebug`)
    - [x] Commit and push to `dev`
