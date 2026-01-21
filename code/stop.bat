@echo off
chcp 65001 >nul

echo ========================================
echo    健身管理系统 - 停止所有服务
echo ========================================
echo.

echo 正在停止后端服务 (Java)...
taskkill /F /IM java.exe /T >nul 2>&1
if errorlevel 1 (
    echo   ⚠ 未发现运行中的 Java 进程
) else (
    echo   ✓ 后端服务已停止
)

echo 正在停止前端服务 (Node)...
taskkill /F /IM node.exe /T >nul 2>&1
if errorlevel 1 (
    echo   ⚠ 未发现运行中的 Node 进程
) else (
    echo   ✓ 前端服务已停止
)

echo.
echo ========================================
echo ✓ 所有服务已停止
echo ========================================
echo.
pause
