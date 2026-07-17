# Implementation Plan - Wire local reminders and takenAt fixes

This plan covers un-parking the reminders runtime, connecting it to the intake stack, and fixing `takenAt` format issues.

## User Review Required

> [!IMPORTANT]
> The reminders feature will be opt-in. Users must explicitly enable it in Settings, which will trigger a notification permission request on Android 13+.

## Proposed Changes

### Core Data & Utils

#### [MODIFY] [IntakeReportRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/IntakeReportRepository.kt)
- Update `confirm` to use `OffsetDateTime.now().toString()` as the default `moment`.
- Implement `syncPending()` to drain the offline buffer (POST pending upserts, DELETE pending deletes).
- Update `MockIntakeReportRepository` for consistency.

#### [MODIFY] [TimeUtils.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/utils/TimeUtils.kt)
- Ensure `parseToLocal` is robust (already looks good, but will be used in `ScheduleScreen`).

#### [MODIFY] [SettingsStore.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/config/SettingsStore.kt)
- Add `remindersEnabled` preference (default `false`).

---

### Reminders Logic

#### [MODIFY] [TodayScheduleUseCase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/TodayScheduleUseCase.kt)
- Add `getTodayOnce(date: String): TodaySchedule` for one-shot background reads.

#### [MODIFY] [ReminderScheduler.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderScheduler.kt)
- Implement `rescheduleAll()` to schedule alarms for Morning, Day, and Evening based on `getCachedConfig()`.

#### [MODIFY] [ReminderReceiver.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderReceiver.kt)
- Implement `onReceive` to:
    - Use `goAsync()` for background work.
    - Check if reminders are enabled in `SettingsStore`.
    - Fetch today's schedule via `getTodayOnce`.
    - If the slot is valid, has meds, and is not taken: show notification.
    - Reschedule for the next day.

#### [MODIFY] [ReminderActionReceiver.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderActionReceiver.kt)
- Implement "taken" action to record intake via `IntakeReportRepository.confirm`.

---

### UI & UX

#### [MODIFY] [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
- Update `getLocalizedTime` to use `TimeUtils.parseToLocal(timeStr)` for consistent parsing of `takenAt`.

#### [MODIFY] [ScheduleViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleViewModel.kt)
- Call `intakeRepo.syncPending()` inside `refresh()`.

#### [MODIFY] [SettingsViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/settings/SettingsViewModel.kt)
- Connect `remindersActive` state and `setRemindersEnabled` action to `SettingsStore`.
- Update `refresh()` to fetch the current state.

#### [MODIFY] [SettingsScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/settings/SettingsScreen.kt)
- Implement `onRemindersToggle` to:
    - Request `POST_NOTIFICATIONS` permission if enabling (API 33+).
    - Call `NotificationHelper.createNotificationChannel()` and `ReminderScheduler.rescheduleAll()` if enabled.
    - Call `ReminderScheduler.cancelAllReminders()` if disabled.

#### [MODIFY] [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml) & [strings.xml (uk)](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)
- Add strings for notification title, "Taken" action, and permission rationale.

---

### Glue

#### [MODIFY] [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt)
- Ensure `ReminderScheduler` and `NotificationHelper` are available.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:testDebugUnitTest` to ensure no regressions in projection rules.
- Verify `TimeUtils` parsing via unit tests if possible.

### Manual Verification
1. **Enable Reminders**: Toggle in Settings, grant permission.
2. **Verify Alarms**: Check logs to see if alarms are scheduled.
3. **Offline Notification**: Set system time to a slot time while offline. Verify notification appears with correct meds.
4. **Taken Action**: Tap "Taken" on notification. Verify slot is marked taken in Schedule (even offline).
5. **Sync**: Re-enable network, tap "Refresh" in Schedule. Verify intake is synced to server.
6. **Disable Reminders**: Toggle off in Settings. Verify alarms are cancelled.
7. **Boot**: Reboot device (or simulate `BOOT_COMPLETED` via ADB). Verify alarms are rescheduled if enabled.
