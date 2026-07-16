# Prescription Form UX Fixes

This plan addresses two UX issues in the prescription form:
1.  Misleading date field trailing icon and limited click area.
2.  Navigation to the list instead of the detail screen after creating a prescription.

## Proposed Changes

### Resources

#### [MODIFY] [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml)
- Add `prescriptions_pick_date_desc` string for the calendar icon content description.

#### [MODIFY] [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)
- Add `prescriptions_pick_date_desc` string in Ukrainian.

### Feature: Prescriptions

#### [MODIFY] [PrescriptionFormViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormViewModel.kt)
- Update `PrescriptionFormState` to include `savedId: String? = null`.
- Modify `save()` method:
    - On creation, capture the ID from the returned `PrescriptionReadDto` and set it in `savedId`.
    - On edit, ensure `savedId` remains `null`.

#### [MODIFY] [PrescriptionFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormScreen.kt)
- Remove the `LaunchedEffect` that automatically calls `onBack()` when `isSaved` is true. Navigation will now be handled by the host.
- Update the `OutlinedTextField` for `prescribedOn`:
    - Add an `interactionSource` and a `Modifier.clickable` (or similar) to make the entire field open the date picker.
    - Replace the `TextButton` in `trailingIcon` with an `IconButton` using `Icons.Default.DateRange`.
    - Use the new `prescriptions_pick_date_desc` for the icon's content description.

### Main Activity

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
- Update the `composable` for `prescription_form`:
    - Add a `LaunchedEffect(formState.isSaved)` block.
    - If `isSaved` is true:
        - If `savedId` is present (create flow), navigate to `prescription_detail/{savedId}` and pop the form from the backstack.
        - If `savedId` is null (edit flow), call `navController.popBackStack()`.

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to ensure the project builds successfully.

### Manual Verification
- **Date Field:**
    - Open the prescription form.
    - Verify the trailing icon is now a calendar icon.
    - Tap on the field itself — the date picker should open.
    - Tap on the calendar icon — the date picker should open.
- **Navigation:**
    - Create a new prescription. Verify it navigates to the detail screen of the new prescription.
    - Press "Back" from the detail screen. Verify it returns to the prescriptions list.
    - Edit an existing prescription and save. Verify it returns to the detail screen (where it came from).
