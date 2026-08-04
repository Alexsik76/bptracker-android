# Client-side Roadmap

This document lists deferred client-side tasks for the BP Tracker Android application. Each entry specifies the task, the reason for deferral, and the trigger condition for implementation.

## 1. Reduce Reminder Diagnostic Logging
**What**: Verbose `Log.i` lines tagged `ReminderDiag` in `ReminderScheduler`, `ReminderReceiver`, and `ReminderActionReceiver`.
**Why deferred**: Kept at `Log.i` to survive in release builds for diagnosing missed reminders.
**Trigger**: Reminders have fired correctly for seven consecutive days, including at least one device reboot and at least one full repeat cycle that ran to the end of the window without confirmation.
**Action**: Downgrade informational lines to `Log.d` (stripped from release); keep `Log.e` and `Log.w`.

## 2. 16 KB Page-size Compliance
**What**: Align native libraries (`libonnxruntime.so`, `libonnxruntime4j_jni.so`, `libimage_processing_util_jni.so`, `libdatastore_shared_counter.so`, and `libandroidx.graphics.path.so`) to 16 KB page boundaries.
**Why deferred**: App is self-distributed; no external deadline. AndroidX libraries likely resolved by upgrades; ONNX depends on upstream.
**Trigger**: A dependency-upgrade pass or a decision to publish through the Play Store.

## 3. Navigation Migration to NavHost
**What**: Port from hand-rolled overlay state machine to `NavHost`. 1:1 port: same screens/ViewModels, hoisted entities as route id arguments, keeping `ViewModel()` and `ServiceLocator`, leaving magic-link intent handling in `MainActivity`.
**Why deferred**: Current state machine is functional for the current scope.
**Trigger**: Before any further screen work that would otherwise be built on top of the current state machine.

## 4. Unused String Resources
**What**: Remove roughly 32 unused entries in `strings.xml` (e.g., `schedule_edit_duration`, `schedule_edit_max_reminders`).
**Why deferred**: Previous attempt to remove them broke the build; needs careful verification.
**Trigger**: A general cleanup pass; requires verification against both locales and a full rebuild.

## 5. System Font Scaling
**What**: Ensure the app respects device system font-size settings for accessibility.
**Why deferred**: Focus was on core functionality; critical for the intended audience.
**Trigger**: Before the next UI pass.
**Action**: Audit typography for `sp` vs `dp` usage and verify at largest scale.

## 6. Reminder Settings Help Text
**What**: Replace verbose helper lines beneath repeat settings on the reminder configuration screen with "?" icons that open help text.
**Why deferred**: Current layout is functional; pending real-world feedback on verbosity.
**Trigger**: A decision after using the current layout on a device.

## 7. Measurement retrieval — date range and pagination (Cross-repo: Client + Backend)
**What**: Replace the `days` query parameter on GET /measurements with explicit date_from / date_to filtering plus pagination, and add period presets in the history screen (week, month, three months, since last prescription, all).
**Why deferred**: The backend caps `days` at 365 (Query(ge=1, le=365)), so the client currently backfills with days=365. This works today — the dataset spans April 2026 onward, roughly 700 records per year — but breaks once history exceeds one year, and returning everything in a single unpaginated response does not scale.
**Trigger**: Before the dataset approaches one year of history, or the next time the history screen is worked on. Requires a backend change first, then the client.
