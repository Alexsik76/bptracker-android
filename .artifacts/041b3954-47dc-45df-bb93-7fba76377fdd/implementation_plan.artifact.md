# Fix Broken Tests

The project currently fails to compile unit tests because `ScheduleEditState` and `ScheduleEditViewModel` were removed in a previous refactoring (replaced by `ReminderConfigViewModel`), but the corresponding test files were left behind.

## Proposed Changes

### Tests cleanup

#### [DELETE] [ScheduleEditViewModelTest.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/test/java/ua/vn/home/bptracker/feature/home/ScheduleEditViewModelTest.kt)
- This test refers to a deleted class and is no longer relevant.

#### [NEW] [ReminderConfigViewModelTest.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/test/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigViewModelTest.kt)
- Create a new test to verify `ReminderConfigState` validation and time parsing logic, replacing the functionality that was in `ScheduleEditViewModelTest`.

#### [MODIFY] [ReminderSchedulerTest.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/test/java/ua/vn/home/bptracker/feature/reminders/ReminderSchedulerTest.kt)
- Remove the dependency on `ScheduleEditState`.
- Update the time validation test to use `java.time.LocalTime` or a simple regex, as used in the production `ReminderScheduler`.

### Prescription Form Tests

#### [NEW] [PrescriptionFormViewModelTest.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/test/java/ua/vn/home/bptracker/feature/prescriptions/PrescriptionFormViewModelTest.kt)
- Add a test for `PrescriptionFormViewModel` to verify the fix where `savedId` is captured after creation. This ensures the new navigation logic in `MainActivity` works as intended.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:testDebugUnitTest` and ensure all tests pass.
- Run `./gradlew :app:assembleDebug` to ensure no production code was affected.
