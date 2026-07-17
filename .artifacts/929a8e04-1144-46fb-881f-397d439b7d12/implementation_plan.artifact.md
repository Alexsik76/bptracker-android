# Implementation Plan - Today Schedule Projection

Build the client-side "today" projection: what to take today, per slot, with taken/not state. This is a domain use-case with unit tests.

## User Review Required

> [!IMPORTANT]
> The projection logic relies on a pure function `buildTodaySchedule` which will be unit-tested. This function handles merging items from multiple prescriptions and filtering course items based on `courseStart`.

## Proposed Changes

### feature/reminders

#### [NEW] [TodaySchedule.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/TodaySchedule.kt)
Define the domain models as specified:
- `TodaySchedule`
- `TodaySlot`
- `TodayMed`

#### [NEW] [TodayScheduleUseCase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/TodayScheduleUseCase.kt)
Implement the use case with:
- `observeToday(date: String): Flow<TodaySchedule>`
- `buildTodaySchedule(...)` pure function (internal).

### core/di

#### [MODIFY] [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt)
Register `TodayScheduleUseCase` in `ServiceLocator`.

### tests

#### [NEW] [TodayScheduleUseCaseTest.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/test/java/ua/vn/home/bptracker/feature/reminders/TodayScheduleUseCaseTest.kt)
Unit tests for `buildTodaySchedule` covering:
1. Null config handling.
2. Single item slot placement.
3. Merging items from multiple prescriptions.
4. Slot ordering and omission of empty slots.
5. Intake state (taken, pending delete).
6. Course item filtering by date.
7. Dose fields mapping.

## Verification Plan

### Automated Tests
- Run unit tests: `./gradlew :app:testDebugUnitTest --tests "ua.vn.home.bptracker.feature.reminders.TodayScheduleUseCaseTest"`
- Full build: `./gradlew app:assembleDebug`

### Manual Verification
- N/A (Logic only task as per prompt).
