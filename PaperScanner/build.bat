@echo off
REM Build script for PaperScanner Android app
REM Requires: JDK 17+, Android SDK, Gradle

echo Building PaperScanner...

REM Check Java version
java -version

REM Build debug APK
gradle assembleDebug --no-daemon

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Build failed!
    exit /b %ERRORLEVEL%
)
