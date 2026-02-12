@echo off
chcp 65001 >nul
setlocal

set "NO_PAUSE=0"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

echo ========================================
echo    Gym Fitness - Install Dependencies
echo ========================================
echo.

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

echo Project root: %PROJECT_ROOT%
echo.

echo [1/2] Installing backend Maven dependencies...
echo.
cd /d "%PROJECT_ROOT%\backend"
if not exist "pom.xml" (
    echo [X] backend\pom.xml not found.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

call mvn clean install -DskipTests -q
if errorlevel 1 (
    echo [X] Backend dependency install failed.
    echo   Check Maven config and network.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)
echo [OK] Backend dependencies installed.
echo.

echo [2/2] Installing frontend npm dependencies...
echo.
cd /d "%PROJECT_ROOT%\frontend"
if not exist "package.json" (
    echo [X] frontend\package.json not found.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

call npm install
if errorlevel 1 (
    echo [X] Frontend dependency install failed.
    echo   Check npm config and network.
    echo   Try mirror: npm config set registry https://registry.npmmirror.com
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)
echo [OK] Frontend dependencies installed.
echo.

echo ========================================
echo [OK] Dependency install completed.
echo.
echo Next:
echo   1. run start-all.bat ^(recommended^)
echo   2. or run migrate-and-start.bat
echo ========================================
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 0
