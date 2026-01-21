@echo off
chcp 65001 >nul
echo ========================================
echo 健身管理系统 - 安装并运行
echo ========================================
echo.

:: 获取脚本所在目录
cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

echo 项目根目录: %PROJECT_ROOT%
echo.

:: 安装后端依赖
echo [1/4] 安装后端依赖...
cd /d "%PROJECT_ROOT%\backend"
call mvn clean install -DskipTests -q
if errorlevel 1 (
    echo ✗ 后端依赖安装失败
    pause
    exit /b 1
)
echo ✓ 后端依赖安装完成
echo.

:: 安装前端依赖
echo [2/4] 安装前端依赖...
cd /d "%PROJECT_ROOT%\frontend"
call npm install
if errorlevel 1 (
    echo ✗ 前端依赖安装失败
    pause
    exit /b 1
)
echo ✓ 前端依赖安装完成
echo.

:: 初始化数据库
echo [3/4] 初始化数据库...
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS gym_fitness_analytics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if exist "%PROJECT_ROOT%\database\schema.sql" (
    mysql -u root -p123456 gym_fitness_analytics < "%PROJECT_ROOT%\database\schema.sql" 2>nul
)
echo ✓ 数据库初始化完成
echo.

:: 启动服务
echo [4/4] 启动服务...
call "%PROJECT_ROOT%\start-all.bat"
