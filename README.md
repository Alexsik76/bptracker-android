# BP Tracker (Android)

A native Android app for monitoring blood pressure and managing medication schedules. It is part of the BP Tracker ecosystem, focusing on fast data entry (including camera scanning) and reminders.

Development is currently happening in the `dev` branch.

## 🟢 Key Features (MVP)

### 📊 Blood Pressure Control
- **Quick Entry:** Manual form or scanning the tonometer screen with your camera.
- **Optimistic UI:** Instant saving of measurements to the local database with background sync.
- **Local OCR:** Recognition of SYS/DIA/Pulse values directly on the device using **ONNX Runtime**.
- **History and Details:** View recent measurements, classified by zones (Optimal, Normal, Stage 1/2) according to ESC/ESH guidelines.
- **Export:** Send measurement history as a CSV file via email.

### 💊 Medications and Reminders
- **Today's Schedule:** A convenient list of doses **calculated on-device** based on active prescriptions and set times.
- **Confirmation:** Quick "Taken" button with offline buffering and sync when online.
- **Reminders:** Local push notifications via `AlarmManager` that work offline. This is an **opt-in** feature (can be turned off in settings).
- **Prescriptions:** Manage doctor's prescriptions and see details for each medicine.

### 🔐 Security and Settings
- **Authorization:** Supports modern **Passkeys** (Credential Manager) and Magic Links for passwordless login.
- **Personalization:** Light and dark themes, support for Ukrainian and English.
- **Privacy:** Option to disable sending photos to help improve the OCR model.

## 🛠 Tech Stack
- **Language:** Kotlin 2.2.x
- **SDK:** compileSdk/targetSdk 35, minSdk 28
- **UI:** Jetpack Compose (Material 3, BOM 2024.12.01)
- **Architecture:** MVVM (Compose + ViewModel), Repository pattern, DTO-through, Offline-First (Room SSoT).
- **UI UX:** Token-based design system, Shimmer effects, adaptive Window Insets (Edge-to-Edge), dynamic navigation (Hide on Scroll).
- **Tools:** AGP 9.2.1, Gradle 9.6.1, KSP
- **Local DB:** Room 2.8.4
- **Networking:** Retrofit 2.11.0, OkHttp 4.12.0, Kotlinx Serialization 1.7.3
- **Camera and AI:** CameraX 1.4.0, ONNX Runtime 1.19.0 (for the local recognition model)
- **Navigation:** Jetpack Navigation Compose 2.8.5
- **DI:** Lightweight ServiceLocator

## 📂 Project Structure
- `ua.vn.home.bptracker.core`: Network layer, DI, token and setting storage.
- `ua.vn.home.bptracker.data`: API interfaces, Room DB, repositories, and DTOs.
- `ua.vn.home.bptracker.feature`: Logic and screens grouped by feature (home, prescriptions, reminders, camera, login, settings).
- `ua.vn.home.bptracker.ui`: Shared components and app theme.

## 🚀 How to Run
1. **Clone:** `git clone -b dev https://github.com/Alexsik76/bptracker-android.git`
2. **Setup:** By default, the app is connected to `https://api2-bptracker.home.vn.ua/`. For local development without a backend, you can enable `MOCK_MODE = true` in `ua.vn.home.bptracker.core.config.DevConfig`.
3. **Build:** `./gradlew assembleDebug`

## 🟡 Future Plans and Known Limitations

### Known Limitations / Deferred
- **Simplified Course Mechanism:** Medicines with a limited course appear in the schedule based on `courseStart`, but don't end automatically after a certain number of doses (requires manual deactivation).
- **16 KB page-size:** Support is blocked at the ONNX Runtime JNI level.

### Plans
- **Live-OCR:** Recognition through frame consensus (see `docs/live-ocr-scanning.md`).
- Integration with **Health Connect** to sync data.
- Support for **Bluetooth (BLE)** tonometers.

---
© 2026 BP Tracker Team
