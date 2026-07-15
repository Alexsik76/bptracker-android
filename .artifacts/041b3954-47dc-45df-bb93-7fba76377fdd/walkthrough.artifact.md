# Walkthrough — Призупинення роботи нагадувань

Тимчасово деактивовано незавершену логіку нагадувань для стабілізації додатка перед переходом до наступного етапу розробки.

## Зміни

### Інтерфейс розкладу
- **Placeholder**: Оновлено [ScheduleViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleViewModel.kt) та [ScheduleScreen.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/home/ScheduleScreen.kt). Тепер вкладка розкладу відображає повідомлення "Нагадування з'являться згодом" (локалізовано обома мовами) замість спроби завантаження даних із застарілих ендпоінтів.
- **Точки входу**: Кнопки переходу до налаштувань годин (`reminder_config`) та списку рецептів залишилися активними та функціональними.

### Фонова логіка (Runtime)
- **Деактивація планувальника**: Метод `rescheduleAll` у [ReminderScheduler.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderScheduler.kt) перетворено на no-op (порожній метод).
- **Безпечний приймач**: [ReminderReceiver.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/reminders/ReminderReceiver.kt) тепер ігнорує всі вхідні події (аларми, перезавантаження пристрою), що запобігає фоновим збоям через звернення до відсутніх API.

### Налаштування
- **Стан за замовчуванням**: У [SettingsViewModel.kt](file:///D:/dev/bp_tracker/mobile_app/app/src/main/java/ua/vn/home/bptracker/feature/settings/SettingsViewModel.kt) логіку перемикача нагадувань змінено так, щоб він завжди був вимкнений за замовчуванням і не ініціював жодних мережевих або локальних дій.

## Результати перевірки

### Автоматичні тести
- Виконано `./gradlew app:assembleDebug` — **Збірка успішна**.

### Ручна перевірка
- Вкладка розкладу відкривається без помилок і відображає заглушку.
- Навігація до рецептів та конфігурації годин працює коректно.
- Перемикач у налаштуваннях не викликає побічних ефектів.
