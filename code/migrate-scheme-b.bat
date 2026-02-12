@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   Gym Fitness - Scheme B DB Migration
echo ========================================
echo.

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

set "MODE=cleaned"
set "CLEAN_FIRST=0"
set "WITH_API_SMOKE=0"
set "NO_PAUSE=0"

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--large" (
    set "MODE=large"
    shift
    goto parse_args
)
if /I "%~1"=="--clean-first" (
    set "CLEAN_FIRST=1"
    shift
    goto parse_args
)
if /I "%~1"=="--with-api-smoke" (
    set "WITH_API_SMOKE=1"
    shift
    goto parse_args
)
if /I "%~1"=="--no-pause" (
    set "NO_PAUSE=1"
    shift
    goto parse_args
)
echo [WARN] Unknown argument: %~1
shift
goto parse_args

:args_done
if not defined MYSQL_HOST set "MYSQL_HOST=localhost"
if not defined MYSQL_PORT set "MYSQL_PORT=3306"
if not defined MYSQL_USER set "MYSQL_USER=root"
if not defined MYSQL_PASSWORD set "MYSQL_PASSWORD=123456"
if not defined MYSQL_DB set "MYSQL_DB=gym_fitness_analytics"

set "MYSQL_CMD=mysql"
call :resolve_mysql
if errorlevel 1 (
    echo [X] MySQL client not found.
    echo     Please install MySQL client or add mysql.exe to PATH.
    goto :failed
)

echo Config:
echo   mode             = %MODE%
echo   clean_first      = %CLEAN_FIRST%
echo   with_api_smoke   = %WITH_API_SMOKE%
echo   db               = %MYSQL_USER%@%MYSQL_HOST%:%MYSQL_PORT%/%MYSQL_DB%
echo   mysql_cli        = %MYSQL_CMD%
echo.

echo [1/5] Checking MySQL connection...
call "%MYSQL_CMD%" -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo [X] Cannot connect to MySQL.
    echo     Check MYSQL_HOST / MYSQL_PORT / MYSQL_USER / MYSQL_PASSWORD.
    goto :failed
)
echo [OK] MySQL connection passed.
echo.

echo [2/5] Initializing schema...
if not exist "%PROJECT_ROOT%\database\schema.sql" (
    echo [X] schema.sql not found: %PROJECT_ROOT%\database\schema.sql
    goto :failed
)

call "%MYSQL_CMD%" -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -e "CREATE DATABASE IF NOT EXISTS %MYSQL_DB% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >nul 2>&1
if errorlevel 1 (
    echo [X] Failed to create database %MYSQL_DB%.
    goto :failed
)

call "%MYSQL_CMD%" -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DB% -e "DROP PROCEDURE IF EXISTS update_leaderboards;" >nul 2>&1
call "%MYSQL_CMD%" -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DB% < "%PROJECT_ROOT%\database\schema.sql"
if errorlevel 1 (
    echo [X] Failed to import schema.sql.
    goto :failed
)
echo [OK] Schema initialized.
echo.

echo [3/5] Preparing Python dependencies...
python --version >nul 2>&1
if errorlevel 1 (
    echo [X] Python not found. Please install Python 3.8+.
    goto :failed
)

python -c "import pymysql, pandas, bcrypt" >nul 2>&1
if errorlevel 1 (
    echo Installing required Python packages...
    pip install pymysql pandas bcrypt -q
    if errorlevel 1 (
        echo [X] Failed to install Python dependencies.
        goto :failed
    )
)
echo [OK] Python dependencies ready.
echo.

echo [4/5] Importing business data...
if "%CLEAN_FIRST%"=="1" (
    echo   - Cleaning dataset folder to regenerate CSV...
    python database\clean_dataset_folder.py
    if errorlevel 1 (
        echo [X] Failed to clean dataset folder.
        goto :failed
    )
)

if /I "%MODE%"=="large" (
    echo   - Generating large realistic CSV data...
    python database\generate_large_realistic_data.py --users 10000 --exercise-records 100000 --body-metrics 50000
    if errorlevel 1 (
        echo [X] Failed to generate large data.
        goto :failed
    )

    echo   - Importing generated CSV into MySQL ^(--reset-users^)...
    python database\import_cleaned_data.py --reset-users
    if errorlevel 1 (
        echo [X] Failed to import large generated data.
        goto :failed
    )
) else (
    for %%F in (users.csv exercise_records.csv body_metrics.csv exercise_reference.csv) do (
        if not exist "%PROJECT_ROOT%\data-processing\cleaned\%%F" (
            echo [X] Missing cleaned CSV: data-processing\cleaned\%%F
            echo [Tip] Run migrate-scheme-b.bat --clean-first
            goto :failed
        )
    )

    echo   - Importing cleaned CSV into MySQL ^(--reset^)...
    python database\import_cleaned_data.py --reset
    if errorlevel 1 (
        echo [X] Failed to import cleaned data.
        goto :failed
    )
)
echo [OK] Business data imported.
echo.

echo [5/5] Rebuilding DB artifacts for all pages...
python database\assign_coaches_and_seed_plans.py --students-per-coach 30 --admin-count 1 --password 123456
if errorlevel 1 (
    echo [X] assign_coaches_and_seed_plans failed.
    goto :failed
)

python database\ensure_recent_student_activity.py
if errorlevel 1 (
    echo [X] ensure_recent_student_activity failed.
    goto :failed
)

python database\seed_user_achievements.py --reset
if errorlevel 1 (
    echo [X] seed_user_achievements failed.
    goto :failed
)

python database\backfill_exercise_created_at.py
if errorlevel 1 (
    echo [X] backfill_exercise_created_at failed.
    goto :failed
)

python database\seed_analytics_tables.py --reset
if errorlevel 1 (
    echo [X] seed_analytics_tables failed.
    goto :failed
)

if "%WITH_API_SMOKE%"=="1" (
    echo.
    echo [Extra] Running API smoke checks ^(requires backend at http://localhost:8080^)...
    python scripts\api_smoke_check.py
    if errorlevel 1 (
        echo [X] API smoke check failed.
        goto :failed
    )
)

echo.
echo ========================================
echo [OK] Scheme B migration completed.
echo Frontend pages can now read real DB data.
echo ========================================
echo.
echo Default generated login:
echo   admin : admin_auto_001 / 123456
echo   coach : coach_auto_001 / 123456
echo   student: imported students / 123456
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 0

:failed
echo.
echo ========================================
echo [FAILED] Scheme B migration failed.
echo ========================================
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 1

:resolve_mysql
where mysql >nul 2>&1
if not errorlevel 1 (
    set "MYSQL_CMD=mysql"
    exit /b 0
)

if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    set "MYSQL_CMD=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    exit /b 0
)

if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_CMD=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    exit /b 0
)

if exist "C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe" (
    set "MYSQL_CMD=C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe"
    exit /b 0
)

exit /b 1
