@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"

python --version >nul 2>nul
if errorlevel 1 (
  echo [X] Python not found in PATH.
  exit /b 1
)

python -c "import requests,pymysql" >nul 2>nul
if errorlevel 1 (
  echo Installing required Python packages...
  pip install requests pymysql -q
  if errorlevel 1 (
    echo [X] Failed to install required Python packages.
    exit /b 1
  )
)

python scripts\api_crud_consistency_check.py
set "ERR=%ERRORLEVEL%"

if not "%ERR%"=="0" (
  echo.
  echo [X] CRUD consistency smoke check failed.
  exit /b %ERR%
)

echo.
echo [OK] CRUD consistency smoke check passed.
exit /b 0
