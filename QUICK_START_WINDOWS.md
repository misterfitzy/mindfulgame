# Quick Start Guide - Windows

Get the Mindful Game running on Windows in under 10 minutes!

## ⚡ Super Quick Setup

### 1. Prerequisites Check
Open **Command Prompt** and verify you have:
```cmd
java -version
```
If this fails, [download JDK 17](https://www.oracle.com/java/technologies/downloads/) and install.

### 2. Open Android Studio
Double-click the `build.bat` file in the project folder, or:
```cmd
build.bat studio
```

### 3. First Time Setup in Android Studio
When Android Studio opens:
1. **Wait for Gradle sync** to complete (progress bar at bottom)
2. **If prompted about SDK**: Click "Install missing SDK(s)" 
3. **Set environment variables** (if not done):
   - Note the SDK path shown in error messages
   - Add `ANDROID_HOME` environment variable with that path

### 4. Setup Device

**Option A - Use Phone:**
1. Connect your Android phone via USB
2. Enable **Developer Options**: Settings → About → Tap "Build Number" 7 times
3. Enable **USB Debugging**: Settings → Developer Options → USB Debugging
4. Allow debugging when prompted

**Option B - Use Emulator:**
1. In Android Studio: Tools → AVD Manager
2. Create Virtual Device → Pick Pixel 6 → Download system image → Finish
3. Click ▶️ to start emulator

### 5. Run the Game
In Android Studio, click the green **▶️ Run** button, or:
```cmd
build.bat run
```

## 🎮 You're Done!

The game should now be running on your device. You'll see:
- White rectangle on the left, black on the right
- A boundary that becomes wavy over time
- Touch and drag to restore the straight boundary
- Progress bar showing your restoration progress

## 🛠️ If Something Goes Wrong

### "ANDROID_HOME not set"
```cmd
# Check current environment variables
echo %ANDROID_HOME%

# If empty, set it (replace with your actual SDK path):
setx ANDROID_HOME "C:\Users\YourName\AppData\Local\Android\Sdk"

# Restart Command Prompt and try again
```

### "No devices found"
```cmd
# Check if device is connected
adb devices

# If empty:
# - For phone: Check USB debugging is enabled
# - For emulator: Make sure it's fully started
```

### "Build failed"
1. In Android Studio: Build → Clean Project
2. Then: Build → Rebuild Project
3. Or use: `build.bat clean` then `build.bat build`

### Need More Help?
- Check the detailed [Windows Setup Guide](WINDOWS_SETUP_GUIDE.md)
- Or run `build.bat help` for all available commands

## 🚀 Development Commands

Once everything works, use these for daily development:

```cmd
build.bat studio     # Open Android Studio
build.bat run        # Build and run on device
build.bat logs       # View app logs (useful for debugging)
build.bat clean      # Clean build files if issues
```

## 🎯 What's Next?

Now that it's running, try modifying the code:
1. Open `NoiseBasedEngine.kt` 
2. Change `maxNoiseIntensity = 0.15f` to `0.3f`
3. Rebuild and run to see more chaotic boundaries!

The modular design makes it easy to experiment with different physics engines and rendering approaches.
