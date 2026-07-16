# Tasks - Intake-reports data layer + reminder_config Room cache

- `[x]` Part A: Intake Reports
    - `[x]` Create `IntakeReportDtos.kt`
    - `[x]` Create `IntakeReportApi.kt`
    - `[x]` Create `IntakeReportEntity.kt`
    - `[x]` Create `IntakeReportDao.kt`
    - `[x]` Implement `IntakeReportRepository.kt` (Interface, Real, Mock)
- `[x]` Part B: Reminder Config Cache
    - `[x]` Create `ReminderConfigEntity.kt`
    - `[x]` Create `ReminderConfigDao.kt`
    - `[x]` Update `ReminderConfigRepository.kt` (Interface, Real, Mock)
- `[x]` Room & DI Integration
    - `[x]` Update `BpDatabase.kt` (Entities, DAOs, version bump)
    - `[x]` Update `ServiceLocator.kt` (Register new API, repositories, and DAOs)
- `[x]` Verification
    - `[x]` Add `IntakeReportRepositoryTest.kt`
    - `[x]` Run `./gradlew app:assembleDebug`
