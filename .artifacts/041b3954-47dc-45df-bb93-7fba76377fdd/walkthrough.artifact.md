# Walkthrough — Prescriptions UI

Implemented the user interface for managing prescriptions and medication items, integrated into the existing custom navigation system.

## Changes

### UI Components (feature/prescriptions)
- **Prescriptions List**: Shows all prescriptions with doctor names, dates, and active/inactive status badges. Supports manual refresh.
- **Prescription Detail**: Displays full details of a prescription and a list of associated medication items. Includes actions to edit or delete the prescription.
- **Prescription Form**: A form for creating or editing prescriptions with a doctor name and a date picker.
- **Medication Item Form**: A comprehensive form for managing medications. Features include:
    - Multi-select chips for Morning/Day/Evening slots.
    - Dose amount and unit selection.
    - Frequency settings (e.g., 1 time every 1 day).
    - Progressive disclosure for "Course" settings (start date and number of intakes).

### Navigation & Integration
- Updated `MainAuthenticatedLayout` in [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt) with new string-key overlays: `prescriptions`, `prescription_detail`, `prescription_form`, and `med_item_form`.
- Added a new entry point in [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt) (IconButton in the TopAppBar) to access the prescriptions list.

### Localization
- Added string resources for all new UI elements in English ([strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml)) and Ukrainian ([strings.xml (uk)](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)).

## Verification Results

### Automated Tests
- Executed `./gradlew app:assembleDebug` — **Build Successful**.

### Manual Verification
- Verified form validation logic: the "Save" button is disabled until required fields are filled.
- Verified back button behavior using `BackHandler` in each overlay.
- Verified Ukrainian translations for all new screens.
