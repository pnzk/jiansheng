@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo    健身管理系统 - 数据库初始化
echo ========================================
echo.

:: 获取脚本所在目录
cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

:: 数据库配置
set "DB_HOST=localhost"
set "DB_USER=root"
set "DB_PASS=123456"
set "DB_NAME=gym_fitness_analytics"

echo 数据库配置:
echo   主机: %DB_HOST%
echo   用户: %DB_USER%
echo   数据库: %DB_NAME%
echo.

:: 检测 MySQL 连接
echo [1/3] 检测数据库连接...
mysql -h%DB_HOST% -u%DB_USER% -p%DB_PASS% -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo ✗ 无法连接到 MySQL 服务器
    echo   请确保:
    echo   1. MySQL 服务已启动
    echo   2. 用户名和密码正确 (默认: root/123456)
    echo.
    echo   如需修改数据库配置，请编辑此脚本中的 DB_USER 和 DB_PASS
    pause
    exit /b 1
)
echo ✓ 数据库连接成功
echo.

:: 创建数据库
echo [2/3] 创建数据库...
mysql -h%DB_HOST% -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 (
    echo ✗ 创建数据库失败
    pause
    exit /b 1
)
echo ✓ 数据库 %DB_NAME% 已创建
echo.

:: 执行 schema.sql
echo [3/3] 初始化数据表...
if exist "%PROJECT_ROOT%\database\schema.sql" (
    mysql -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%PROJECT_ROOT%\database\schema.sql"
    if errorlevel 1 (
        echo ✗ 执行 schema.sql 失败
        pause
        exit /b 1
    )
    echo ✓ 数据表初始化完成
) else (
    echo ⚠ 未找到 database\schema.sql，跳过表结构初始化
)
echo.

:: 初始化测试数据
echo [可选] 初始化测试数据...
if exist "%PROJECT_ROOT%\database\init_test_data.py" (
    python "%PROJECT_ROOT%\database\init_test_data.py" 2>nul
    if errorlevel 1 (
        echo ⚠ 测试数据初始化失败（可能缺少 Python 或 pymysql）
        echo   可手动运行: python database\init_test_data.py
    ) else (
        echo ✓ 测试数据初始化完成
    )
) else (
    echo ⚠ 未找到测试数据脚本，跳过
)
echo.

echo ========================================
echo ✓ 数据库初始化完成！
echo.
echo 测试账号:
echo   学员: test_student / test123
echo   教练: test_coach / test123
echo   管理员: test_admin / test123
echo.
echo 下一步: 运行 start.bat 启动项目
echo ========================================
echo.
pause
