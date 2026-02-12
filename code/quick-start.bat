@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"
echo [INFO] quick-start.bat is now an alias of start-all.bat
call "%~dp0start-all.bat" %*
exit /b %errorlevel%
