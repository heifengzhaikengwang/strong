#!/bin/bash
# Build script for PaperScanner Android app
# Requires: JDK 17+, Android SDK, Gradle

echo "Building PaperScanner..."

# Check Java version
java -version

# Build debug APK
gradle assembleDebug --no-daemon

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "Build failed!"
    exit 1
fi
