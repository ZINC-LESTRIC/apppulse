# AppPulse — Phase 1 + Phase 2

Personal app usage tracker, categorizer, and notification reader for Android (tested target: Realme 14 5G / Android 14+).

## Features

### Phase 1 — Usage tracking
- Requests **Usage Access** (PACKAGE_USAGE_STATS) via the special Settings screen
- Collects foreground usage time for the last **7 days** for all launchable apps
- Displays a sorted list (Most used / Least used) with app icon, name, and formatted time (`Xh Ym`)
- Manual category tagging with persistence via **Room**
- Categories already used appear as quick-select chips
- Default category: `Uncategorized`

### Phase 2 — Notification reader + TTS
- **NotificationListenerService** reads incoming notifications
- Per-app **allowlist** (everything OFF by default)
- Reads allowed notifications aloud via **Text-to-Speech**: "[App] says: [title]. [text]"
- Skips AppPulse’s own notifications and ongoing/foreground-service notifications
- Notification **log** of the last 50 spoken/logged notifications
- Bottom navigation: **Usage** | **Allowlist** | **Log**

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- minSdk 26, targetSdk 34
- Real device recommended (notification listener + accurate usage stats)

## Build & Run (Local)

1. Clone:
   ```bash
   git clone https://github.com/ZINC-LESTRIC/apppulse.git
   cd apppulse
   ```
2. Open in Android Studio → Sync Gradle → Run on a device.

## Download pre-built Debug APK

1. Go to **Actions**: https://github.com/ZINC-LESTRIC/apppulse/actions
2. Open the latest successful **Build Debug APK** run
3. Download the artifact **`apppulse-debug-apk`**
4. Install on your phone (enable “Install unknown apps”)

## Required special permissions

### 1. Usage Access (Phase 1)
Settings → Apps → Special app access → Usage access → AppPulse → Allow

### 2. Notification access (Phase 2)
Settings → Apps → Special app access → Notification access → AppPulse → Allow

(Or use the in-app buttons that open these screens directly.)

After granting either permission, return to the app — it re-checks on resume.

## How to use Phase 2

1. Grant **Notification access**.
2. Open the **Allowlist** tab and turn ON the apps whose notifications you want read aloud.
3. Receive a notification from an enabled app → it is spoken and appears in the **Log** tab.

## Project structure (key files)

```
app/src/main/java/com/ahmar/apppulse/
├── MainActivity.kt
├── MainScreen.kt                 # Bottom tabs + Phase 1 UI
├── AppPulseViewModel.kt
├── UsageStatsHelper.kt
├── NotificationAccessHelper.kt
├── NotificationReaderService.kt  # NotificationListenerService
├── TtsManager.kt
├── NotificationAccessScreen.kt
├── AllowlistScreen.kt
├── NotificationLogScreen.kt
└── data/
    ├── AppCategory.kt / Dao
    ├── NotificationAllowlist.kt / Dao
    ├── NotificationLog.kt / Dao
    └── AppDatabase.kt            # version 2
```

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room
- UsageStatsManager + NotificationListenerService + TextToSpeech
- Coroutines + Flow + ViewModel

---

Built for Realme 14 5G / Android 14+ · Package: `com.ahmar.apppulse`
