@echo off
chcp 65001 >nul
setlocal

set "NO_PAUSE=0"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

echo ========================================
echo    Gym Fitness System - Import Cleaned Data
echo ========================================
echo.

cd /d "%~dp0"

python --version >nul 2>&1
if errorlevel 1 (
    echo [X] Python not found. Please install Python 3.8+
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

python -c "import pymysql, pandas, bcrypt" >nul 2>&1
if errorlevel 1 (
    echo Installing required Python packages...
    pip install pymysql pandas bcrypt -q
    if errorlevel 1 (
        echo [X] Failed to install Python packages.
        if "%NO_PAUSE%"=="0" pause
        exit /b 1
    )
)

echo Running full re-import ^(--reset^) ...
python database\import_cleaned_data.py --reset
if errorlevel 1 (
    echo [X] Import failed.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [OK] Import completed.
if "%NO_PAUSE%"=="0" pause
exit /b 0
