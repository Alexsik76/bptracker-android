# Walkthrough — Prompt 04: Налаштування нагадувань

Реалізовано клієнтський шар даних та інтерфейс для налаштування нагадувань (`reminder_config`), що замінив застарілий механізм редагування розкладу.

## Зміни

### Шар даних (Data Layer)
- **DTO**: Створено [ReminderConfigDtos.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/dto/ReminderConfigDtos.kt) для передачі налаштувань (час ранку/дня/вечора, ліміти).
- **API**: Додано [ReminderConfigApi.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/api/ReminderConfigApi.kt) з методами GET та PUT.
- **Repository**: Реалізовано [ReminderConfigRepository.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/data/repository/ReminderConfigRepository.kt), який обробляє HTTP 404 як стан "не налаштовано" (повертає `null`).
- **DI**: Нові компоненти зареєстровані в `ServiceLocator`.

### Інтерфейс (UI Layer)
- **Нова форма**: Впроваджено [ReminderConfigScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigScreen.kt) та [ReminderConfigViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderConfigViewModel.kt).
- **Вибір часу**: Використовуються Material3 `TimePicker` для зручного встановлення годин та хвилин.
- **Валідація**: Кнопка збереження активується лише при коректно заповнених полях (позитивні числа для лімітів).
- **Локалізація**: Додано всі необхідні рядки в [strings.xml](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values/strings.xml) та [strings.xml (uk)](file:///D:/dev/bp_tracker/mobile_app/app/src/main/res/values-uk/strings.xml).

### Очищення та інтеграція
- Видалено застарілі файли `ScheduleEditScreen.kt` та `ScheduleEditViewModel.kt`.
- Маршрут `schedule_edit` у `MainActivity.kt` замінено на `reminder_config`.
- Оновлено кнопку редагування на екрані розкладу для переходу до нових налаштувань.

## Результати перевірки

### Автоматичні тести
- Проект успішно збирається: `./gradlew app:assembleDebug`.

### Ручна перевірка
- Форма відкривається з вкладки розкладу.
- При першому відкритті (404 з сервера) підставляються дефолтні значення (08:00, 14:00, 20:00).
- `TimePicker` коректно оновлює значення в полях.
- Збереження виконує PUT запит та повертає користувача на попередній екран.
- Перевірено українську локалізацію.
