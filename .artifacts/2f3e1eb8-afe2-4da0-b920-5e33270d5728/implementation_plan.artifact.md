# Implementation Plan - Cosmetic Fixes and Warning Cleanup

This plan covers behavior-preserving cosmetic fixes, addressing deprecation warnings, and cleaning up minor code quality issues.

## Proposed Changes

### Fix 1: Reminder Notification Text
#### [MODIFY] [ReminderReceiver.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderReceiver.kt)
- Fix dose label building to avoid trailing spaces inside parentheses (e.g., `"Аторис (1 )"` -> `"Аторис (1 мг)"` or `"Аторис (1)"`).
- Use `context.getString` with localized dose units.
- Add clarifying parentheses to the `if (!enabled ...)` expression.
- Add necessary imports for `DoseUnit` and `R`.

### Fix 2: Deprecated Icons
#### [MODIFY] [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
- Replace `Icons.Default.Assignment` with `Icons.AutoMirrored.Filled.Assignment`.
- Add `androidx.compose.material.icons.automirrored.filled.Assignment` import.

### Fix 3: Warning Cleanup
I will address the following minor warnings across several files:

#### [MODIFY] [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
- Remove unused import `TodayMed`.
- Remove unused parameter `e` in `getLocalizedTime`.
- Add missing trailing commas.
- Move lambda argument out of parentheses.
- Fold simple `if` statements where applicable.
- Add parameter names for boolean literals in `mutableStateOf`.
- Add missing line breaks in `TextButton` lambdas.

#### [MODIFY] [SettingsScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/settings/SettingsScreen.kt)
- Remove unused `Color` import.
- Add missing trailing commas.
- Add clarifying parentheses.

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
- Remove unused `BackHandler` import.
- Add missing trailing commas.
- Move lambda argument out of parentheses.
- Add missing line breaks in `TextButton` lambdas.

#### [MODIFY] [TodayScheduleUseCase.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/TodayScheduleUseCase.kt)
- Remove unnecessary `!!` non-null assertion.
- Add missing trailing commas.
- Remove explicit type arguments that can be inferred.
- Add clarifying parentheses.

#### [MODIFY] [HomeScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/HomeScreen.kt)
- Remove unused `KeyboardArrowRight` import.
- Remove unused parameter `onLogout`.
- Add missing trailing commas.
- Move lambda argument out of parentheses.
- Add clarifying parentheses.

#### [MODIFY] [MedicationItemFormScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/prescriptions/MedicationItemFormScreen.kt)
- Remove unused parameter `e`.
- Add missing trailing commas.
- Add parameter names for boolean literals.
- Add missing line breaks in `TextButton` lambdas.
- Fold simple `if` statements.

## Verification Plan
### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify compilation and check if warnings are reduced.
- Run `./gradlew :app:testDebugUnitTest` to ensure no regressions.

### Manual Verification
- Trigger a reminder notification and verify the medication label format (e.g., `"Drug (10 mg)"` or `"Drug (1)"` if unit is null).
- Check the Schedule screen to ensure the prescriptions icon is displayed correctly.
