# План впровадження — Prompt: Призупинення роботи нагадувань

Цей етап спрямований на тимчасове нейтралізування незавершеної логіки нагадувань, щоб уникнути помилок на екрані та фонових збоїв, залишаючи при цьому функціонал рецептів та конфігурації годин повністю працездатним.

## User Review Required

> [!IMPORTANT]
> - Всі фонові задачі та аларми нагадувань будуть деактивовані.
> - Вкладка розкладу відображатиме заглушку "Reminders — coming soon" замість спроби завантаження даних.
> - Перемикач нагадувань у налаштуваннях буде встановлено у положення "Вимкнено" за замовчуванням.
> - **Точки входу до налаштувань конфігурації та рецептів залишаються доступними.**

## Proposed Changes

### Feature: Schedule (Розклад)

#### [MODIFY] [ScheduleViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleViewModel.kt)
- Видалити логіку виклику `repository.getToday()` та `repository.confirm()`.
- Встановити початковий стан як `ScheduleState.Empty` (або новий стан-заглушку).

#### [MODIFY] [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
- Оновити відображення стану `Empty` або додати спеціальну секцію для виведення повідомлення про те, що нагадування з'являться згодом.
- Зберегти кнопки переходу до рецептів та налаштувань у `TopAppBar`.

#### [MODIFY] [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml) & [strings-uk.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)
- Додати рядок `schedule_coming_soon`.

---

### Feature: Reminders Runtime (Фонова робота)

#### [MODIFY] [ReminderScheduler.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderScheduler.kt)
- Зробити метод `rescheduleAll()` порожнім (no-op), щоб він не робив мережевих запитів та не планував аларми.

#### [MODIFY] [ReminderReceiver.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderReceiver.kt)
- Зробити `onReceive()` порожнім, щоб він ігнорував будь-які події (включаючи `BOOT_COMPLETED` та аларми).

---

### Feature: Settings (Налаштування)

#### [MODIFY] [SettingsViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/settings/SettingsViewModel.kt)
- Оновити `refresh()`, щоб перемикач нагадувань завжди мав стан `false` (або `null`), і не викликати репозиторій.

## Verification Plan

### Automated Tests
- Складання проекту: `./gradlew app:assembleDebug`.

### Manual Verification
- Перехід на вкладку розкладу: має відображатися повідомлення-заглушка без помилок.
- Перевірка, що кнопки "Рецепти" та "Налаштування" на вкладці розкладу працюють.
- Перевірка, що в налаштуваннях перемикач нагадувань вимкнений.
- Перевірка відсутності фонових збоїв (через логування, якщо можливо).
