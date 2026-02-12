@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"

python --version >nul 2>nul
if errorlevel 1 (
  echo [X] Python not found in PATH.
  exit /b 1
)

python scripts\auth_smoke_test.py
set "ERR=%ERRORLEVEL%"

if not "%ERR%"=="0" (
  echo.
  echo [X] Auth smoke test failed.
  exit /b %ERR%
)

echo.
echo [OK] Auth smoke test passed.
exit /b 0
