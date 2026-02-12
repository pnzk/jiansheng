@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   Gym Fitness - One Click Migrate+Start
echo ========================================
echo.

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

set "MIGRATE_MODE_ARGS="
set "WITH_API_SMOKE=0"
set "NO_START=0"
set "NO_PAUSE=0"

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
if /I "%~1"=="--no-pause" (
    set "NO_PAUSE=1"
    shift
    goto parse_args
)

echo [WARN] Unknown argument: %~1
shift
goto parse_args

:args_done
if "%WITH_API_SMOKE%"=="1" (
    set "MIGRATE_MODE_ARGS=!MIGRATE_MODE_ARGS! --with-api-smoke"
)
if "%NO_PAUSE%"=="1" (
    set "MIGRATE_MODE_ARGS=!MIGRATE_MODE_ARGS! --no-pause"
)

echo Migration args: !MIGRATE_MODE_ARGS!
echo.

if not exist "%PROJECT_ROOT%\migrate-scheme-b.bat" (
    echo [X] Script not found: migrate-scheme-b.bat
    goto :failed
)

if not exist "%PROJECT_ROOT%\start.bat" (
    echo [X] Script not found: start.bat
    goto :failed
)

echo [1/2] Running Scheme-B DB migration...
call "%PROJECT_ROOT%\migrate-scheme-b.bat" !MIGRATE_MODE_ARGS!
if errorlevel 1 (
    echo [X] Migration failed.
    goto :failed
)
echo [OK] Migration completed.
echo.

if "%NO_START%"=="1" (
    echo [2/2] Start step skipped by --no-start.
    goto :success
)

echo [2/2] Starting backend and frontend...
call "%PROJECT_ROOT%\start.bat" --no-pause
if errorlevel 1 (
    echo [X] Service startup failed.
    goto :failed
)

goto :success

:success
echo.
echo ========================================
echo [OK] One-click migrate+start completed.
echo ========================================
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
