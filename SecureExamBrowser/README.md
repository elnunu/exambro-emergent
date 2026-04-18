# Secure Exam Browser for Moodle

A production-ready Android application that functions as a locked-down exam browser, similar to Safe Exam Browser (SEB), specifically designed for Moodle LMS access.

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Requirements](#requirements)
4. [Project Structure](#project-structure)
5. [Setup Instructions](#setup-instructions)
6. [Building the APK](#building-the-apk)
7. [Device Administrator Setup](#device-administrator-setup)
8. [Configuration](#configuration)
9. [Usage Guide](#usage-guide)
10. [Security Features](#security-features)
11. [Moodle Integration](#moodle-integration)
12. [Testing](#testing)
13. [Troubleshooting](#troubleshooting)
14. [License](#license)

---

## Overview

Secure Exam Browser is an enterprise-grade Android application that provides a secure, locked-down environment for online examinations through Moodle LMS. It implements comprehensive security measures to ensure exam integrity.

### Key Highlights

- **Full Kiosk Mode**: Locks the device to single-task mode during exams
- **Security Checks**: Detects rooted devices, VPNs, screen mirroring, and more
- **Moodle Compatible**: Works with Moodle's Safe Exam Browser plugin
- **Violation Logging**: Records all security violations with timestamps
- **Exit Password Protection**: Only invigilators can exit the exam

---

## Features

### Kiosk Mode & Screen Lock
- Full kiosk mode using Android Device Policy Manager
- App Pinning / Screen Pinning (Lock Task Mode)
- Override back, home, and recent apps buttons
- Prevent status bar pull-down
- Immersive fullscreen mode

### Secure WebView
- Hardened WebView with URL whitelisting
- Custom user-agent for SEB identification
- SSL certificate validation
- No caching of exam content
- JavaScript injection for Moodle detection

### System Restrictions (During Exam)
- Screenshot & screen recording blocked (FLAG_SECURE)
- Clipboard access disabled
- Split screen / multi-window mode blocked
- Picture-in-picture mode blocked
- External display detection and blocking
- Screen mirroring detection (Miracast, Chromecast)

### Security Checks
- Root detection (multiple methods)
- Developer options detection
- USB debugging detection
- VPN detection
- Mock location detection
- Emulator detection

### Exam Session Management
- Session start with exam token/config
- Exit only via invigilator password
- Auto-lock on violation attempts
- Violation logging with timestamps
- Boot persistence for active sessions

---

## Requirements

### Development Environment
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Gradle 8.2+
- Kotlin 1.9.21

### Target Devices
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- Works on non-rooted devices
- No special hardware requirements

---

## Project Structure

```
SecureExamBrowser/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/secureexam/browser/
│   │   │   │   ├── App.kt                    # Application class
│   │   │   │   ├── MainActivity.kt           # Main activity
│   │   │   │   ├── di/                       # Hilt DI modules
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   └── RepositoryModule.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/                # Room DB, SecurePreferences
│   │   │   │   │   ├── remote/               # Moodle API
│   │   │   │   │   └── repository/           # Repository implementations
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/                # Data models
│   │   │   │   │   ├── repository/           # Repository interfaces
│   │   │   │   │   └── usecase/              # Use cases
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── splash/               # Splash screen
│   │   │   │   │   ├── admin/                # Device admin setup
│   │   │   │   │   ├── config/               # Configuration screen
│   │   │   │   │   ├── security/             # Security check screen
│   │   │   │   │   └── exam/                 # Main exam activity
│   │   │   │   ├── security/
│   │   │   │   │   ├── SecurityChecker.kt    # Security validations
│   │   │   │   │   ├── RootDetector.kt       # Root detection
│   │   │   │   │   ├── ScreenMirrorDetector.kt
│   │   │   │   │   ├── KioskManager.kt       # Kiosk mode control
│   │   │   │   │   ├── KioskLockService.kt   # Foreground service
│   │   │   │   │   └── ViolationLogger.kt    # Violation logging
│   │   │   │   ├── webview/
│   │   │   │   │   ├── SecureWebView.kt      # Hardened WebView
│   │   │   │   │   ├── SecureWebViewClient.kt
│   │   │   │   │   └── SecureWebChromeClient.kt
│   │   │   │   └── receiver/
│   │   │   │       ├── DeviceAdminReceiver.kt
│   │   │   │       ├── BootReceiver.kt
│   │   │   │       └── ScreenStateReceiver.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/                   # UI layouts
│   │   │   │   ├── values/                   # Strings, colors, themes
│   │   │   │   ├── xml/                      # Device admin, network config
│   │   │   │   └── drawable/                 # Icons, backgrounds
│   │   │   └── AndroidManifest.xml
│   │   └── test/                             # Unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Setup Instructions

### Step 1: Clone or Copy the Project

Copy the entire `SecureExamBrowser` folder to your development machine.

### Step 2: Open in Android Studio

1. Open Android Studio
2. Select **File > Open**
3. Navigate to the `SecureExamBrowser` folder
4. Click **OK**

### Step 3: Sync Gradle

Android Studio will automatically start syncing Gradle. If it doesn't:

1. Click **File > Sync Project with Gradle Files**
2. Wait for the sync to complete

### Step 4: Configure SDK

Ensure you have the following installed via SDK Manager:

- Android SDK Platform 34
- Android SDK Platform 26 (minimum)
- Android SDK Build-Tools 34.0.0
- Google Play services (optional, for certain features)

### Step 5: Create local.properties

Create a `local.properties` file in the project root if it doesn't exist:

```properties
sdk.dir=/path/to/your/Android/sdk
```

---

## Building the APK

### Debug Build

```bash
# Navigate to project directory
cd SecureExamBrowser

# Build debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

1. **Generate a Keystore** (first time only):

```bash
keytool -genkey -v -keystore secure-exam-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias secure-exam-key
```

2. **Create `keystore.properties`** in project root:

```properties
storePassword=your_keystore_password
keyPassword=your_key_password
keyAlias=secure-exam-key
storeFile=../secure-exam-keystore.jks
```

3. **Update `app/build.gradle.kts`** to use release signing:

```kotlin
signingConfigs {
    create("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = java.util.Properties()
        keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
        
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... rest of release config
    }
}
```

4. **Build Release APK**:

```bash
./gradlew assembleRelease

# APK location: app/build/outputs/apk/release/app-release.apk
```

### Install on Device

```bash
# Debug
adb install app/build/outputs/apk/debug/app-debug.apk

# Release
adb install app/build/outputs/apk/release/app-release.apk
```

---

## Device Administrator Setup

For full kiosk mode functionality, the app must be set as a Device Administrator.

### Standard Device Admin (User Activation)

1. Launch the app on the device
2. On first launch, you'll be prompted to enable Device Admin
3. Click **Enable Device Admin**
4. Accept the permissions

### Device Owner Mode (Enhanced Security)

For maximum security (e.g., managed devices), set the app as Device Owner:

```bash
# Factory reset the device first, then:
adb shell dpm set-device-owner com.secureexam.browser/.receiver.DeviceAdminReceiver
```

**Note**: Device Owner provides the strongest kiosk mode but requires a factory-reset device.

---

## Configuration

### Teacher/Admin Configuration

1. Open the app
2. Select **Teacher Mode**
3. Enter the **Moodle Server URL** (e.g., `https://moodle.yourschool.edu`)
4. Set an **Exit Password** (minimum 6 characters)
5. Confirm the password
6. Click **Save Configuration**

### Student Entry

1. Open the app
2. Select **Student Mode**
3. Enter the **Exam Token** (provided by teacher)
4. Click **Start Exam**

### Importing SEB Config

The app supports Safe Exam Browser configuration files:

1. Click **Import .seb Config**
2. Select the `.seb` file
3. Configuration will be auto-populated

---

## Usage Guide

### Starting an Exam

1. Teacher configures the app with Moodle URL and exit password
2. Student launches the app and enters exam mode
3. Security checks run automatically
4. If all checks pass, the exam loads in kiosk mode

### During the Exam

- The device is locked to the exam app
- All other apps are inaccessible
- Screenshots are blocked
- All violation attempts are logged

### Ending the Exam

**Option 1: Student Submits Exam**
- When the student submits in Moodle, the app detects this and auto-exits kiosk mode

**Option 2: Invigilator Exit**
- Tap the **Exit** button in the header
- Enter the exit password
- Kiosk mode will be disabled

---

## Security Features

### Root Detection
The app uses multiple methods to detect rooted devices:
- Check for su binary in common paths
- Check for root management apps (Magisk, SuperSU, etc.)
- Check for dangerous system properties
- Check for writable system paths
- Check for test-keys builds
- Check for busybox

### Screenshot Prevention
- `FLAG_SECURE` window flag blocks screenshots
- Screen recording is also blocked
- Content is not visible in recent apps

### Kiosk Mode Implementation
- Uses `startLockTask()` for screen pinning
- Device Policy Manager for Device Owner/Admin
- Immersive fullscreen mode
- Back/Home/Recent buttons overridden

### Violation Logging
All the following are logged with timestamps:
- Screenshot attempts
- App switch attempts
- Navigation to blocked URLs
- Screen mirroring activation
- External display connection
- Volume/power button presses

---

## Moodle Integration

### Compatible Moodle Versions
- Moodle 3.9+
- Moodle 4.x

### SEB Plugin
For full integration, install the Moodle SEB plugin:
- `quizaccess_seb` - Safe Exam Browser access rule

### WebView User Agent
The app sends a custom user-agent to identify itself:
```
SecureExamBrowser/1.0 (Android; SEB)
```

### JavaScript Interface
The app injects JavaScript to communicate with Moodle:
```javascript
window.SafeExamBrowser = {
    version: '1.0',
    security: {
        configKey: '...',
        browserExamKey: '...'
    }
};
```

---

## Testing

### Running Unit Tests

```bash
./gradlew test
```

### Running Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist

- [ ] Security checks run on launch
- [ ] Root detection works (test on rooted device)
- [ ] Developer options detection works
- [ ] Kiosk mode activates correctly
- [ ] Screenshot blocking works
- [ ] WebView loads Moodle correctly
- [ ] URL whitelisting blocks external sites
- [ ] Exit password validation works
- [ ] Violations are logged
- [ ] Boot persistence restores session

---

## Troubleshooting

### App won't enter kiosk mode
- Ensure Device Admin is enabled
- Check if another app has lock task permissions
- Try running as Device Owner

### WebView doesn't load
- Check Moodle URL is correct (include https://)
- Verify network connectivity
- Check SSL certificate is valid

### Exit password doesn't work
- Password is case-sensitive
- Must be at least 6 characters
- Ensure you're entering the correct password set during configuration

### Security check fails
- **Root Detection**: Device may be rooted
- **Developer Options**: Disable in Settings > Developer Options
- **USB Debugging**: Disable in Developer Options
- **VPN**: Disconnect any VPN connections

### Violations not syncing to Moodle
- Ensure Moodle SEB plugin is installed
- Check network connectivity
- Verify Moodle API token

---

## Architecture

### Clean Architecture Layers

1. **Presentation Layer**: Activities, ViewModels (if needed), UI components
2. **Domain Layer**: Use cases, repository interfaces, models
3. **Data Layer**: Repository implementations, local storage, remote API

### Dependency Injection

Using Hilt for dependency injection:
- `AppModule`: Network, security components
- `DatabaseModule`: Room database, DAOs
- `RepositoryModule`: Repository bindings

### Security Module

- `SecurityChecker`: Orchestrates all security checks
- `RootDetector`: Multiple root detection methods
- `ScreenMirrorDetector`: Miracast/Chromecast detection
- `KioskManager`: Lock task mode management
- `ViolationLogger`: Centralized violation logging

---

## Customization

### Adding New Security Checks

1. Add check type to `SecurityCheckType` enum
2. Implement check in `SecurityChecker`
3. Add to `RunSecurityChecksUseCase`
4. Update UI in `SecurityCheckActivity`

### Modifying Violation Severity

Edit `Violation.kt`:
```kotlin
private fun getSeverityForType(type: ViolationType): Int {
    return when (type) {
        ViolationType.YOUR_NEW_TYPE -> 3 // 1-5 scale
        // ...
    }
}
```

### Custom Moodle API Endpoints

Update `MoodleApiService.kt` to add new endpoints for custom Moodle plugins.

---

## License

This project is provided as-is for educational and institutional use. 

For commercial deployment, ensure compliance with:
- Android Enterprise licensing
- Your institution's software policies
- Local data protection regulations (GDPR, etc.)

---

## Support

For issues and feature requests, please contact your system administrator or the development team.

---

## Changelog

### v1.0.0 (Initial Release)
- Full kiosk mode implementation
- Security checks (root, VPN, screen mirroring, etc.)
- Secure WebView with URL whitelisting
- Violation logging system
- Moodle SEB plugin compatibility
- Exit password protection
- Boot persistence
