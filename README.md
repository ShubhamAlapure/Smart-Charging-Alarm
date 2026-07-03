# Smart Charging Alarm ⚡🔋

A premium, feature-rich Android application built with **Jetpack Compose** and **Kotlin** that monitors your battery level in real-time. It notifies you with custom alarms when your device reaches a specified battery percentage (e.g., 80% to prolong battery lifespan, or 100% to prevent overcharging).

---

## 📥 Download the App

You can download the pre-compiled APK directly to your Android device from the GitHub Releases page:

**[👉 Download Latest Smart Charging Alarm APK 👈](https://github.com/ShubhamAlapure/Smart-Charging-Alarm/releases/tag/latest)**

*Choose the **`Smart-Charging-Alarm-debug.apk`** from the Assets list on the Releases page.*

---

## 🚀 Key Features

- **Real-Time Battery Insights**: Displays current battery percentage, battery health, temperature, voltage, technology, and charging status (AC/USB/Wireless).
- **Customizable Alarm Thresholds**: Set your target percentage slider (e.g., 80% for preserving battery longevity, 100% for full charge).
- **Foreground Service**: Reliable background monitoring. The service stays active even when your screen is off or you are using other apps, complying with Android's battery optimization guidelines.
- **Auto-Start on Connection**: Automatically starts the monitoring service the moment you plug in your charger, and stops when unplugged.
- **Customizable Alarm Sounds**: Choose your favorite ringtone/notification sound, toggle vibration, and configure repeating alarms.
- **Modern Jetpack Compose UI**: Clean, responsive, and beautiful dark-themed interface with smooth animations and canvas-drawn battery graphics.

---

## 📲 How to Install (Sideloading)

Since this app is distributed as an APK outside of the Google Play Store, you need to sideload it:

1. **Download the APK**: Click the [Download Link](https://github.com/ShubhamAlapure/Smart-Charging-Alarm/releases/latest) above and select `Smart-Charging-Alarm-debug.apk`.
2. **Enable Unknown Sources**:
   - If prompted by your browser or file manager during the download/open process, tap **Settings** and toggle **Allow from this source**.
   - Alternatively, go to your phone's **Settings > Apps > Special app access > Install unknown apps**, select your file manager or browser, and enable it.
3. **Install**: Open the downloaded APK file and tap **Install**.
4. **Grant Permissions**:
   - Launch the app and grant the **Post Notifications** permission (required for Android 13+) so the background service can alert you.
   - For absolute reliability, turn off Battery Optimization for this app if prompted (allowing it to run unrestricted in the background).

---

## 🛠️ Development & Building from Source

If you want to modify or compile the app yourself, follow these steps:

### Prerequisites
- [Android Studio Koala](https://developer.android.com/studio) or newer.
- Android SDK 34 (Android 14) and Build Tools.
- JDK 17.

### Building Locally

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/ShubhamAlapure/Smart-Charging-Alarm.git
   cd Smart-Charging-Alarm
   ```

2. **Open in Android Studio**:
   - Open Android Studio, select **Open**, and navigate to the project directory.
   - Let Gradle sync completely.

3. **Build the APK**:
   - You can build the debug APK via the Gradle command line wrapper:
     ```bash
     ./gradlew assembleDebug
     ```
   - The generated APK will be available at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🛡️ License

This project is open-source and available under the [MIT License](LICENSE).
