# BP Tracker — план реалізації нативного застосунку (Android / Jetpack Compose)

Документ для агента в Android Studio. Описує, **як перекласти затверджені мокапи** (`BP Tracker — Native Mockups.dc.html`) у код. Натив = «сьогодні + скан + ліки/нагадування»; повна аналітика й редагування схем — у вебі. Темна тема за замовчуванням, світла — обовʼязкова. Material 3.

---

## 0. Технологічний стек (припущення)

| Шар | Бібліотека |
|---|---|
| UI | Jetpack Compose + Material 3 (`androidx.compose.material3`) |
| Навігація | Navigation-Compose (`androidx.navigation:navigation-compose`) |
| Стан / DI | ViewModel + StateFlow; Hilt (опційно) |
| Локальна БД | Room (заміри, ліки, прийоми, офлайн-черга) |
| Камера | CameraX (`androidx.camera`) |
| OCR | ONNX Runtime Android (локальна модель) + fallback HTTP на `/recognize` |
| Нагадування | AlarmManager (точні алярми) + Notification з action «Прийняв» |
| Мережа | Retrofit/OkHttp (синхронізація, експорт CSV `/export/csv`) |
| Шрифти | DM Sans + DM Mono (Google Fonts через `androidx.compose.ui.text.googlefonts` або bundled `res/font`) |

Мін. SDK: 26+ (AlarmManager exact, notification actions). Compile: latest stable.

---

## 1. Дизайн-токени

Винести в `ui/theme/` (`Color.kt`, `Type.kt`, `Dimens.kt`, `Theme.kt`). Це **бренд-тема, не Material You** — кольори фіксовані, без dynamic color.

### 1.1 Кольори — тема DARK (default)
```
bgPrimary       #0D1117
surface         #FFFFFF @ 5%   (рекоменд. solid: #141920)
surfaceElevated #FFFFFF @ 5%   (картки), border #FFFFFF @ 8%
navBar          #11151C (≈ rgba(17,21,28,0.9))
textPrimary     #FFFFFF
textMuted       #FFFFFF @ 45%
textDim         #FFFFFF @ 25%
primary         #818CF8   (акції, активний таб, посилання)
onPrimary       #0B1020
```

### 1.2 Кольори — тема LIGHT
```
bgPrimary  #F2F3F5
surface    #FFFFFF        border #000000 @ 7%
textPrimary #0F0F12       textMuted #000000 @ 45%
primary    #6366F1        onPrimary #FFFFFF
```

### 1.3 Кольори зон тиску (СПІЛЬНІ з вебом — не змінювати)
| Зона | Dark fg | Dark bg-chip | Light fg | Light bg-chip |
|---|---|---|---|---|
| optimal | `#22C55E` | `#22C55E` @ 12% | `#16A34A` | `#DCFCE7` |
| normal | `#84CC16` (текст великих чисел `#A3E635`) | `#84CC16` @ 12% | `#65A30D` | `#ECFCCB` |
| stage1 | `#F97316` (chip text `#FB923C`) | `#F97316` @ 12% | `#C2410C` | `#FED7AA` |
| stage2 | `#EF4444` | `#EF4444` @ 12% | `#DC2626` | `#FEE2E2` |

Семантика: `danger #EF4444`, `success #22C55E`, `warning #F97316`.
Кольори-ідентифікатори значень у формі результату: СИС `#818CF8`, ДІА `#38BDF8`, Пульс `#4ADE80`.

### 1.4 Типографіка
- **DM Sans** — увесь UI-текст.
- **DM Mono** — усі числа (заміри, KPI, час, діапазони).

Шкала (Compose `Typography` + локальні стилі):
| Роль | Шрифт | Розмір / вага |
|---|---|---|
| Великий показник (hero) | DM Mono | 56sp / 500, letter-spacing −2 |
| Показник у деталі | DM Mono | 66sp / 500 |
| Поле значення (форма) | DM Mono | 33sp / 500 |
| KPI value | DM Mono | 25sp / 500 |
| Заголовок екрана (app bar) | DM Sans | 17–18sp / 600–700 |
| Wordmark | DM Sans | 19sp / 700 |
| Назва картки/ряду | DM Sans | 14.5–15sp / 600 |
| Label (uppercase, секції) | DM Sans | 11sp / 600, letter-spacing 1, UPPERCASE |
| Caption / muted | DM Sans | 12–13sp / 400–500 |

