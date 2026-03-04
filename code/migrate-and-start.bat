@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

set "MIGRATE_MODE_ARGS="
set "WITH_API_SMOKE=0"
set "NO_START=0"
set "NO_PAUSE=0"
set "SHOW_HELP=0"

:parse_args
if "%~1"=="" goto args_done

if /I "%~1"=="--large" (
    set "MIGRATE_MODE_ARGS=!MIGRATE_MODE_ARGS! --large"
    shift
    goto parse_args
)
if /I "%~1"=="--clean-first" (
    set "MIGRATE_MODE_ARGS=!MIGRATE_MODE_ARGS! --clean-first"
    shift
    goto parse_args
)
if /I "%~1"=="--with-api-smoke" (
    set "WITH_API_SMOKE=1"
    shift
    goto parse_args
)
if /I "%~1"=="--no-start" (
    set "NO_START=1"
    shift
    goto parse_args
)
if /I "%~1"=="--db-only" (
    set "NO_START=1"
    shift
    goto parse_args
)
if /I "%~1"=="--no-pause" (
    set "NO_PAUSE=1"
    shift
    goto parse_args
)
if /I "%~1"=="--help" (
    set "SHOW_HELP=1"
    shift
    goto parse_args
)
if /I "%~1"=="-h" (
    set "SHOW_HELP=1"
    shift
    goto parse_args
)
if /I "%~1"=="-?" (
    set "SHOW_HELP=1"
    shift
    goto parse_args
)

echo [WARN] Unknown argument: %~1
shift
goto parse_args

:args_done
if "%SHOW_HELP%"=="1" goto usage

if "%WITH_API_SMOKE%"=="1" (
    set "MIGRATE_MODE_ARGS=!MIGRATE_MODE_ARGS! --with-api-smoke"
)
if "%NO_PAUSE%"=="1" (
    set "MIGRATE_MODE_ARGS=!MIGRATE_MODE_ARGS! --no-pause"
)

if not defined MYSQL_HOST set "MYSQL_HOST=localhost"
if not defined MYSQL_PORT set "MYSQL_PORT=3306"
if not defined MYSQL_USER set "MYSQL_USER=root"
if not defined MYSQL_PASSWORD set "MYSQL_PASSWORD=123456"
if not defined MYSQL_DB set "MYSQL_DB=gym_fitness_analytics"

echo ========================================
echo   Gym Fitness - One Click Migrate+Start
echo ========================================
echo.
echo Project root: %PROJECT_ROOT%
echo Migration args: !MIGRATE_MODE_ARGS!
echo DB target: %MYSQL_USER%@%MYSQL_HOST%:%MYSQL_PORT%/%MYSQL_DB%
if "%NO_START%"=="1" (
    echo Start service: OFF ^(--db-only / --no-start^)
) else (
    echo Start service: ON
)
echo.

if not exist "%PROJECT_ROOT%\migrate-scheme-b.bat" (
    echo [X] Script not found: migrate-scheme-b.bat
    goto failed
)

if not exist "%PROJECT_ROOT%\start.bat" (
    echo [X] Script not found: start.bat
    goto failed
)

echo [1/2] Running Scheme-B DB migration...
call "%PROJECT_ROOT%\migrate-scheme-b.bat" !MIGRATE_MODE_ARGS!
if errorlevel 1 (
    echo [X] Migration failed.
    goto failed
)
echo [OK] Migration completed.
echo.

if "%NO_START%"=="1" (
    echo [2/2] Start step skipped.
    goto success
)

echo [2/2] Starting backend and frontend...
call "%PROJECT_ROOT%\start.bat" --no-pause
if errorlevel 1 (
    echo [X] Service startup failed.
    goto failed
)

goto success

:usage
echo Usage:
echo   migrate-and-start.bat [options]
echo.
echo Options:
echo   --db-only         Rebuild DB only ^(same as --no-start^)
echo   --no-start        Skip starting backend/frontend
echo   --large           Use large synthetic dataset mode
echo   --clean-first     Clean and regenerate dataset before import
echo   --with-api-smoke  Run API smoke check during migration
echo   --no-pause        Exit without waiting for key press
echo   --help, -h, -?    Show this help
echo.
echo Examples:
echo   migrate-and-start.bat --db-only
echo   migrate-and-start.bat --clean-first --with-api-smoke
echo   migrate-and-start.bat --large
exit /b 0

:success
echo.
echo ========================================
echo [OK] One-click migrate+start completed.
echo ========================================
echo.
if "%NO_START%"=="1" (
    echo Next:
    echo   To start services manually, run start-all.bat
) else (
    echo Frontend: http://localhost:3000
    echo Backend : http://localhost:8080
)
echo.
echo Default generated account:
echo   admin : admin_auto_001 / 123456
echo   coach : coach_auto_001 / 123456
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 0

:failed
echo.
echo ========================================
echo [FAILED] One-click migrate+start failed.
echo ========================================
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 1

