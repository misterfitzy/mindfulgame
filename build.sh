#!/bin/bash

# Build script for Mindful Game MVP
# This script helps with common development tasks

set -e

echo "🎮 Mindful Game - Build Script"
echo "=============================="

# Function to check if Android SDK is available
check_android_sdk() {
    if [ -z "$ANDROID_HOME" ]; then
        echo "❌ ANDROID_HOME not set. Please set up Android SDK."
        exit 1
    fi
    
    if [ ! -f "$ANDROID_HOME/platform-tools/adb" ]; then
        echo "❌ Android SDK not found. Please install Android SDK."
        exit 1
    fi
    
    echo "✅ Android SDK found at: $ANDROID_HOME"
}

# Function to clean the project
clean() {
    echo "🧹 Cleaning project..."
    ./gradlew clean
    echo "✅ Project cleaned"
}

# Function to build debug APK
build_debug() {
    echo "🔨 Building debug APK..."
    ./gradlew assembleDebug
    echo "✅ Debug APK built successfully"
    echo "📦 APK location: app/build/outputs/apk/debug/app-debug.apk"
}

# Function to build release APK
build_release() {
    echo "🔨 Building release APK..."
    ./gradlew assembleRelease
    echo "✅ Release APK built successfully"
    echo "📦 APK location: app/build/outputs/apk/release/app-release.apk"
}

# Function to install debug APK on connected device
install_debug() {
    echo "📱 Installing debug APK on connected device..."
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        echo "❌ No Android device connected. Please connect a device or start an emulator."
        exit 1
    fi
    
    ./gradlew installDebug
    echo "✅ App installed successfully"
}

# Function to run the app on connected device
run_app() {
    echo "🚀 Launching app on connected device..."
    adb shell am start -n com.mindful.game/.MainActivity
    echo "✅ App launched"
}

# Function to view logs
view_logs() {
    echo "📋 Viewing app logs (Press Ctrl+C to stop)..."
    adb logcat | grep "com.mindful.game"
}

# Function to show device info
device_info() {
    echo "📱 Connected device information:"
    adb devices -l
    echo ""
    adb shell getprop ro.build.version.release | sed 's/^/Android Version: /'
    adb shell getprop ro.product.model | sed 's/^/Device Model: /'
    adb shell getprop ro.product.manufacturer | sed 's/^/Manufacturer: /'
}

# Function to show help
show_help() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  clean          Clean the project"
    echo "  build          Build debug APK"
    echo "  release        Build release APK"
    echo "  install        Build and install debug APK"
    echo "  run            Install and run the app"
    echo "  logs           View app logs"
    echo "  device         Show connected device info"
    echo "  help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build       # Build debug APK"
    echo "  $0 run         # Build, install, and run app"
    echo "  $0 logs        # View live logs"
}

# Main script logic
case "${1:-help}" in
    "clean")
        check_android_sdk
        clean
        ;;
    "build")
        check_android_sdk
        clean
        build_debug
        ;;
    "release")
        check_android_sdk
        clean
        build_release
        ;;
    "install")
        check_android_sdk
        clean
        build_debug
        install_debug
        ;;
    "run")
        check_android_sdk
        clean
        build_debug
        install_debug
        run_app
        ;;
    "logs")
        check_android_sdk
        view_logs
        ;;
    "device")
        check_android_sdk
        device_info
        ;;
    "help"|*)
        show_help
        ;;
esac
