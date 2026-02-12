@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    Gym Fitness System - One Click Start
echo ========================================
echo.

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

set "NO_PAUSE=0"
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

set "DB_HOST=localhost"
set "DB_USER=root"
set "DB_PASS=123456"
set "DB_NAME=gym_fitness_analytics"

echo Project root: %PROJECT_ROOT%
echo.

echo [1/5] Check database connection...
mysql -h%DB_HOST% -u%DB_USER% -p%DB_PASS% -e "USE %DB_NAME%; SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo [X] Database connection failed.
    echo     Please run init-database.bat first.
    pause
    exit /b 1
)
echo [OK] Database connection passed.
echo.

echo [2/5] Check and install dependencies...
if not exist "%PROJECT_ROOT%\backend\target" (
    echo     Backend not compiled, building...
    cd /d "%PROJECT_ROOT%\backend"
    call mvn clean install -DskipTests -q
    if errorlevel 1 (
        echo [X] Backend build failed. Please run install-deps.bat
        pause
        exit /b 1
    )
)

if not exist "%PROJECT_ROOT%\frontend\node_modules" (
    echo     Frontend dependencies missing, installing...
    cd /d "%PROJECT_ROOT%\frontend"
    call npm install
    if errorlevel 1 (
        echo     npm failed, retry with npm.cmd...
        call npm.cmd install
        if errorlevel 1 (
            echo [X] Frontend dependency install failed. Please run install-deps.bat
            pause
            exit /b 1
        )
    )
)
echo [OK] Dependency check passed.
echo.

echo [3/5] Auto clean port 8080 before backend start...
set "PORT_8080_FOUND=0"
set "KILLED_PIDS=;"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    if "!KILLED_PIDS:;%%P;=!"=="!KILLED_PIDS!" (
        set "PORT_8080_FOUND=1"
        echo     Found PID %%P on port 8080, stopping...
        taskkill /F /PID %%P >nul 2>&1
        if errorlevel 1 (
            echo [WARN] Failed to stop PID %%P. Try Administrator terminal.
        ) else (
            echo [OK] Stopped PID %%P
        )
        set "KILLED_PIDS=!KILLED_PIDS!%%P;"
    )
)
if "!PORT_8080_FOUND!"=="0" (
    echo [OK] Port 8080 is already free.
)
echo.

echo [4/5] Start backend service (port 8080)...
cd /d "%PROJECT_ROOT%\backend"
start "Backend - Spring Boot" cmd /k "title Backend - Spring Boot && call mvn spring-boot:run"
echo [OK] Backend start command sent.
echo.

echo Waiting for backend startup (10s)...
timeout /t 10 /nobreak >nul 2>&1
if errorlevel 1 ping 127.0.0.1 -n 11 >nul

echo [5/5] Auto clean port 3000 before frontend start...
set "PORT_3000_FOUND=0"
set "KILLED_PIDS=;"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTENING"') do (
    if "!KILLED_PIDS:;%%P;=!"=="!KILLED_PIDS!" (
        set "PORT_3000_FOUND=1"
        echo     Found PID %%P on port 3000, stopping...
        taskkill /F /PID %%P >nul 2>&1
        if errorlevel 1 (
            echo [WARN] Failed to stop PID %%P. Try Administrator terminal.
        ) else (
            echo [OK] Stopped PID %%P
        )
        set "KILLED_PIDS=!KILLED_PIDS!%%P;"
    )
)
if "!PORT_3000_FOUND!"=="0" (
    echo [OK] Port 3000 is already free.
)
echo.

echo [5/5] Start frontend service (port 3000)...
cd /d "%PROJECT_ROOT%\frontend"
start "Frontend - Vite" cmd /k "title Frontend - Vite && npm.cmd run dev"
echo [OK] Frontend start command sent.
echo.

echo Waiting for frontend startup (5s)...
timeout /t 5 /nobreak >nul 2>&1
if errorlevel 1 ping 127.0.0.1 -n 6 >nul

echo Opening browser...
start http://localhost:3000

echo.
echo ========================================
echo [OK] Startup workflow completed.
echo Frontend: http://localhost:3000
echo Backend : http://localhost:8080
echo ========================================
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 0