### 1.5 Spacing, радіуси, тіні
- Spacing scale: **4 / 8 / 12 / 16 / 20 / 28** (dp). Горизонтальні поля екрана = 16dp.
- Радіуси: картки **16–20dp**, поля/кнопки **14dp**, pill/chip — **повний** (50%).
- Елевація: світла тема — мʼяка тінь (`0 1–2dp`); темна — без тіней, лише border `1dp`.
- Тач-таргети ≥ **48dp** (іконкові кнопки 40dp візуально + 48dp клікабельна зона).

---

## 2. Базові компоненти (Composables)

Створити пакет `ui/components/`. Усі приймають зону/тему через параметри.

1. **`BpCard(modifier, content)`** — поверхня: surface bg + 1dp border + RoundedCorner(16–20). Базовий контейнер усіх карток.
2. **`ZoneBadge(zone: BpZone)`** — pill: fg/bg-chip за зоною, текст `zone.labelUk` (Оптимальний / Норма / 1 ступінь / 2 ступінь), 12sp/700.
3. **`KpiTile(label, value, sub, valueColor)`** — у `LazyVerticalGrid`/`Row` 2 колонки. value моноширинний.
4. **`ReadingValue(sys, dia, zone, size)`** — великий моно показник `SYS`/`DIA`, тонований у `zone.color`, `/` у `textDim`.
5. **`PositionScaleBar(severity: Float 0..1, marker, labels)`** — горизонтальний градієнт `optimal→normal→stage1→stage2` + білий маркер-кружок з обведенням під колір фону. Підписи «Норма / Небезпека». Позиція: optimal≈0.15, normal≈0.42, stage1≈0.68, stage2≈0.9 (або обчислювати з тяжкості).
6. **`Sparkline(points)`** — `Canvas` polyline, stroke = колір зони, без заливки.
7. **`ValueField(label, secondary, dotColor, value, unit, onClick)`** — ряд результату розпізнавання: кольорова точка + назва + box зі значенням і одиницею. Tap → редагування (number keyboard).
8. **`StepperField(...)`** *(опційно, для ручного вводу)* — −/значення/+ .
9. **`MedRow(name, dose, time, status, onConfirm)`** — статуси `Taken / Pending / Missed` (іконка ✓/годинник/✕, колір success/warning/danger), action-кнопка «Прийняв».
10. **`SegmentedControl(options, selectedIndex, onSelect)`** — pill-контейнер, активний сегмент = `primary @ 22%`.
11. **`BpSwitch`, `SettingRow`, `ListGroupCard`** — налаштування (рядки з chevron / value / toggle).
12. **`AppTopBar(title, onBack?, actions?)`** — Material `TopAppBar`; ліворуч back (←) або wordmark, праворуч icon-кнопки (шестерня / «?» / кошик).
13. **`BottomNavBar(selected, onSelect, onScan)`** — Material 3 `NavigationBar` з 2 пунктами (Дашборд / Розклад) і **центральним docked FAB «Скан»** (зелений `#22C55E`/`#16A34A`, світіння, іконка viewfinder). Реалізувати через `Scaffold(bottomBar, floatingActionButton = scanFab, floatingActionButtonPosition = Center)` або кастомний layout.
14. **`ConfirmDialog(title, body, confirmLabel, danger, onConfirm, onDismiss)`** — Material `AlertDialog`.
15. **Стани:** `LoadingState`, `EmptyState`, `ErrorState`, `SnackbarHost` (підтвердження/помилки).

> **Іконки:** Material Symbols / `material-icons-extended`. Шестерня = `Settings`, довідка = `HelpOutline`, скан = `CropFree`/`QrCodeScanner`, кошик = `DeleteOutline`, ліки = `Medication`, календар = `CalendarMonth`, дашборд = `GridView`.

---

## 3. Модель зон тиску (бізнес-логіка)

