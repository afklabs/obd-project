@echo off
echo === Android Auto OBD2 Debug Script for Windows ===
echo.

REM Check if Android SDK is in PATH
where adb >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ADB not found in PATH. Looking in common locations...
    
    REM Common Android SDK locations
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB_PATH="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
        echo Found ADB at: %ADB_PATH%
    ) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
        set ADB_PATH="%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
        echo Found ADB at: %ADB_PATH%
    ) else (
        echo ADB not found. Please install Android SDK or add platform-tools to PATH
        echo Common location: %LOCALAPPDATA%\Android\Sdk\platform-tools
        pause
        exit /b 1
    )
) else (
    set ADB_PATH=adb
    echo ADB found in PATH
)

echo.
echo 1. Building the app...
gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo Build failed! Please fix compilation errors first.
    pause
    exit /b 1
)

echo.
echo 2. Checking connected devices...
%ADB_PATH% devices

echo.
echo 3. Installing the app...
%ADB_PATH% install -r app\build\outputs\apk\debug\app-debug.apk

echo.
echo 4. Starting the app...
%ADB_PATH% shell am start -n com.example.androidautoobd2/.MainActivity

echo.
echo 5. Checking app logs (press Ctrl+C to stop)...
%ADB_PATH% logcat -s "OBDCarAppService:*" "OBDSession:*" "MainScreen:*" "AndroidAutoOBD2:*"

echo.
echo === Debug Complete ===
pause