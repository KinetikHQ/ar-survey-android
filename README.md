# AR Survey & Inspection — Android App

Kotlin + CameraX app for AR construction site inspection. Captures video clips, uploads to backend, displays detection results.

---

## Prerequisites

- **Android Studio** (latest stable) — https://developer.android.com/studio
- **JDK 17+** (bundled with Android Studio)
- **Physical Android device** or emulator (Android 10+ / API 29+)

## Setup

1. Clone the repo:
   ```powershell
   git clone https://github.com/KinetikHQ/ar-survey-android.git
   ```

2. Open in Android Studio:
   - File → Open → select the `ar-survey-android` folder
   - Wait for Gradle sync to complete

3. Connect your device:
   - **Emulator:** Device Manager → Create Device → Pixel 6 → API 33+
   - **Physical phone:** Settings → About Phone → Tap Build Number 7 times → Developer Options → Enable USB Debugging → Connect via USB

## Configure API Connection

The app defaults to `http://10.0.2.2:8000` (emulator → host localhost). You'll need to change this.

### Emulator
Leave as default (`http://10.0.2.2:8000`). Make sure the backend is running on the host machine.

### Physical device on same Wi-Fi
In the app: Settings → API Base URL → `http://YOUR_PC_IP:8000`

To find your Windows IP:
```powershell
ipconfig | findstr "IPv4"
```

### API Key
In the app: Settings → API Key → `dev-api-key-change-in-prod`

Tap **Test Connection** to verify.

## Run

1. Start the [backend](https://github.com/KinetikHQ/ar-survey-backend) on your machine
2. In Android Studio, click ▶️ Run
3. Select your device (emulator or physical phone)
4. App launches — record a clip → upload → view results

## Architecture

```
com.kinetik.arsurvey/
├── api/          # Retrofit API client + models
├── data/         # Room DB (offline queue)
├── queue/        # Upload queue manager
├── ui/           # Compose screens
│   ├── MainActivity      # Entry point
│   ├── CameraScreen      # Camera preview + capture
│   ├── JobsScreen        # Job list
│   ├── JobDetailScreen   # Detection results
│   ├── SettingsScreen    # API URL + key config
│   └── Navigation        # Nav host
└── util/         # Network monitor, preferences, glasses notifier
```

## Offline Behaviour

| Connection | Behaviour |
|---|---|
| Wi-Fi | Immediate upload |
| Cellular | Upload with data saver check |
| Offline | Clips saved locally, upload on reconnect |

Failed uploads show a clear error with manual retry button. One silent retry before prompting the user.

---

Internal use only. Kinetik © 2026.
