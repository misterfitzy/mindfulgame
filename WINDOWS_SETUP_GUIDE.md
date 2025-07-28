# Windows Setup Guide for Mindful Game

This guide will help you set up and build the Mindful Game MVP on Windows with Android Studio.

## 🛠️ Prerequisites

### 1. Install Java Development Kit (JDK)
- Download **JDK 17** or **JDK 11** from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
- Install and ensure `JAVA_HOME` is set in your environment variables
- Verify installation: Open Command Prompt and run `java -version`

### 2. Install Android Studio
- Download from [Android Studio](https://developer.android.com/studio)
- Run the installer and follow the setup wizard
- Install the Android SDK when prompted
- **Important**: Remember the SDK location (usually `C:\Users\YourName\AppData\Local\Android\Sdk`)

## 🔧 Environment Setup

### 1. Set Environment Variables
Open **System Properties** → **Advanced** → **Environment Variables** and add:

**System Variables:**
- `ANDROID_HOME` = `C:\Users\YourName\AppData\Local\Android\Sdk` (your SDK path)
- `JAVA_HOME` = `C:\Program Files\Java\jdk-17` (your JDK path)

**Path Variable (add these entries):**
- `%ANDROID_HOME%\platform-tools`
- `%ANDROID_HOME%\tools`
- `%JAVA_HOME%\bin`

### 2. Verify Setup
Open **Command Prompt** and run:
```cmd
java -version
adb version
```

Both commands should work without errors.

## 📱 Device Setup

### Option 1: Physical Android Device
1. **Enable Developer Options:**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"

2. **Connect Device:**
   - Connect via USB cable
   - Allow USB debugging when prompted
   - Verify: Run `adb devices` in Command Prompt

### Option 2: Android Emulator
1. **Create Virtual Device:**
   - Open Android Studio
   - Go to Tools → AVD Manager
   - Click "Create Virtual Device"
   - Choose a device (Pixel 6 recommended)
   - Select API level 24 or higher
   - Click Finish

2. **Start Emulator:**
   - Click the play button next to your virtual device
   - Wait for it to fully boot

## 🚀 Building the Project

### Method 1: Using the Windows Build Script (Recommended)

1. **Open Command Prompt** in the project directory
2. **Run commands:**

```cmd
# Open project in Android Studio
build.bat studio

# Or build and run directly
build.bat run

# Other useful commands
build.bat build      # Build debug APK
build.bat install    # Build and install
build.bat logs       # View app logs
build.bat device     # Show device info
```

### Method 2: Using Android Studio GUI

1. **Open Project:**
   - Launch Android Studio
   - Click "Open an Existing Project"
   - Navigate to the `MindfulGame` folder
   - Click OK

2. **Sync Project:**
   - Wait for Gradle sync to complete
   - If prompted, click "Sync Now"

3. **Build and Run:**
   - Connect device or start emulator
   - Click the green "Run" button (▶️)
   - Select your device
   - Wait for installation and launch

### Method 3: Command Line with Gradle

1. **Open Command Prompt** in project directory
2. **Run commands:**

```cmd
# Build debug APK
gradlew.bat assembleDebug

# Install on connected device
gradlew.bat installDebug

# Build and install
gradlew.bat installDebug && adb shell am start -n com.mindful.game/.MainActivity
```

## 🎮 Using the Game

Once installed:

1. **Launch the app** on your device
2. **Initial State**: You'll see white and black rectangles with a straight boundary
3. **Watch**: The boundary becomes noisy over time
4. **Interact**: Touch and drag to restore the boundary
5. **Progress**: Watch the progress bar to see restoration completion
6. **Controls**: Tap screen to hide/show controls
7. **Settings**: Adjust touch sensitivity and rendering quality

## 🔧 Troubleshooting

### Build Errors

**"ANDROID_HOME not set"**
- Verify environment variables are set correctly
- Restart Command Prompt after setting variables

**"Java version incompatible"**
- Ensure you're using JDK 11 or 17
- Update `JAVA_HOME` environment variable

**"SDK not found"**
- Check Android Studio SDK path: File → Settings → Android SDK
- Update `ANDROID_HOME` to match this path

### Device Connection Issues

**"No devices found"**
- For physical device: Check USB debugging is enabled
- For emulator: Ensure it's fully booted and running
- Run `adb devices` to verify connection

**"Installation failed"**
- Try `adb kill-server && adb start-server`
- Restart device/emulator
- Check available storage space

### Performance Issues

**Slow performance:**
- Lower rendering quality in app settings
- Use a newer device/emulator (API 28+)
- Close other apps to free memory

**Build is slow:**
- Add to `gradle.properties`: `org.gradle.parallel=true`
- Increase Gradle memory: `org.gradle.jvmargs=-Xmx4g`

## 📋 Development Workflow

### Daily Development
```cmd
# Start development session
build.bat studio

# Or quick test cycle
build.bat run
build.bat logs    # In another terminal for logs
```

### Making Changes
1. Edit code in Android Studio
2. Press Ctrl+F9 to build
3. Run app to test changes
4. Use `build.bat logs` to debug

### Project Structure
- **Core interfaces**: `app/src/main/java/com/mindful/game/core/interfaces/`
- **Physics engine**: `app/src/main/java/com/mindful/game/physics/`
- **Rendering**: `app/src/main/java/com/mindful/game/rendering/`
- **UI**: `app/src/main/java/com/mindful/game/ui/`

## 🆘 Getting Help

### Common Commands Reference
```cmd
# Project management
build.bat studio     # Open Android Studio
build.bat clean      # Clean build files
build.bat build      # Build debug APK

# Device management
build.bat device     # Show device info
adb devices          # List connected devices
adb logcat           # View all device logs

# Installation
build.bat install    # Build and install
build.bat run        # Build, install, and launch
```

### Useful Android Studio Shortcuts
- `Ctrl+F9` - Build project
- `Shift+F10` - Run app
- `Ctrl+Shift+F10` - Run with debugger
- `Alt+6` - Open logcat window
- `Ctrl+Alt+Y` - Sync project with Gradle

### Log Analysis
- Use `build.bat logs` to filter for app-specific logs
- In Android Studio: View → Tool Windows → Logcat
- Filter by package name: `com.mindful.game`

If you encounter any issues not covered here, check the main README.md or refer to the Android Studio documentation.
