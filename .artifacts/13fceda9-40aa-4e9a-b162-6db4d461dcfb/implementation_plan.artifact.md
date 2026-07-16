# Implementation Plan - Intake-reports data layer + reminder_config Room cache

Builds the client data layer for `intake_reports` (offline-first) and adds a Room cache for `reminder_config`.

## Proposed Changes

### Data Layer: Intake Reports

#### [NEW] [IntakeReportDtos.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/dto/IntakeReportDtos.kt)
- `IntakeReportCreateDto`, `SnapshotEntryDto`, `IntakeReportReadDto`.

#### [NEW] [IntakeReportApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/IntakeReportApi.kt)
- Retrofit interface for `reminders/intake-reports`.

#### [NEW] [IntakeReportEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/IntakeReportEntity.kt)
- Room entity for `intake_reports` table.
- Includes `syncState` enum (SYNCED, PENDING_UPSERT, PENDING_DELETE).
- Composite primary key on `(date, period)`.

#### [NEW] [IntakeReportDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/IntakeReportDao.kt)
- DAO for `IntakeReportEntity`.

#### [NEW] [IntakeReportRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/IntakeReportRepository.kt)
- Interface with `observeForDate`, `refresh`, `confirm`, `delete`.
- `LocalIntake` domain holder.
- `RealIntakeReportRepository` (offline-first with local-pending fallback).
- `MockIntakeReportRepository` (in-memory).

---

### Data Layer: Reminder Config Cache

#### [NEW] [ReminderConfigEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/ReminderConfigEntity.kt)
- Room entity for `reminder_config` (single-row table, `id = 0`).

#### [NEW] [ReminderConfigDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/ReminderConfigDao.kt)
- DAO for `ReminderConfigEntity`.

#### [MODIFY] [ReminderConfigRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/ReminderConfigRepository.kt)
- Add `getCachedConfig()` to interface and implementations.
- Update `RealReminderConfigRepository` to write through to Room.

---

### Room & DI Integration

#### [MODIFY] [BpDatabase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/BpDatabase.kt)
- Add `IntakeReportEntity` and `ReminderConfigEntity`.
- Expose `intakeReportDao()` and `reminderConfigDao()`.
- Bump version to 4.

#### [MODIFY] [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt)
- Register `IntakeReportApi`.
- Register `IntakeReportRepository`.
- Provide `IntakeReportDao` and `ReminderConfigDao` to repositories.

---

## Verification Plan

### Automated Tests
- [NEW] `IntakeReportRepositoryTest.kt`: Unit tests for `confirm` and `delete` logic, mocking API and DAO.
- `./gradlew app:assembleDebug` to ensure compilation.

### Manual Verification
- N/A (No UI in this prompt).
