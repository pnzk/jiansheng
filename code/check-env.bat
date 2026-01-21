@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    健身管理系统 - 环境检测工具
echo ========================================
echo.

set "MISSING="
set "ALL_OK=1"

echo [检测必需软件]
echo.

:: 检测 Java
echo 正在检测 Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo   ✗ Java 未安装
    set "MISSING=!MISSING!  - Java JDK 8+ (推荐 JDK 11 或 17)!LF!"
    set "ALL_OK=0"
) else (
    for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        echo   ✓ Java 已安装: %%i
    )
)

:: 检测 Maven
echo 正在检测 Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo   ✗ Maven 未安装
    set "MISSING=!MISSING!  - Apache Maven 3.6+!LF!"
    set "ALL_OK=0"
) else (
    for /f "tokens=3" %%i in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        echo   ✓ Maven 已安装: %%i
    )
)

:: 检测 Node.js
echo 正在检测 Node.js...
node -v >nul 2>&1
if errorlevel 1 (
    echo   ✗ Node.js 未安装
    set "MISSING=!MISSING!  - Node.js 16+ (推荐 LTS 版本)!LF!"
    set "ALL_OK=0"
) else (
    for /f %%i in ('node -v') do (
        echo   ✓ Node.js 已安装: %%i
    )
)

:: 检测 npm
echo 正在检测 npm...
npm -v >nul 2>&1
if errorlevel 1 (
    echo   ✗ npm 未安装
    set "MISSING=!MISSING!  - npm (随 Node.js 一起安装)!LF!"
    set "ALL_OK=0"
) else (
    for /f %%i in ('npm -v') do (
        echo   ✓ npm 已安装: %%i
    )
)

:: 检测 MySQL
echo 正在检测 MySQL...
mysql --version >nul 2>&1
if errorlevel 1 (
    echo   ✗ MySQL 客户端未安装
    set "MISSING=!MISSING!  - MySQL 8.0+!LF!"
    set "ALL_OK=0"
) else (
    for /f "tokens=1-5" %%i in ('mysql --version') do (
        echo   ✓ MySQL 已安装: %%l
    )
)

:: 检测 MySQL 服务是否运行
echo 正在检测 MySQL 服务...
sc query mysql >nul 2>&1
if errorlevel 1 (
    sc query mysql80 >nul 2>&1
    if errorlevel 1 (
        echo   ⚠ MySQL 服务可能未运行，请确保 MySQL 服务已启动
    ) else (
        echo   ✓ MySQL 服务运行中
    )
) else (
    echo   ✓ MySQL 服务运行中
)

echo.
echo ========================================

if "%ALL_OK%"=="1" (
    echo ✓ 所有必需软件已安装完成！
    echo.
    echo 下一步：
    echo   1. 运行 install-deps.bat 安装项目依赖
    echo   2. 运行 init-database.bat 初始化数据库
    echo   3. 运行 start.bat 启动项目
) else (
    echo ✗ 以下软件需要安装：
    echo.
    echo !MISSING!
    echo.
    echo 软件下载地址：
    echo   - Java JDK: https://adoptium.net/
    echo   - Maven: https://maven.apache.org/download.cgi
    echo   - Node.js: https://nodejs.org/
    echo   - MySQL: https://dev.mysql.com/downloads/mysql/
    echo.
    echo 安装完成后，请重新运行此脚本检测。
)

echo ========================================
echo.
pause
