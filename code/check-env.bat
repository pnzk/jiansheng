@echo off
chcp 65001 >nul
setlocal

set "NO_PAUSE=0"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

echo ========================================
echo    Gym Fitness System - Env Check
echo ========================================
echo.

set "ALL_OK=1"

echo [Checking required tools]
echo.

echo Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo   [X] Java not found ^(need JDK 17+^)
    set "ALL_OK=0"
) else (
    echo   [OK] Java found
)

echo Checking Maven...
call mvn -version >nul 2>&1
if errorlevel 1 (
    echo   [X] Maven not found ^(need 3.6+^)
    set "ALL_OK=0"
) else (
    echo   [OK] Maven found
)

echo Checking Node.js...
node -v >nul 2>&1
if errorlevel 1 (
    echo   [X] Node.js not found ^(need 16+^)
    set "ALL_OK=0"
) else (
    echo   [OK] Node.js found
)

echo Checking npm...
call npm -v >nul 2>&1
if errorlevel 1 (
    echo   [X] npm not found
    set "ALL_OK=0"
) else (
    echo   [OK] npm found
)

echo Checking MySQL client...
mysql --version >nul 2>&1
if errorlevel 1 (
    echo   [X] MySQL client not found ^(need 8.0+^)
    set "ALL_OK=0"
) else (
    echo   [OK] MySQL client found
)

echo Checking MySQL service...
sc query mysql >nul 2>&1
if errorlevel 1 (
    sc query mysql80 >nul 2>&1
    if errorlevel 1 (
        echo   [WARN] mysql/mysql80 service not found by name
    ) else (
        echo   [OK] mysql80 service detected
    )
) else (
    echo   [OK] mysql service detected
)

echo.
echo ========================================
if "%ALL_OK%"=="1" (
    echo [OK] Environment check passed
    echo Next:
    echo   1. run start-all.bat ^(recommended^)
    echo   2. or run migrate-and-start.bat
) else (
    echo [X] Environment check failed. Install missing tools first.
    echo Download links:
    echo   - Java JDK: https://adoptium.net/
    echo   - Maven: https://maven.apache.org/download.cgi
    echo   - Node.js: https://nodejs.org/
    echo   - MySQL: https://dev.mysql.com/downloads/mysql/
)
echo ========================================
echo.
if "%NO_PAUSE%"=="0" pause
if "%ALL_OK%"=="1" (
    exit /b 0
) else (
    exit /b 1
)
