@echo off
chcp 65001 >nul
setlocal

set "NO_PAUSE=0"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

echo ========================================
echo    Gym Fitness - Stop Services
echo ========================================
echo.

echo Stopping backend processes (java.exe)...
taskkill /F /IM java.exe /T >nul 2>&1
if errorlevel 1 (
    echo   [WARN] No running Java process found.
) else (
    echo   [OK] Java processes stopped.
)

echo Stopping frontend processes (node.exe)...
taskkill /F /IM node.exe /T >nul 2>&1
if errorlevel 1 (
    echo   [WARN] No running Node process found.
) else (
    echo   [OK] Node processes stopped.
)

echo.
echo ========================================
echo [OK] Stop workflow completed.
echo ========================================
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 0
