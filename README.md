# HabitTracking

A native Android habit-tracking application engineered using **Jetpack Compose (Material 3)**. The application adheres strictly to the principles of **Clean Architecture** and the **MVVM (Model-View-ViewModel)** design pattern, delivering a robust, modular, and offline-first user experience.

---

## 🏗️ Architectural Framework & Design Patterns
The codebase is decoupled into distinct semantic layers to maximize testability, modularity, and maintainability:

- **Presentation Layer (Jetpack Compose):** Implements a purely declarative, unidirectional data flow (UDF). It leverages low-level graphic primitives inside a custom `Canvas` drawing block to render interactive weekly activity charts with real-time pointer-gesture tooltips.
- **Domain & State Management Layer (Hilt ViewModels):** Manages component lifecycles, intercepts runtime UI events, and exposes thread-safe `StateFlow` reactive streams to the view layer.
- **Data Abstraction Layer (Repository Pattern):** Encapsulates all remote cloud operations and caching. It protects the presentation layer from direct dependencies on external SDKs, abstracting data models cleanly.

---

## 📂 Production Project Directory Structure Map
The package hierarchy implements a **Package-by-Feature** presentation splitting and a unified model layout architecture inside your clean split:

```text
HabitTracking2/
├── gradle/                          # Centralised Version Catalog configuration structures
│   └── libs.versions.toml           # Configuration versions matching Kotlin 2.0.21 pipelines
├── app/                             # Core Application Module Folder
│   ├── google-services.json         # Firebase Cloud configuration file credentials
│   ├── build.gradle.kts             # Module build dependencies configuration script (SDK 34 targets)
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml  # Base platform declarations and background initialization nodes
│           └── java/com/example/habittracking/
│               ├── MainActivity.kt  # Base Activity context starting the Compose UI layout window
│               ├── HabitTrackerApplication.kt # Configures custom Hilt Workers runtime factories at boot time
│               │
│               ├── data/            # --- CORE DATA LAYER (Data Access & Abstractions) ---
│               │   ├── model/       
│               │   │   └── Models.kt # Unified Data Models (AppUser, Habit, etc.) & Enums
│               │   └── repository/  # Encapsulates Remote Cloud SDK Transactions
│               │       ├── AuthRepository.kt 
│               │       └── HabitRepository.kt # Handles server-side atomic Write Batch actions
│               │
│               ├── ui/              # --- PRESENTATION UI VIEWPORT LAYER (Declarative Screens) ---
│               │   ├── auth/        # LoginScreen.kt & SignUpScreen.kt authentication layouts
│               │   ├── calendar/    # CalendarScreen.kt layout displaying custom color status dots
│               │   ├── dashboard/   # DashboardScreen.kt & InteractiveWeeklyChart.kt custom drawing layers
│               │   ├── habitcreate/ # HabitCreateScreen.kt & HabitEditScreen.kt operational form states
│               │   ├── logentry/    # LogEntryScreen.kt logging panel driving timer, checklist, and journal
│               │   ├── splash/      # SplashScreen.kt managing launch security authentication token checks
│               │   └── theme/       # Color.kt, Theme.kt, and Type.kt Material 3 layout style sets
│               │
│               └── viewmodel/       # --- PRESENTATION STATE LAYER (Reactive Flow Nodes) ---
│                   ├── AuthViewModel.kt 
│                   ├── CalendarViewModel.kt
│                   ├── DashboardViewModel.kt
│                   ├── HabitCreateViewModel.kt
│                   ├── HabitEditViewModel.kt
│                   ├── LogEntryViewModel.kt
│                   └── LogEntryViewModelTest.kt # Automated validation runner verifying business logic rules
```

---

## 🚀 Key Technical Architectures Implemented

### 1. Robust Background Automation (Jetpack WorkManager)
Background processing is handled by a custom `CoroutineWorker` integrated with a decoupled **Hilt Worker Factory**. It bypasses the system's default background initializers via an explicit Jetpack **App Startup Manifest Node Merge** rule to constructor-inject data repositories cleanly.

#### How to Simulate the Background Reminders (Cross-Platform)
Instead of waiting 24 hours to verify that the worker runs correctly inside the Android OS `JobScheduler` system, you can use the Android Debug Bridge (`adb`) terminal tool to force execution immediately. Ensure your phone or emulator is connected with USB Debugging enabled, open your terminal pane, and copy the command matching your operating system:

*   **Linux / macOS (Bash / Zsh):**
    ```bash
    adb shell cmd jobscheduler run -f com.example.habittracking 1
    ```
*   **Windows (PowerShell):**
    ```powershell
    adb shell cmd jobscheduler run -f com.example.habittracking 1
    ```
*   **Windows (Command Prompt / CMD):**
    ```cmd
    adb shell cmd jobscheduler run -f com.example.habittracking 1
    ```

*Note: For these global shell shortcuts to function seamlessly, ensure your local Android SDK `platform-tools` environment path variables are correctly registered inside your operating system settings layout.*

### 2. Resilient Offline-First Syncing (Firestore SQLite Caching)
To ensure high availability under weak network environments, the application configures local SQLite disk caching. Write operations performed offline update the UI instantly and are queued to automatically sync atomically up to the Cloud Firestore Console as soon as network access restores.

### 3. Structural Data Cascading & Security
- **Atomic Batches:** When a user executes a habit deletion, a defensive Firestore Write Batch runs atomically on the server to completely erase the parent habit alongside all historical check-in sub-logs, eradicating orphaned data clutter.
- **Server-Side Security:** Data isolation is securely evaluated on the server using rule validation policies matching authenticated active user IDs (`request.auth.uid`).

---

## 🛠️ High-Performance Version Control & Toolchain
The development ecosystem maps directly to stable, high-performance tooling baselines:
- **Build System Engine:** Gradle 8.11 Wrapper Framework
- **Android Gradle Plugin (AGP):** Version 8.8.0
- **Compiler Configuration Tooling:** Kotlin 2.0.21 with Kotlin Symbol Processing (KSP)
- **Target Android SDK platform:** API Level 34 (Android 14)
- **Minimum System Requirements:** API Level 24 (Android 7.0)

---

## 🧪 Automated Business Logic Validation Tests
An automated diagnostic test module is embedded into the core initialization architecture. Every time the dashboard lands on its composition target, it validates core formatting requirements, spacing boundaries, and a strict 150-word journal text cap boundary constraint.

### Logcat Execution Verification Output
To view the live automated code verification report, launch the application on a target device, open the **Logcat** pane in Android Studio, and filter by typing `SCHOOL`:

```text
===========================================
🎓 SCHOOL PROJECT AUTOMATED VALIDATION STATUS:
journalWordCount_emptyString: PASSED ✅
journalWordCount_standardSentence: PASSED ✅
updateJournalText_exceedingWordCap: PASSED ✅
===========================================
```
