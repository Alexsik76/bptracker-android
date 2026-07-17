# Implementation Plan - Remove Dead Reminders Cluster

Mechanical removal of the legacy reminders code that is no longer used by the active features.

## User Review Required

> [!IMPORTANT]
> This change involves deleting code and bumping the database version. Since `fallbackToDestructiveMigration` is enabled, the local cache will be cleared upon the next app launch.

## Proposed Changes

### Data Layer Cleanup

#### [DELETE] [ReminderApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/ReminderApi.kt)
#### [DELETE] [ReminderDtos.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/dto/ReminderDtos.kt)
#### [DELETE] [ReminderRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/ReminderRepository.kt)
#### [DELETE] [MedIntakeEntity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/entity/MedIntakeEntity.kt)
#### [DELETE] [MedIntakeDao.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/dao/MedIntakeDao.kt)

### Wiring Updates

#### [MODIFY] [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt)
- Remove `reminderApi` property.
- Remove `reminderRepository` property and its construction.
- Remove unused imports related to these properties.

#### [MODIFY] [BpDatabase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/local/BpDatabase.kt)
- Remove `MedIntakeEntity::class` from the `entities` list.
- Remove the `medIntakeDao()` abstract method.
- Bump the database version from `4` to `5`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the project compiles.
- Run `./gradlew :app:testDebugUnitTest` to ensure all tests pass.

### Manual Verification
- Verify that the app still launches and the current reminders functionality (which uses `IntakeReportRepository`) still works as expected.
