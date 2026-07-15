# Implementation Plan — Migrate Navigation to Navigation-Compose

This plan migrates the hand-rolled `activeOverlay` navigation in `MainAuthenticatedLayout` to a formal `NavHost` structure while preserving all existing behaviors and ViewModel lifecycles.

## User Review Required

> [!IMPORTANT]
> - `androidx.navigation:navigation-compose` will be added as a dependency.
> - Authentication gate and magic-link intent handling remain in `MainActivity` as-is.
> - `MainAuthenticatedLayout` will now host the `NavHost` and manage bottom bar visibility based on the current route.
> - Non-serializable data (Bitmaps, `MeasurementDto`) will continue to be shared via host-scoped state to avoid complex navigation workarounds.

## Proposed Changes

### [MODIFY] Build Configuration

#### [libs.versions.toml](file:///D:/dev/bp_tracker/mobile_app/gradle/libs.versions.toml)
- Add `navigation = "2.8.5"` to `[versions]`.
- Add `androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }` to `[libraries]`.

#### [app/build.gradle.kts](file:///D:/dev/bp_tracker/mobile_app/app/build.gradle.kts)
- Add `implementation(libs.androidx.navigation.compose)` to `dependencies`.

---

### [MODIFY] Navigation Port

#### [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
- Replace `activeOverlay` state with `rememberNavController()`.
- Implement `NavHost` inside `MainAuthenticatedLayout` with routes:
    - `home`, `schedule`, `camera`, `ocr_review`, `manual_entry`, `measurement_detail`, `settings`, `bp_scale`, `history`, `schedule_edit`, `prescriptions`, `prescription_detail/{prescriptionId}`, `prescription_form?prescriptionId={prescriptionId}`, `med_item_form/{prescriptionId}?itemId={itemId}`.
- Logic for `selectedMeasurement`, `capturedBitmap`, etc., stays as host-scoped `remember { mutableStateOf(...) }`.
- `BpBottomNavBar` visibility gated by `navController.currentBackStackEntryAsState()`.
- Call VM `setX` / `init` methods within `LaunchedEffect` in each `composable` block.

#### [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
- Ensure the `prescriptions` entry point uses `navController.navigate("prescriptions")`.

---

### [MODIFY] Screen Composables
Update callback signatures and internal `BackHandler` usage (where applicable) to use `navController` actions.

- **History**: `onMeasurementClick` -> `navController.navigate("measurement_detail")`.
- **Camera**: `onCapture` -> `navController.navigate("ocr_review")`.
- **Prescriptions**: Pass `prescriptionId` and `itemId` via route arguments.

## Verification Plan

### Automated Tests
- Run `./gradlew app:assembleDebug` to verify compilation and dependency wiring.

### Manual Verification (Re-walk)
- **Auth**: Login flow, magic links, passkey enrollment.
- **Main Tabs**: Home vs. Schedule, state preservation.
- **Measurements**: Manual entry, Camera/OCR flow, Detail/Delete, History.
- **Settings**: Theme/Language, BP Scale help.
- **Prescriptions**: List, Detail, Item Form (create/edit), Prescription Form (create/edit), cascade deletes.
- **Back Stack**: Verify system back behavior matches the old state machine.
