# Walkthrough - "Today" schedule UI + intake card

The Schedule tab has been rewritten to display a real-time, offline-readable medication plan for "today", driven by the `TodayScheduleUseCase`. Users can now confirm intakes, edit the recorded time, and delete records directly from the app.

## Changes Made

### Reminders Logic Fix
- **Course Gate Fix**: In `TodayScheduleUseCase.kt`, corrected the comparison logic to ensure medications starting *today* are included.
- **Updated Tests**: In `TodayScheduleUseCaseTest.kt`, updated the course filtering tests to use full ISO datetime strings and verified the "starts today" case.

### UI & UX Enhancements
- **Schedule Screen**: Completely redesigned [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt) to show medication slots (Morning, Day, Evening) with localized names, configuration-based times, and medication details (name, dose, condition).
- **Intake Card**: Added an interactive [IntakeBottomSheet] to handle actions:
  - **Confirm**: Mark a slot as taken at the current time.
  - **Edit Time**: Change the recorded time using a Material3 `TimePicker`.
  - **Delete**: Remove the intake record.
- **Refresh Mechanism**: Added `ON_RESUME` refresh in [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt), ensuring the schedule stays up-to-date when returning from settings or background.
- **Localization**: Added comprehensive strings and translations in `values/strings.xml` and `values-uk/strings.xml`.

### Data Management
- **ViewModel Rewrite**: Updated [ScheduleViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleViewModel.kt) to observe the reactive projection and expose actions for repository-level updates.

## Verification Results

### Automated Tests
Ran all unit tests via Gradle:
```bash
./gradlew :app:testDebugUnitTest
```
**Result**: 20 tests passed, including the updated `TodayScheduleUseCaseTest`.

### Build Check
Verified that the project compiles successfully:
```bash
./gradlew app:assembleDebug
```
**Result**: Build finished successfully.

### Manual Verification
1.  **Schedule Rendering**: Verified that active prescriptions appear in the correct slots with accurate times from configuration.
2.  **Confirm Intake**: Tapping "Confirm" marks the slot as taken and displays the time.
3.  **Edit/Delete**: Verified that editing time via `TimePicker` updates the local state correctly, and deleting restores the slot to "Pending".
4.  **Empty/Unconfigured States**: Verified that the screen correctly handles cases with no scheduled medications or missing configuration.
