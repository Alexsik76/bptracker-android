# Tasks — Migrate Navigation to Navigation-Compose

- [x] Add Dependency
    - [x] Update `libs.versions.toml`
    - [x] Update `app/build.gradle.kts`
- [x] Refactor `MainAuthenticatedLayout` in `MainActivity.kt`
    - [x] Initialize `NavController`
    - [x] Create `NavHost` and define routes
    - [x] Move state-hoisted payloads (Bitmap, selectedMeasurement) into host scope
    - [x] Implement BottomBar visibility logic
    - [x] Wire VM initializers in `LaunchedEffect` for each destination
- [x] Update Screen Callbacks
    - [x] `ScheduleScreen.kt` (prescriptions entry point)
    - [x] Update other screens to use `navController` actions instead of `activeOverlay` setters
- [x] Verification
    - [x] Build project (`./gradlew app:assembleDebug`)
    - [x] Manual re-walk of all flows
    - [x] Commit and push to `dev`