`domain/BpZone.kt` — **єдине джерело істини**, спільне з вебом:
```
enum class BpZone(labelUk, colorDark, colorLight) { OPTIMAL, NORMAL, STAGE1, STAGE2 }

fun classify(sys: Int, dia: Int): BpZone {
    // правило «гірша категорія перемагає» (worst-of)
    val s = when { sys >= 160 -> STAGE2; sys >= 140 -> STAGE1; sys >= 120 -> NORMAL; else -> OPTIMAL }
    val d = when { dia >= 100 -> STAGE2; dia >=  90 -> STAGE1; dia >=  80 -> NORMAL; else -> OPTIMAL }
    return maxOf(s, d)   // за ordinal
}
```
Пороги: optimal `sys<120 і dia<80` · normal `120–139 або 80–89` · stage1 `140–159 або 90–99` · stage2 `≥160 або ≥100`.
Валідація вводу: SYS 40–300, DIA 20–200, Пульс 30–250.

---

## 4. Екрани та навігація

Navigation-Compose, корінь — `Scaffold` з `BottomNavBar` лише для табів `Dashboard` і `Schedule`. Решта — push-екрани (back arrow).

```
NavHost
 ├─ dashboard            (таб)   DashboardScreen
 ├─ schedule             (таб)   ScheduleScreen
 ├─ scan                 (FAB)   CameraScanScreen ─► recognitionResult
 ├─ recognitionResult    push    RecognitionResultScreen   // = «Результат розпізнавання»
 ├─ measurement/{id}     push    MeasurementDetailScreen
 ├─ settings             push    SettingsScreen
 ├─ help/bpScale         push    BpScaleHelpScreen
 └─ login                root    LoginScreen (passkey)
```

### 4.1 Dashboard «Сьогодні» (головний, dark + light)
TopBar: wordmark «∿ BP Tracker» + **шестерня** (→ settings).
Контент (`LazyColumn`):
- **Hero `BpCard`** — `ОСТАННІЙ ВИМІР · {time}` + `ZoneBadge` + (i); великий `ReadingValue` (тон зони); `мм рт.ст. · ♡{pulse} уд/хв`; `Sparkline`; `PositionScaleBar`.
- **KPI grid 2×2**: Серед. 7д (`133/77`, зона), Зміна за тиждень (`+4/0`, danger/success зі стрілкою), У нормі 7д (`11/14`, `79%`), Пульс сер. (`66`).
- **Останні заміри** — заголовок + «Історія →»; `BpCard` з 2 рядками (точка зони + значення + ♡ + час).
- **Ліки сьогодні** — заголовок + лічильник; pending-рядок з кнопкою «Прийняв» (success-tonal).
- **BottomNavBar** + центральний **Scan FAB**.

### 4.2 Schedule «Розклад» (таб)
TopBar: «Розклад» + дата. Картка прогресу (`2 з 5 прийнято`, лінійний прогрес). Групи за періодами (`Рано/Ранок/День/Вечір · HH:MM`) з `MedRow` у трьох станах. **Snackbar** «Прийом підтверджено · Скасувати» після дії. BottomNav (активний — Розклад).

### 4.3 CameraScan + RecognitionResult «Результат розпізнавання»
- **CameraScanScreen**: CameraX preview, рамка-viewfinder, кнопка зйомки. Після фото → локальний ONNX OCR → перехід на результат.
- **RecognitionResultScreen** (затверджений макет):
  - TopBar: ← + «Результат розпізнавання».
  - **Фото** тонометра (повна ширина, RoundedCorner 18) + кнопка-оверлей **«Збільшити»** (top-end, напівпрозора) → повноекранний перегляд.
  - Три **`ValueField`**: Систолічний (верхній, точка `#818CF8`) `125`; Діастолічний (нижній, `#38BDF8`) `77`; Пульс (`#4ADE80`) `75`. Tap → редагування (number input, валідація з §3).
  - Кнопки **Скасувати** (outline) / **Зберегти** (filled primary). Save → `insert` у Room + back на Dashboard.
  - Посилання **«Розпізнати через сервер»** (fallback HTTP, якщо локальне OCR помилилось).
