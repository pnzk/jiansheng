@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"
set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"
set "BACKEND_DIR=%PROJECT_ROOT%\backend"
set "STATIC_DIR=%BACKEND_DIR%\src\main\resources\static"
set "CLIENT_DIR=%PROJECT_ROOT%\delivery-client"
set "CLIENT_PACKAGE_DIR=%CLIENT_DIR%\gym-fitness-client-runtime"
set "CLIENT_ZIP=%CLIENT_DIR%\gym-fitness-client-runtime.zip"

echo ========================================
echo   Build Client Runtime Package
echo ========================================
echo.

if exist "%CLIENT_PACKAGE_DIR%" rmdir /s /q "%CLIENT_PACKAGE_DIR%"
if exist "%CLIENT_ZIP%" del /f /q "%CLIENT_ZIP%"
if not exist "%CLIENT_DIR%" mkdir "%CLIENT_DIR%"

echo [1/6] Build frontend dist...
cd /d "%FRONTEND_DIR%"
call npm.cmd run build
if errorlevel 1 goto failed

echo [2/6] Refresh backend static resources...
cd /d "%PROJECT_ROOT%"
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"
xcopy "%FRONTEND_DIR%\dist\*" "%STATIC_DIR%\" /E /I /Y >nul
if errorlevel 1 goto failed

echo [3/6] Build backend jar...
cd /d "%BACKEND_DIR%"
call mvn clean package -DskipTests
if errorlevel 1 goto failed

echo [4/6] Prepare runtime package folder...
cd /d "%PROJECT_ROOT%"
mkdir "%CLIENT_PACKAGE_DIR%"
mkdir "%CLIENT_PACKAGE_DIR%\app"
mkdir "%CLIENT_PACKAGE_DIR%\database"

copy /Y "%BACKEND_DIR%\target\gym-fitness-analytics-1.0.0.jar" "%CLIENT_PACKAGE_DIR%\app\gym-fitness-analytics.jar" >nul
copy /Y "%PROJECT_ROOT%\DELIVERY_DEPLOYMENT.md" "%CLIENT_PACKAGE_DIR%\" >nul
copy /Y "%PROJECT_ROOT%\database\schema.sql" "%CLIENT_PACKAGE_DIR%\database\" >nul
copy /Y "%PROJECT_ROOT%\database\ensure_recent_student_activity.py" "%CLIENT_PACKAGE_DIR%\database\" >nul
copy /Y "%PROJECT_ROOT%\database\expand_exercise_reference.py" "%CLIENT_PACKAGE_DIR%\database\" >nul
copy /Y "%PROJECT_ROOT%\database\fix_demo_accounts.py" "%CLIENT_PACKAGE_DIR%\database\" >nul

echo [4.5/6] Export demo database snapshot...
mysqldump -hlocalhost -uroot -p123456 --default-character-set=utf8mb4 --single-transaction --set-gtid-purged=OFF gym_fitness_analytics > "%CLIENT_PACKAGE_DIR%\database\gym_fitness_analytics_demo.sql"
if errorlevel 1 goto failed

echo [5/6] Generate client runtime scripts...
(
echo @echo off
echo chcp 65001 ^>nul
echo setlocal
echo cd /d "%%~dp0"
echo echo Starting Gym Fitness Analytics runtime...
echo java -jar app\gym-fitness-analytics.jar
) > "%CLIENT_PACKAGE_DIR%\start-client-runtime.bat"

(
echo @echo off
echo chcp 65001 ^>nul
echo setlocal
echo echo Initializing database structure...
echo mysql -hlocalhost -uroot -p123456 ^< database\schema.sql
) > "%CLIENT_PACKAGE_DIR%\init-client-db.bat"

(
echo @echo off
echo chcp 65001 ^>nul
echo setlocal
echo echo Restoring demo database snapshot...
echo mysql -hlocalhost -uroot -p123456 gym_fitness_analytics ^< database\gym_fitness_analytics_demo.sql
) > "%CLIENT_PACKAGE_DIR%\restore-demo-db.bat"

echo [6/6] Create zip package...
timeout /t 2 /nobreak >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%CLIENT_PACKAGE_DIR%\*' -DestinationPath '%CLIENT_ZIP%' -Force"
if errorlevel 1 goto failed

echo.
echo ========================================
echo [OK] Client runtime package created.
echo Folder: %CLIENT_PACKAGE_DIR%
echo Zip   : %CLIENT_ZIP%
echo ========================================
echo.
exit /b 0

:failed
echo.
echo ========================================
echo [FAILED] Build client runtime failed.
echo ========================================
echo.
exit /b 1
