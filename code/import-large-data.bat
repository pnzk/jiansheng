@echo off
chcp 65001 >nul
setlocal

set "NO_PAUSE=0"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

echo ========================================
echo   Gym Fitness - Large Data One-Click Import
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

echo [1/2] Generating realistic CSV data ^(10,000 users^)...
python database\generate_large_realistic_data.py --users 10000 --exercise-records 100000 --body-metrics 50000
if errorlevel 1 (
    echo [X] Failed to generate CSV data.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [2/2] Importing CSV data into MySQL ^(--reset-users^) ...
python database\import_cleaned_data.py --reset-users
if errorlevel 1 (
    echo [X] Failed to import data.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [OK] Large dataset import completed.
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 0
