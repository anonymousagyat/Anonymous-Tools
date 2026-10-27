@echo off
echo ===================================================
echo   Anonymous-Tools: Building Android Debug APK
echo ===================================================
echo.
cd /d "%~dp0"

REM Try using gradlew if present, or gradle if installed on system PATH
if exist "gradlew.bat" (
    call gradlew.bat assembleDebug
) else (
    call gradle assembleDebug
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===================================================
    echo   BUILD SUCCESSFUL!
    echo   APK Generated at:
    echo   app\build\outputs\apk\debug\app-debug.apk
    echo ===================================================
    if exist "app\build\outputs\apk\debug" (
        explorer.exe "app\build\outputs\apk\debug"
    )
) else (
    echo.
    echo [ERROR] Build failed. Please verify Android SDK is configured or open in Android Studio.
)
pause
