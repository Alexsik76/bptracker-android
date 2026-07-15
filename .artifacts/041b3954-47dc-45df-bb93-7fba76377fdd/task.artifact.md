# Tasks — Park Reminders Runtime

- [x] Update String Resources (EN & UK)
- [x] Refactor `ScheduleViewModel` (remove network calls)
- [x] Update `ScheduleScreen` (show "coming soon" message)
- [x] Neutralize `ReminderScheduler` (no-op `rescheduleAll`)
- [x] Neutralize `ReminderReceiver` (no-op `onReceive`)
- [x] Update `SettingsViewModel` (disable reminders toggle logic)
- [x] Verification
    - [x] Build project (`./gradlew app:assembleDebug`)
    - [x] Manual check of Schedule tab
    - [x] Commit and push to `dev`
