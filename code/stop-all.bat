@echo off
chcp 65001 >nul
echo ========================================
echo 健身管理系统 - 停止所有服务
echo ========================================
echo.

echo 正在停止后端服务 (端口 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
    if not errorlevel 1 (
        echo ✓ 后端服务已停止
    )
)

echo 正在停止前端服务 (端口 4173)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :4173 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
    if not errorlevel 1 (
        echo ✓ 前端服务已停止
    )
)

echo.
echo ========================================
echo ✓ 所有服务已停止
echo ========================================
echo.

pause
