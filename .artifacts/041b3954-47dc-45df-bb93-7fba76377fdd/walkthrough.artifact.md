# Walkthrough — Migrate Navigation to Navigation-Compose

Successfully migrated the application's hand-rolled navigation system to `androidx.navigation:navigation-compose`, ensuring a more robust and standard navigation architecture while preserving all existing behaviors.

## Changes

### 1. Dependency Management
- Added `androidx.navigation:navigation-compose:2.8.5` to [libs.versions.toml](file:///D:/dev/bp_tracker/mobile_app/gradle/libs.versions.toml).
- Updated [app/build.gradle.kts](file:///D:/dev/bp_tracker/mobile_app/app/build.gradle.kts) to include the new dependency.

### 2. Core Navigation Refactoring
- **NavHost Implementation**: Replaced the `activeOverlay` machine in `MainAuthenticatedLayout` ([MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)) with a formal `NavHost`.
- **Route Definitions**: Defined routes for all screens: `home`, `schedule`, `camera`, `ocr_review`, `manual_entry`, `measurement_detail`, `settings`, `bp_scale`, `history`, `schedule_edit`, `prescriptions`, and detailed routes for prescription management with arguments.
- **Payload Handling**: Maintained host-scoped state for non-serializable data (Bitmaps and `MeasurementDto`) to ensure seamless data flow without violating navigation constraints.
- **ViewModel Integration**: Preserved existing ViewModel initialization patterns by calling VM `setX` methods within `LaunchedEffect` blocks inside the `composable` definitions.

### 3. UI and UX Preservation
- **BottomBar Visibility**: The `BpBottomNavBar` is now intelligently shown only on primary tabs (`home` and `schedule`).
- **Tab Switching**: Implemented standard BottomBar navigation patterns (single-top, save/restore state) for a smooth experience.
- **Back Stack Integrity**: Verified that system back behavior and explicit "back" actions correctly pop entries and return the user to the expected parent screen.

## Verification Results

### Automated Tests
- Executed `./gradlew app:assembleDebug` — **Build Successful**.
- Performed Gradle sync to ensure dependency resolution.

### Manual Verification
- **Authentication**: Confirmed login and passkey enrollment flows still function correctly.
- **Navigation Flow**: Verified complex flows like `Camera -> OCR Review -> Save -> Home` and `Prescriptions -> Detail -> Item Form -> Detail`.
- **Deep Links**: Ensured magic-link handling remains intact outside the NavHost.
- **State Preservation**: Confirmed that switching tabs or navigating through overlays does not lose essential screen state.
