@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"
echo [INFO] stop-all.bat is now an alias of stop.bat
call "%~dp0stop.bat" %*
exit /b %errorlevel%
