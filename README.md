# AppPulse — Phase 1

Personal app usage tracker and categorizer for Android (tested target: Realme 14 5G / Android 14+).

## Features (Phase 1)

- Requests **Usage Access** (PACKAGE_USAGE_STATS) via the special Settings screen
- Collects foreground usage time for the last **7 days** for all launchable apps
- Displays a sorted list (Most used / Least used) with app icon, name, and formatted time (`Xh Ym`)
- Manual category tagging with persistence via **Room**
- Categories already used appear as quick-select chips
- Default category: `Uncategorized`

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer (or Android Studio Ladybug+)
- JDK 17
- minSdk 26, targetSdk 34
- Device or emulator with Android 8.0+

## Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/ZINC-LESTRIC/apppulse.git
   cd apppulse
   ```

2. Open the project in **Android Studio**.

3. Let Gradle sync (it will download dependencies).

4. Connect a real device (recommended for accurate usage stats) or start an emulator.

5. Click **Run** ▶ (or `./gradlew installDebug`).

> **Note:** On first open the app will show a “Grant Access” screen because `PACKAGE_USAGE_STATS` cannot be requested via a normal runtime dialog.

## Granting Usage Access Permission

1. Tap **Grant Access in Settings**.
2. In the system screen, find **AppPulse** and toggle **Allow usage access** (or “Permit usage access”).
3. Press the back button / return to AppPulse.
4. The app automatically detects the permission on `ON_RESUME` and loads the list.

You can also open the same screen manually:
**Settings → Apps → Special app access → Usage access → AppPulse**.

## Project Structure

```
app/src/main/java/com/ahmar/apppulse/
├── MainActivity.kt          # Entry point + permission re-check on resume
├── MainScreen.kt            # Compose UI (list, chips, dialogs)
├── AppPulseViewModel.kt     # State + Room + sorting
├── UsageStatsHelper.kt      # UsageStatsManager + PackageManager logic
└── data/
    ├── AppCategory.kt         # Room entity
    ├── AppCategoryDao.kt      # DAO
    └── AppDatabase.kt         # Room database
```

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room (local persistence)
- UsageStatsManager + AppOpsManager
- Coroutines + Flow + ViewModel
- Single-activity architecture

## Acceptance Criteria (Phase 1)

- [x] Builds and runs on a real device without crashing
- [x] Shows grant-access screen when permission is missing and correctly detects grant on return
- [x] List shows real usage data for launchable apps, sorted by usage time
- [x] Category assignment persists across app restarts (Room)

## Later Phases (not implemented)

Notifications, TTS, Accessibility services, voice features, etc.

---

Built for Realme 14 5G / Android 14+ · Package: `com.ahmar.apppulse`
