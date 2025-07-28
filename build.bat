@echo off
setlocal enabledelayedexpansion

:: Build script for Mindful Game MVP on Windows
:: This script helps with common development tasks using Android Studio

echo 🎮 Mindful Game - Windows Build Script
echo ========================================

:: Function to check if Android SDK is available
:check_android_sdk
if "%ANDROID_HOME%"=="" (
    echo ❌ ANDROID_HOME not set. Please set up Android SDK.
    echo.
    echo To fix this:
    echo 1. Open Android Studio
    echo 2. Go to File ^> Settings ^> Appearance ^& Behavior ^> System Settings ^> Android SDK
    echo 3. Copy the SDK path
    echo 4. Set ANDROID_HOME environment variable to that path
    echo 5. Add %%ANDROID_HOME%%\platform-tools to your PATH
    pause
    exit /b 1
)

if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo ❌ Android SDK not found. Please install Android SDK.
    pause
    exit /b 1
)

echo ✅ Android SDK found at: %ANDROID_HOME%
goto :eof

:: Function to clean the project
:clean
echo 🧹 Cleaning project...
call gradlew.bat clean
if errorlevel 1 (
    echo ❌ Clean failed
    pause
    exit /b 1
)
echo ✅ Project cleaned
goto :eof

:: Function to build debug APK
:build_debug
echo 🔨 Building debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo ❌ Build failed
    pause
    exit /b 1
)
echo ✅ Debug APK built successfully
echo 📦 APK location: app\build\outputs\apk\debug\app-debug.apk
goto :eof

:: Function to build release APK
:build_release
echo 🔨 Building release APK...
call gradlew.bat assembleRelease
if errorlevel 1 (
    echo ❌ Release build failed
    pause
    exit /b 1
)
echo ✅ Release APK built successfully
echo 📦 APK location: app\build\outputs\apk\release\app-release.apk
goto :eof

:: Function to install debug APK on connected device
:install_debug
echo 📱 Installing debug APK on connected device...

:: Check if device is connected
adb devices | findstr "device" >nul
if errorlevel 1 (
    echo ❌ No Android device connected. Please:
    echo 1. Connect your Android device via USB
    echo 2. Enable Developer Options and USB Debugging
    echo 3. Or start an Android emulator
    pause
    exit /b 1
)

call gradlew.bat installDebug
if errorlevel 1 (
    echo ❌ Installation failed
    pause
    exit /b 1
)
echo ✅ App installed successfully
goto :eof

:: Function to run the app on connected device
:run_app
echo 🚀 Launching app on connected device...
adb shell am start -n com.mindful.game/.MainActivity
if errorlevel 1 (
    echo ❌ Failed to launch app
    pause
    exit /b 1
)
echo ✅ App launched
goto :eof

:: Function to view logs
:view_logs
echo 📋 Viewing app logs (Press Ctrl+C to stop)...
echo Starting logcat filter for com.mindful.game...
adb logcat | findstr "com.mindful.game"
goto :eof

:: Function to show device info
:device_info
echo 📱 Connected device information:
adb devices -l
echo.
for /f "delims=" %%i in ('adb shell getprop ro.build.version.release') do echo Android Version: %%i
for /f "delims=" %%i in ('adb shell getprop ro.product.model') do echo Device Model: %%i
for /f "delims=" %%i in ('adb shell getprop ro.product.manufacturer') do echo Manufacturer: %%i
goto :eof

:: Function to show help
:show_help
echo Usage: %0 [command]
echo.
echo Commands:
echo   clean          Clean the project
echo   build          Build debug APK
echo   release        Build release APK
echo   install        Build and install debug APK
echo   run            Install and run the app
echo   logs           View app logs
echo   device         Show connected device info
echo   studio         Open project in Android Studio
echo   help           Show this help message
echo.
echo Examples:
echo   %0 build       # Build debug APK
echo   %0 run         # Build, install, and run app
echo   %0 logs        # View live logs
echo   %0 studio      # Open in Android Studio
goto :eof

:: Function to open Android Studio
:open_studio
echo 🚀 Opening project in Android Studio...
echo Looking for Android Studio installation...

:: Common Android Studio installation paths
set "STUDIO_PATHS="
set "STUDIO_PATHS=%STUDIO_PATHS% "%ProgramFiles%\Android\Android Studio\bin\studio64.exe""
set "STUDIO_PATHS=%STUDIO_PATHS% "%ProgramFiles(x86)%\Android\Android Studio\bin\studio64.exe""
set "STUDIO_PATHS=%STUDIO_PATHS% "%LOCALAPPDATA%\Programs\Android Studio\bin\studio64.exe""

for %%i in (%STUDIO_PATHS%) do (
    if exist %%i (
        echo Found Android Studio at: %%i
        start "" %%i "%CD%"
        echo ✅ Android Studio opened
        goto :eof
    )
)

echo ❌ Android Studio not found in common locations.
echo Please open Android Studio manually and open this project folder:
echo %CD%
pause
goto :eof

:: Main script logic
set "command=%~1"
if "%command%"=="" set "command=help"

call :check_android_sdk

if "%command%"=="clean" (
    call :clean
) else if "%command%"=="build" (
    call :clean
    call :build_debug
) else if "%command%"=="release" (
    call :clean
    call :build_release
) else if "%command%"=="install" (
    call :clean
    call :build_debug
    call :install_debug
) else if "%command%"=="run" (
    call :clean
    call :build_debug
    call :install_debug
    call :run_app
) else if "%command%"=="logs" (
    call :view_logs
) else if "%command%"=="device" (
    call :device_info
) else if "%command%"=="studio" (
    call :open_studio
) else (
    call :show_help
)

echo.
echo Press any key to exit...
pause >nul
