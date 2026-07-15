# План впровадження — Prompt 04: Налаштування нагадувань

Цей етап включає створення клієнтського шару даних для конфігурації нагадувань (`reminder_config`) та заміну існуючого екрана редагування розкладу на нову форму налаштувань.

## User Review Required

> [!IMPORTANT]
> - Використовується DTO-through патерн: дані з мережі напряму потрапляють у ViewModel без проміжного шару доменних моделей.
> - На даному етапі Room не використовується для кешування конфігурації; дані запитуються та зберігаються лише через мережу.
> - HTTP 404 при отриманні конфігурації розглядається як нормальний стан ("не налаштовано"), що призводить до використання значень за замовчуванням.
> - Старі файли `ScheduleEditScreen.kt` та `ScheduleEditViewModel.kt` будуть видалені.

## Proposed Changes

### Data Layer (DTO, API, Repository)

#### [NEW] [ReminderConfigDtos.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/dto/ReminderConfigDtos.kt)
Створення DTO для конфігурації:
- `morningTime`, `dayTime`, `eveningTime` (String, формат `HH:MM:SS`)
- `maxReminders` (Int)
- `durationMinutes` (Int)

#### [NEW] [ReminderConfigApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/ReminderConfigApi.kt)
- `GET reminders/config`
- `PUT reminders/config`

#### [NEW] [ReminderConfigRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/ReminderConfigRepository.kt)
- Метод `getConfig()`: повертає DTO або `null` при 404.
- Метод `saveConfig(config)`: виконує PUT запит.

#### [MODIFY] [ServiceLocator.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/core/di/ServiceLocator.kt)
Реєстрація `ReminderConfigApi` та `ReminderConfigRepository`.

---

### UI Layer (Feature: Reminders)

#### [NEW] [ReminderConfigViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigViewModel.kt)
- Стан форми (плоский `data class`).
- Завантаження існуючої конфігурації або встановлення дефолтів.
- Валідація полів.
- Логіка збереження.

#### [NEW] [ReminderConfigScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigScreen.kt)
- Використання Material3 `TimePicker` для трьох слотів часу.
- Поля вводу для цілих чисел (ліміт нагадувань, тривалість вікна підтвердження).
- Відображення стану завантаження/помилки.

#### [DELETE] [ScheduleEditScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleEditScreen.kt) & [ScheduleEditViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleEditViewModel.kt)
Видалення застарілих компонентів.

---

### Navigation & Localization

#### [MODIFY] [MainActivity.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/MainActivity.kt)
Заміна маршруту `schedule_edit` на `reminder_config`.

#### [MODIFY] [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt)
Оновлення дії кнопки редагування для переходу на `reminder_config`.

#### [MODIFY] [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml) & [strings-uk.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml)
Додавання нових рядків для форми налаштувань.

## Verification Plan

### Automated Tests
- Складання проекту: `./gradlew app:assembleDebug`.

### Manual Verification
- Відкриття форми налаштувань з вкладки розкладу.
- Перевірка заповнення дефолтними значеннями при відсутності конфігурації (404).
- Перевірка роботи `TimePicker` для кожного слота.
- Валідація: кнопка збереження неактивна, якщо числа <= 0.
- Успішне збереження та повернення назад.
- Перевірка локалізації обома мовами.
