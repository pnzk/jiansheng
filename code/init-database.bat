@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    Gym Fitness System - Database Init
echo ========================================
echo.

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_USER=root"
set "DB_PASS=123456"
set "DB_NAME=gym_fitness_analytics"
set "SEED_TEST_DATA=0"
set "NO_PAUSE=0"

if /I "%~1"=="--with-test-data" (
    set "SEED_TEST_DATA=1"
)
if /I "%~1"=="--no-pause" (
    set "NO_PAUSE=1"
)

set "MYSQL_CMD=mysql"
call :resolve_mysql
if errorlevel 1 (
    echo [X] MySQL client not found.
    echo     Please install MySQL client or add mysql.exe to PATH.
    echo     Checked common path: C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)

echo Database config:
echo   Host : %DB_HOST%
echo   Port : %DB_PORT%
echo   User : %DB_USER%
echo   DB   : %DB_NAME%
echo   CLI  : %MYSQL_CMD%
echo.

echo [1/3] Checking database connection...
call "%MYSQL_CMD%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo [X] Cannot connect to MySQL.
    echo     Please check:
    echo       1^) MySQL service is running
    echo       2^) DB_USER/DB_PASS in this script are correct
    echo       3^) Host/Port are correct
    echo.
    echo     Current: %DB_USER% / %DB_PASS% @ %DB_HOST%:%DB_PORT%
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)
echo [OK] MySQL connection passed.
echo.

echo [2/3] Creating database if not exists...
call "%MYSQL_CMD%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >nul 2>&1
if errorlevel 1 (
    echo [X] Failed to create database %DB_NAME%.
    if "%NO_PAUSE%"=="0" pause
    exit /b 1
)
echo [OK] Database %DB_NAME% is ready.
echo.

echo [3/3] Initializing schema...
if exist "%PROJECT_ROOT%\database\schema.sql" (
    call "%MYSQL_CMD%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% -e "DROP PROCEDURE IF EXISTS update_leaderboards;" >nul 2>&1
    call "%MYSQL_CMD%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%PROJECT_ROOT%\database\schema.sql"
    if errorlevel 1 (
        echo [X] Failed to execute schema.sql.
        if "%NO_PAUSE%"=="0" pause
        exit /b 1
    )
    echo [OK] Schema initialized.
) else (
    echo [WARN] schema.sql not found at database\schema.sql, skipped.
)
echo.

echo [4/4] Data seeding policy...
if "%SEED_TEST_DATA%"=="1" (
    if not exist "%PROJECT_ROOT%\database\init_test_data.py" (
        echo [X] init_test_data.py not found. Test-data mode unavailable.
        echo [Tip] Keep clean mode or restore test data scripts first.
        if "%NO_PAUSE%"=="0" pause
        exit /b 1
    )

    echo [Info] Seeding test accounts and demo data enabled.
    set "SEED_SQL=%TEMP%\gym_seed_test_users_%RANDOM%%RANDOM%.sql"
    (
        echo USE %DB_NAME%;
        echo INSERT INTO users ^(username,password,email,real_name,user_role,show_in_leaderboard,allow_coach_view,created_at,updated_at^) VALUES
        echo ^('test_student','\$2b\$12\$VfYg.4yGMh.zliVxEAJUq.k7.MaLT5AF0XsCvkyPAVK8AZr/Xsmsi','student@test.com','Test Student','STUDENT',1,1,NOW^(^),NOW^(^)^),
        echo ^('test_coach','\$2b\$12\$VfYg.4yGMh.zliVxEAJUq.k7.MaLT5AF0XsCvkyPAVK8AZr/Xsmsi','coach@test.com','Test Coach','COACH',1,1,NOW^(^),NOW^(^)^),
        echo ^('test_admin','\$2b\$12\$VfYg.4yGMh.zliVxEAJUq.k7.MaLT5AF0XsCvkyPAVK8AZr/Xsmsi','admin@test.com','Test Admin','ADMIN',1,1,NOW^(^),NOW^(^)^)
        echo ON DUPLICATE KEY UPDATE
        echo password=VALUES^(password^),
        echo real_name=VALUES^(real_name^),
        echo user_role=VALUES^(user_role^),
        echo show_in_leaderboard=VALUES^(show_in_leaderboard^),
        echo allow_coach_view=VALUES^(allow_coach_view^),
        echo updated_at=NOW^(^);
        echo SET @coach_id = ^(SELECT id FROM users WHERE username='test_coach' LIMIT 1^);
        echo UPDATE users SET coach_id=@coach_id WHERE username='test_student';
    ) > "%SEED_SQL%"

    call "%MYSQL_CMD%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% < "%SEED_SQL%" >nul 2>&1
    set "SEED_RC=%ERRORLEVEL%"
    del /q "%SEED_SQL%" >nul 2>&1
    if not "%SEED_RC%"=="0" (
        echo [X] Failed to seed test accounts.
        if "%NO_PAUSE%"=="0" pause
        exit /b 1
    )
    echo [OK] Test accounts are ready. Password: test123
    echo.

    if exist "%PROJECT_ROOT%\database\init_test_data.py" (
        echo [Optional] Initializing test data...
        python "%PROJECT_ROOT%\database\init_test_data.py" >nul 2>&1
        if errorlevel 1 (
            echo [WARN] Test data init skipped/failed ^(Python or pymysql may be missing^).
        ) else (
            echo [OK] Test data initialized.
        )
        echo.
    )
) else (
    echo [OK] Clean mode: test accounts/data NOT imported.
    echo [Tip] Run init-database.bat --with-test-data if you need demo data.
    echo.
)

echo ========================================
echo [OK] Database initialization completed.
echo Next step: run start-all.bat ^(recommended^)
echo ========================================
echo.
if "%NO_PAUSE%"=="0" pause
exit /b 0

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
