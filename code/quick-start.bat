@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo    健身管理系统 - 快速启动向导
echo ========================================
echo.

:: 获取脚本所在目录
cd /d "%~dp0"
set "PROJECT_ROOT=%cd%"

echo 此向导将帮助您完成以下步骤:
echo   1. 检测运行环境
echo   2. 安装项目依赖
echo   3. 初始化数据库
echo   4. 启动系统
echo.
echo 按任意键开始...
pause >nul

:: 步骤1: 环境检测
echo.
echo ========================================
echo [步骤 1/4] 检测运行环境
echo ========================================
echo.

set "ENV_OK=1"

:: 检测 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ✗ Java 未安装 - 请安装 JDK 8+
    set "ENV_OK=0"
) else (
    echo ✓ Java 已安装
)

:: 检测 Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ✗ Maven 未安装 - 请安装 Maven 3.6+
    set "ENV_OK=0"
) else (
    echo ✓ Maven 已安装
)

:: 检测 Node.js
node -v >nul 2>&1
if errorlevel 1 (
    echo ✗ Node.js 未安装 - 请安装 Node.js 16+
    set "ENV_OK=0"
) else (
    echo ✓ Node.js 已安装
)

:: 检测 MySQL
mysql --version >nul 2>&1
if errorlevel 1 (
    echo ✗ MySQL 未安装 - 请安装 MySQL 8.0+
    set "ENV_OK=0"
) else (
    echo ✓ MySQL 已安装
)

if "%ENV_OK%"=="0" (
    echo.
    echo ✗ 环境检测未通过，请先安装缺失的软件
    echo.
    echo 软件下载地址:
    echo   Java: https://adoptium.net/
    echo   Maven: https://maven.apache.org/download.cgi
    echo   Node.js: https://nodejs.org/
    echo   MySQL: https://dev.mysql.com/downloads/mysql/
    echo.
    pause
    exit /b 1
)

echo.
echo ✓ 环境检测通过
timeout /t 2 >nul

:: 步骤2: 安装依赖
echo.
echo ========================================
echo [步骤 2/4] 安装项目依赖
echo ========================================
echo.

if not exist "%PROJECT_ROOT%\backend\gym-web\target" (
    echo 正在安装后端依赖 (首次运行需要几分钟)...
    cd /d "%PROJECT_ROOT%\backend"
    call mvn clean install -DskipTests -q
    if errorlevel 1 (
        echo ✗ 后端依赖安装失败
        pause
        exit /b 1
    )
    echo ✓ 后端依赖安装完成
) else (
    echo ✓ 后端依赖已存在，跳过
)

if not exist "%PROJECT_ROOT%\frontend\node_modules" (
    echo 正在安装前端依赖...
    cd /d "%PROJECT_ROOT%\frontend"
    call npm install --silent
    if errorlevel 1 (
        echo ✗ 前端依赖安装失败
        pause
        exit /b 1
    )
    echo ✓ 前端依赖安装完成
) else (
    echo ✓ 前端依赖已存在，跳过
)

timeout /t 2 >nul

:: 步骤3: 初始化数据库
echo.
echo ========================================
echo [步骤 3/4] 初始化数据库
echo ========================================
echo.

set "DB_USER=root"
set "DB_PASS=123456"
set "DB_NAME=gym_fitness_analytics"

mysql -u%DB_USER% -p%DB_PASS% -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo ✗ 无法连接 MySQL，请确保:
    echo   1. MySQL 服务已启动
    echo   2. 用户名: root, 密码: 123456
    echo.
    echo 如需修改密码，请编辑此脚本
    pause
    exit /b 1
)

mysql -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
echo ✓ 数据库已就绪

if exist "%PROJECT_ROOT%\database\schema.sql" (
    mysql -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%PROJECT_ROOT%\database\schema.sql" 2>nul
    echo ✓ 数据表已初始化
)

timeout /t 2 >nul

:: 步骤4: 启动系统
echo.
echo ========================================
echo [步骤 4/4] 启动系统
echo ========================================
echo.

echo 正在启动后端服务...
cd /d "%PROJECT_ROOT%\backend\gym-web"
start "后端服务" cmd /c "title 后端服务 - 请勿关闭 && mvn spring-boot:run"

echo 等待后端启动 (10秒)...
timeout /t 10 >nul

echo 正在启动前端服务...
cd /d "%PROJECT_ROOT%\frontend"
start "前端服务" cmd /c "title 前端服务 - 请勿关闭 && npm run dev"

echo 等待前端启动 (5秒)...
timeout /t 5 >nul

echo 正在打开浏览器...
start http://localhost:3000

echo.
echo ========================================
echo ✓ 系统启动成功！
echo ========================================
echo.
echo 访问地址: http://localhost:3000
echo.
echo 测试账号:
echo   学员: test_student / test123
echo   教练: test_coach / test123
echo   管理员: test_admin / test123
echo.
echo 提示: 关闭此窗口不会停止服务
echo       如需停止服务，请运行 stop.bat
echo ========================================
echo.
pause
