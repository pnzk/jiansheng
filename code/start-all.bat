@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"
echo [INFO] Redirecting to migrate-and-start.bat
call "%~dp0migrate-and-start.bat" --no-pause %*
exit /b %errorlevel%
