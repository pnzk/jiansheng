@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"
set "DELIVERY_ROOT=%PROJECT_ROOT%\delivery"
set "PACKAGE_DIR=%DELIVERY_ROOT%\gym-fitness-delivery"
set "ZIP_PATH=%DELIVERY_ROOT%\gym-fitness-delivery.zip"

echo ========================================
echo   Gym Fitness - Package Delivery
echo ========================================
echo.

if exist "%PACKAGE_DIR%" (
  echo [INFO] Cleaning old package directory...
  rmdir /s /q "%PACKAGE_DIR%"
)

if exist "%ZIP_PATH%" (
  echo [INFO] Removing old zip package...
  del /f /q "%ZIP_PATH%"
)

if not exist "%DELIVERY_ROOT%" mkdir "%DELIVERY_ROOT%"
mkdir "%PACKAGE_DIR%"

echo [1/5] Copy backend...
xcopy "%PROJECT_ROOT%\backend" "%PACKAGE_DIR%\backend\" /E /I /Y >nul
if errorlevel 1 goto failed

echo [2/5] Copy frontend...
xcopy "%PROJECT_ROOT%\frontend" "%PACKAGE_DIR%\frontend\" /E /I /Y >nul
if errorlevel 1 goto failed

echo [3/5] Copy database and scripts...
xcopy "%PROJECT_ROOT%\database" "%PACKAGE_DIR%\database\" /E /I /Y >nul
if errorlevel 1 goto failed
xcopy "%PROJECT_ROOT%\scripts" "%PACKAGE_DIR%\scripts\" /E /I /Y >nul
if errorlevel 1 goto failed

echo [4/5] Copy root bat files and docs...
for %%F in (
  README.md
  RUN_GUIDE.md
  RUN_DEMO.md
  DELIVERY_DEPLOYMENT.md
  start-all.bat
  quick-start.bat
  start.bat
  stop.bat
  stop-all.bat
  install-deps.bat
  check-env.bat
  init-database.bat
  import-data.bat
  import-large-data.bat
  migrate-and-start.bat
  migrate-scheme-b.bat
  seed-all-db-artifacts.bat
  auth-smoke-test.bat
  crud-smoke-test.bat
) do (
  if exist "%PROJECT_ROOT%\%%F" copy /Y "%PROJECT_ROOT%\%%F" "%PACKAGE_DIR%\" >nul
)

echo [5/5] Create zip package...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%PACKAGE_DIR%\*' -DestinationPath '%ZIP_PATH%' -Force"
if errorlevel 1 goto failed

echo.
echo ========================================
echo [OK] Delivery package created.
echo Folder: %PACKAGE_DIR%
echo Zip   : %ZIP_PATH%
echo ========================================
echo.
exit /b 0

:failed
echo.
echo ========================================
echo [FAILED] Packaging failed.
echo ========================================
echo.
exit /b 1