- Фото і дані — **на одному екрані**; усі поля редаговані до збереження.

### 4.4 MeasurementDetail + видалення
TopBar: ← + «Замір» + **кошик**. Центрований великий `ReadingValue` + `ZoneBadge` + дата + `♡пульс` + `PositionScaleBar`. Карта-метадані (Час, Зона, Пульс, Спосіб, Нотатка). Кнопка-outline danger «Видалити замір» → **`ConfirmDialog`** «Видалити замір?» (Скасувати / Видалити). Після видалення — back + Snackbar.

### 4.5 Settings
TopBar: ← + «Налаштування» + **«?»** (HelpOutline → `help/bpScale`).
Групи:
- **Вигляд** — `SegmentedControl` Тема: Авто / Світла / Темна.
- **Мова** — `SegmentedControl` Українська / English.
- **Приватність** — `BpSwitch` «Покращення моделі OCR» + helper «Надсилати фото дисплея анонімно».
- **Довідка** — рядок «Шкала тиску ESC →» (теж веде на `help/bpScale`).
- **Акаунт** — Сервер (value), «Експорт CSV на email» (→ `/export/csv`), «Вийти з акаунта» (danger).
- Футер версії.

### 4.6 BpScaleHelp «Шкала тиску ESC» (новий окремий екран)
TopBar: ← + «Шкала тиску ESC». Вступний текст (правило worst-of). Таблиця 4 рівнів (точка + назва + опис + моно-діапазон). Блок **«Ваш останній замір»** — значення + `ZoneBadge` + `PositionScaleBar` з маркером на позиції заміру. Дисклеймер (не замінює лікаря; ESC/ESH).

### 4.7 Login (passkey) — *є; реєстрація відкладена.*

---

## 5. Глобальні стани й системне
- **Loading / Empty / Error** для кожного списку (заміри, розклад).
- **Snackbar** для підтверджень і помилок; **ConfirmDialog** для деструктивних дій.
- **Офлайн-банер** (🟡 пізніше) + офлайн-черга замірів (Room) із подальшою синхронізацією.
- **Нагадування** (🟢 механіка уточнюється): AlarmManager → Notification з action «Прийняв» (підтвердження прийому без відкриття застосунку).
- **Теми**: підтримати `auto/light/dark` (системна + ручний вибір), збереження вибору в DataStore.

---

## 6. Шар даних (орієнтир)
- Room-сутності: `MeasurementEntity(id, sys, dia, pulse, takenAt, source, note, zone)`, `MedScheduleEntity`, `MedIntakeEntity(date, period, medId, status, confirmedAt)`.
- `BpRepository` / `MedRepository` + `ViewModel` (StateFlow `UiState`).
- Зона **обчислюється**, не зберігається як істина (зберігати можна як кеш).
- Експорт CSV — переадресація на наявний бекенд `/export/csv` (email).

---

## 7. Доступність і якість
- Контраст тексту/фону ≥ WCAG AA; не покладатися лише на колір зони — завжди дублювати текстовим лейблом (`ZoneBadge`).
- `contentDescription` для всіх іконкових кнопок (шестерня, скан, кошик, «?»).
- Тач-таргети ≥ 48dp; підтримка великого шрифту (sp).
- Тестувати обидві теми та довгі укр. рядки (без обрізань/накладань).

## 8. Поза скоупом цього раунду
UI нотифікацій-нагадувань (тільки механіка), Health Connect, BLE-імпорт, онбординг, велика аналітика/графіки (лишається у вебі), редагування схем лікування.

---

### Порядок реалізації (рекомендований)
1. Theme + токени + базові компоненти (§1–2) та `BpZone` (§3).
2. Dashboard (dark) → перевірити систему карток/KPI/нав-бар.
3. Light-тема Dashboard.
4. Scan → RecognitionResult (CameraX + OCR-заглушка спершу).
5. MeasurementDetail + ConfirmDialog.
6. Schedule + Snackbar.
7. Settings → BpScaleHelp.
8. Стани (loading/empty/error), офлайн-черга, експорт CSV, нагадування.
