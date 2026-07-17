# Implementation Plan - "Today" schedule UI + intake card

Rewrite the Schedule tab to display the real "today" medication schedule and implement intake actions (confirm, edit time, delete). Fix a bug in the course-start projection logic.

## User Review Required

> [!IMPORTANT]
> - The projection logic fix addresses lexicographical comparison of dates vs full datetime strings.
> - `ScheduleViewModel` will now trigger repository refreshes to ensure local data is up-to-date with the server.
> - A `ModalBottomSheet` will be introduced for managing intake actions (Edit/Delete).

## Proposed Changes

### feature/reminders

#### [MODIFY] [TodayScheduleUseCase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/TodayScheduleUseCase.kt)
- Fix `CourseType.Course` gate: `item.courseStart.take(10) <= date`.

#### [MODIFY] [TodayScheduleUseCaseTest.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/test/java/ua/vn/home/bptracker/feature/reminders/TodayScheduleUseCaseTest.kt)
- Update `course items are filtered by start date` test to use full ISO strings for `courseStart`.

### Resources (Localization)

#### [MODIFY] [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml)
- Add strings: `schedule_intake_title`, `schedule_action_confirm`, `schedule_action_edit`, `schedule_action_delete`, `schedule_not_configured`, `schedule_set_hours`.

#### [MODIFY] [strings.xml (uk)](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)
- Add Ukrainian translations for new strings.

### feature/home (UI)

#### [MODIFY] [ScheduleViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleViewModel.kt)
- Update `ScheduleState` to include `NotConfigured`.
- Implement `observeToday(today)` using `TodayScheduleUseCase`.
- Implement `refresh()`, `confirmSlot()`, `editTime()`, `deleteIntake()`.

#### [MODIFY] [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
- Redesign `ScheduleScreen` to handle `NotConfigured`, `Empty`, and `Content` states.
- Implement `SlotCard` to display medications and status.
- Implement `IntakeBottomSheet` with actions:
  - **Confirm**: calls `confirmSlot`.
  - **Edit Time**: opens `TimePicker`, builds ISO string with `today`'s date and system offset, calls `editTime`.
  - **Delete**: calls `deleteIntake`.

### Navigation & Lifecycle

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
- Update `schedule` composable to use new ViewModel surface.
- Add `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` to trigger `refresh()`.

## Verification Plan

### Automated Tests
- Run updated unit tests: `./gradlew :app:testDebugUnitTest --tests "ua.vn.home.bptracker.feature.reminders.TodayScheduleUseCaseTest"`
- Full build: `./gradlew app:assembleDebug`

### Manual Verification
1. Open Schedule tab: verify it shows current medications.
2. Mark a slot as taken: verify state updates to "Taken at HH:MM".
3. Open intake card: Edit time using `TimePicker` and verify update.
4. Delete intake: verify slot returns to "Pending" state.
5. Unconfigure reminders: verify "Not Configured" state shows with a link to settings.
