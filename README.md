# SudokuDroid 🧩

A modern, user-friendly Sudoku game designed for accessibility and simplicity.

## ✨ Features
- **Multiple Difficulties**: Choose from Easy, Medium, and Hard.
- **Theme Support**: Toggle between Light and Dark modes for comfortable play.
- **Multilingual**: Supports both Turkish (Default) and English languages.
- **Game Timer**: Track your solving time with built-in Pause and Resume functionality.
- **Smart Feedback**: Immediate visual feedback for incorrect entries and a victory dialog upon completion.
- **Accessible UI**: High contrast colors, large touch targets, and clear visual cues.

## 🛠️ Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Build System**: Gradle
- **Target SDK**: Android 14 (API 34)

## 🚀 Installation & Build

### Prerequisites
- Android SDK
- OpenJDK 17
- A connected Android device or emulator with USB Debugging enabled.

### Building the App
1. Clone the repository.
2. Ensure your environment variables (`JAVA_HOME`, `ANDROID_HOME`) are set.
3. Run the build command from the root directory:
   ```bash
   ./gradlew assembleDebug
   ```

### Installing to Device
Once the build is successful, install the APK using ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📂 Project Structure
- `app/src/main/java/com/sudokudroid/game/MainActivity.kt`: Main UI and game logic integration.
- `app/src/main/java/com/sudokudroid/game/SudokuGameLogic.kt`: Sudoku board generation and validation engine.
- `app/src/main/res/drawable/ic_launcher.xml`: Custom vector app icon.
