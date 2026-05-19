@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"
set "WORKSPACE_ROOT=%PROJECT_ROOT%\.."
for %%I in ("%WORKSPACE_ROOT%") do set "WORKSPACE_ROOT=%%~fI"

set "OUT_DIR=%PROJECT_ROOT%\delivery-all"
set "ALL_DIR=%OUT_DIR%\gym-fitness-full-package"
set "ALL_ZIP=%OUT_DIR%\gym-fitness-full-package.zip"

echo ========================================
echo   Build Full Delivery Package
echo ========================================
echo.

if exist "%ALL_DIR%" rmdir /s /q "%ALL_DIR%"
if exist "%ALL_ZIP%" del /f /q "%ALL_ZIP%"
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
mkdir "%ALL_DIR%"

echo [1/6] Copy source delivery package...
xcopy "%PROJECT_ROOT%\delivery\gym-fitness-delivery" "%ALL_DIR%\source-delivery\" /E /I /Y >nul
if errorlevel 1 goto failed

echo [2/6] Copy client runtime package...
xcopy "%PROJECT_ROOT%\delivery-client\gym-fitness-client-runtime" "%ALL_DIR%\client-runtime\" /E /I /Y >nul
if errorlevel 1 goto failed

echo [3/6] Copy datasets...
xcopy "%WORKSPACE_ROOT%\健身房运动数据集" "%ALL_DIR%\datasets\健身房运动数据集\" /E /I /Y >nul
if errorlevel 1 goto failed
xcopy "%PROJECT_ROOT%\csv" "%ALL_DIR%\datasets\code-csv\" /E /I /Y >nul
if errorlevel 1 goto failed

echo [4/6] Copy docs...
if exist "%WORKSPACE_ROOT%\doc" xcopy "%WORKSPACE_ROOT%\doc" "%ALL_DIR%\docs\doc\" /E /I /Y >nul
if exist "%WORKSPACE_ROOT%\output" xcopy "%WORKSPACE_ROOT%\output" "%ALL_DIR%\docs\output\" /E /I /Y >nul

echo [5/6] Copy top-level readme and guides...
copy /Y "%WORKSPACE_ROOT%\README.md" "%ALL_DIR%\" >nul
copy /Y "%PROJECT_ROOT%\README.md" "%ALL_DIR%\README_code.md" >nul
copy /Y "%PROJECT_ROOT%\RUN_GUIDE.md" "%ALL_DIR%\" >nul
copy /Y "%PROJECT_ROOT%\RUN_DEMO.md" "%ALL_DIR%\" >nul
copy /Y "%PROJECT_ROOT%\DELIVERY_DEPLOYMENT.md" "%ALL_DIR%\" >nul

echo [6/6] Create full zip...
timeout /t 2 /nobreak >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%ALL_DIR%\*' -DestinationPath '%ALL_ZIP%' -Force"
if errorlevel 1 goto failed

echo.
echo ========================================
echo [OK] Full delivery package created.
echo Folder: %ALL_DIR%
echo Zip   : %ALL_ZIP%
echo ========================================
echo.
exit /b 0

:failed
echo.
echo ========================================
echo [FAILED] Full delivery packaging failed.
echo ========================================
echo.
exit /b 1
