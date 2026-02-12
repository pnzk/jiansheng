@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo   Seed All DB Artifacts (One-click)
echo ========================================
echo.

cd /d "%~dp0"

set "NO_PAUSE=0"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

python --version >nul 2>&1
if errorlevel 1 (
    echo [X] Python not found. Please install Python 3.8+
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

python -c "import pymysql, bcrypt" >nul 2>&1
if errorlevel 1 (
    echo Installing required Python packages...
    pip install pymysql bcrypt -q
)

set "STUDENTS_PER_COACH=30"
set "ADMIN_COUNT=1"
set "ACCOUNT_PASSWORD=123456"

echo Config:
echo   students_per_coach = %STUDENTS_PER_COACH%
echo   admin_count        = %ADMIN_COUNT%
echo   account_password   = %ACCOUNT_PASSWORD%
echo.

echo [1/6] Seed coaches/admin + assign students + active plans...
python database\assign_coaches_and_seed_plans.py --students-per-coach %STUDENTS_PER_COACH% --admin-count %ADMIN_COUNT% --password %ACCOUNT_PASSWORD%
if errorlevel 1 (
    echo [X] Step 1 failed.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [2/6] Ensure recent student activity/metrics + refresh leaderboards...
python database\ensure_recent_student_activity.py
if errorlevel 1 (
    echo [X] Step 2 failed.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [3/6] Rebuild user achievements from real records...
python database\seed_user_achievements.py --reset
if errorlevel 1 (
    echo [X] Step 3 failed.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [4/6] Backfill exercise created_at to match exercise_date...
python database\backfill_exercise_created_at.py
if errorlevel 1 (
    echo [X] Step 4 failed.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [5/6] Rebuild analytics result tables (equipment_usage + user_behavior_analysis)...
python database\seed_analytics_tables.py --reset
if errorlevel 1 (
    echo [X] Step 5 failed.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo [6/6] API smoke check (admin/coach/student core endpoints)...
python scripts\api_smoke_check.py
if errorlevel 1 (
    echo [X] Step 6 failed: API smoke check failed.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo.
echo ========================================
echo [OK] All DB artifacts rebuilt successfully.
echo ========================================
echo.

if "%NO_PAUSE%"=="0" pause
exit /b 0
