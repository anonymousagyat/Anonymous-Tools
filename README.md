# Anonymous-Tools

An offline-first, stealth silent screenshot, high-speed sequential screen recording, and protected media vault application for Android. Built with Jetpack Compose, Kotlin Coroutines, and the Android Accessibility Service, adhering to a clean Pastel Neo-Brutalist design language.

---

## Core Capabilities

### 1. Hardware Shortcut (Volume Up + Down)
- Pressing **Volume Up** and **Volume Down** simultaneously triggers an immediate silent screenshot from anywhere in the OS without needing to touch the screen or open the app.
- If a recording session is active, pressing the hardware combination immediately halts the capture, compiles the video, and saves it directly to the Private Vault.
- Intercepted at the raw key-event layer via `AccessibilityService.FLAG_REQUEST_FILTER_KEY_EVENTS` with tactile haptic feedback and zero shutter sounds.

### 2. Sequential Screenshot-to-Video Engine
- Replaces standard `MediaProjection` capture streams to completely bypass system casting dialogs, status bar recording pills, and third-party screenshot detection callbacks.
- **Asynchronous Disk Writing**: Frame capture and JPEG writing are decoupled onto background `Dispatchers.IO` threads, reducing capture blocking time to 0ms.
- **Maximized Hardware Frame Rate**: Leverages an adaptive 25ms to 35ms cooldown loop to capture frames at ~3.0 to 3.5 frames per second (the physical limit of Android hardware buffer transfers).
- **Adaptive OS Throttle Handling**: Automatically detects `ERROR_TAKE_SCREENSHOT_INTERVAL_RIGID` (code 3) and applies dynamic backoff, preventing system compositor congestion.
- **Hardware-Accelerated Encoding**: Sequence of captured frames is rendered directly to a native input Surface and compiled into an H.264 MP4 container via `MediaCodec` and `MediaMuxer`.
- **Dynamic 1:1 Real-Time Playback**: The video framerate is dynamically computed at compilation time based on the actual recorded elapsed duration, ensuring 1:1 real-time playback pacing.

### 3. Quick Settings Tile (1-Tap Control)
- Integrated into the Android system notification shade quick settings.
- Tapping the tile while idle opens the popup control menu.
- Tapping the tile while recording immediately stops capture, compiles the video, saves it to the vault, and provides a silent vibration confirmation.
- Operates in pure background mode with zero on-screen controllers or overlay windows, preventing black-box censoring or screen occlusion.

### 4. Isolated Private Vault
- All captured snapshots and compiled videos are saved exclusively inside app-private internal storage (`context.filesDir/vault/`).
- Protected by a `.nomedia` sentinel file, ensuring files remain completely invisible to public Android gallery apps, Google Photos, and third-party media indexers.
- In-app vault features include:
  - High-resolution asynchronous thumbnail caching.
  - Fullscreen zoomable image viewer.
  - Explicit export to the public Android Pictures/Movies directory.
  - One-tap shredding to securely delete all vault contents.

---

## Architecture & Tech Stack

- **UI Framework**: Android Jetpack Compose with Material 3.
- **Design System**: Pastel Neo-Brutalist Minimalism.
  - Background: `#F6F0DF` (Warm Cream).
  - Borders: 1.5dp solid `#111111` high-contrast outlines.
  - Color Accents: Mint Green (`#B4F0CE`), Peach (`#FFAE88`), Lavender (`#B9A4E8`), Powder Blue (`#A9C8E5`), Golden Yellow (`#E9B945`).
- **Core Services**:
  - `GestureAccessibilityService`: Hardware key listener, silent screenshot trigger, sequential capture engine.
  - `StealthTileService`: System Quick Settings Tile provider.
  - `FrameVideoEncoder`: Hardware `MediaCodec` H.264 video encoder and `MediaMuxer`.
  - `PrivateVaultManager`: Internal storage file management with `.nomedia` enforcement.
- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 8.0 (API 26)

---

## Building the Project

### Prerequisites
- JDK 17
- Android SDK Platform 34
- Gradle 8.7+

### Option 1: Command Line (PowerShell)
```powershell
# Set JAVA_HOME (if not already set)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Assemble Debug APK
.\gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Android Studio
1. Open Android Studio and select **Open**.
2. Navigate to and select the project root directory.
3. Allow Gradle to sync.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

---

## License
This project is released under the MIT License.
