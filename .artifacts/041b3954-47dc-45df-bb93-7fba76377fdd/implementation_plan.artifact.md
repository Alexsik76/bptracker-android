# Implementation Plan — Prompt 03: Prescriptions UI

Build the user-facing screens for managing prescriptions and medication items using a stateless UI pattern and flat ViewModel state, integrated into the existing hand-rolled navigation system.

## User Review Required

> [!IMPORTANT]
> - Navigation is integrated into `MainAuthenticatedLayout` using string-key overlays.
> - New strings are added in both English and Ukrainian.
> - A new entry point is added to the `ScheduleScreen` app bar.
> - All forms use derived validation state and local Error/Saving/Saved flags.

## Proposed Changes

### [MODIFY] Resources
#### [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml) & [strings.xml (uk)](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)
Add all strings for prescriptions list, detail, and forms.

---

### [NEW] Prescriptions Feature
Package: `ua.vn.home.bptracker.feature.prescriptions`

#### [NEW] [PrescriptionsViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionsViewModel.kt) & [PrescriptionsScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionsScreen.kt)
- Observe `Flow<List<PrescriptionReadDto>>`.
- Handle `refresh()`.

#### [NEW] [PrescriptionDetailViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionDetailViewModel.kt) & [PrescriptionDetailScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionDetailScreen.kt)
- Hoisted `selectedPrescriptionId`.
- Observe items for the prescription.
- Handle deletion.

#### [NEW] [PrescriptionFormViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormViewModel.kt) & [PrescriptionFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormScreen.kt)
- Handle create/edit modes.
- Material3 DatePicker integration.

#### [NEW] [MedicationItemFormViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormViewModel.kt) & [MedicationItemFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormScreen.kt)
- Hoisted `selectedItemId`.
- Progressive disclosure for course settings.
- Enum selection (chips/dropdowns).

---

### [MODIFY] Navigation & Integration

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
- Add hoisted `selectedPrescriptionId` and `selectedItemId`.
- Add overlays: `prescriptions`, `prescription_detail`, `prescription_form`, `med_item_form`.

#### [MODIFY] [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
- Add an `IconButton` to the `TopAppBar` to open the `prescriptions` overlay.

## Verification Plan

### Automated Tests
- Build project: `./gradlew app:assembleDebug`.

### Manual Verification
- Verify screen transitions and back button handling.
- Check form validation (Save button enabled/disabled).
- Confirm deletion dialogs and cascading UI updates.
- Verify Ukrainian translations.
