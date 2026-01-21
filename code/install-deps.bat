@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo    健身管理系统 - 安装项目依赖
echo ========================================
echo.

:: 获取脚本所在目录
cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

echo 项目根目录: %PROJECT_ROOT%
echo.

:: 安装后端依赖
echo [1/2] 安装后端 Maven 依赖...
echo.
cd /d "%PROJECT_ROOT%\backend"
if not exist "pom.xml" (
    echo ✗ 未找到 backend\pom.xml，请确保项目结构完整
    pause
    exit /b 1
)

call mvn clean install -DskipTests -q
if errorlevel 1 (
    echo ✗ 后端依赖安装失败
    echo   请检查 Maven 配置和网络连接
    pause
    exit /b 1
)
echo ✓ 后端依赖安装完成
echo.

:: 安装前端依赖
echo [2/2] 安装前端 npm 依赖...
echo.
cd /d "%PROJECT_ROOT%\frontend"
if not exist "package.json" (
    echo ✗ 未找到 frontend\package.json，请确保项目结构完整
    pause
    exit /b 1
)

call npm install
if errorlevel 1 (
    echo ✗ 前端依赖安装失败
    echo   请检查 npm 配置和网络连接
    echo   可尝试使用淘宝镜像: npm config set registry https://registry.npmmirror.com
    pause
    exit /b 1
)
echo ✓ 前端依赖安装完成
echo.

echo ========================================
echo ✓ 所有依赖安装完成！
echo.
echo 下一步：
echo   1. 运行 init-database.bat 初始化数据库
echo   2. 运行 start.bat 启动项目
echo ========================================
echo.
pause
