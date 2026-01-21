@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo    健身管理系统 - 一键启动
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

echo 项目根目录: %PROJECT_ROOT%
echo.

:: 检测数据库连接
echo [1/4] 检测数据库连接...
mysql -h%DB_HOST% -u%DB_USER% -p%DB_PASS% -e "USE %DB_NAME%; SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo ✗ 数据库连接失败
    echo   请先运行 init-database.bat 初始化数据库
    pause
    exit /b 1
)
echo ✓ 数据库连接成功
echo.

:: 检测后端依赖
echo [2/4] 检测项目依赖...
if not exist "%PROJECT_ROOT%\backend\gym-web\target" (
    echo ⚠ 后端未编译，正在编译...
    cd /d "%PROJECT_ROOT%\backend"
    call mvn clean install -DskipTests -q
    if errorlevel 1 (
        echo ✗ 后端编译失败，请先运行 install-deps.bat
        pause
        exit /b 1
    )
)
if not exist "%PROJECT_ROOT%\frontend\node_modules" (
    echo ⚠ 前端依赖未安装，正在安装...
    cd /d "%PROJECT_ROOT%\frontend"
    call npm install
    if errorlevel 1 (
        echo ✗ 前端依赖安装失败，请先运行 install-deps.bat
        pause
        exit /b 1
    )
)
echo ✓ 项目依赖检测完成
echo.

:: 启动后端服务
echo [3/4] 启动后端服务 (端口 8080)...
cd /d "%PROJECT_ROOT%\backend\gym-web"
start "后端服务 - Spring Boot" cmd /k "title 后端服务 - Spring Boot && mvn spring-boot:run"
echo ✓ 后端服务启动中...
echo.

:: 等待后端启动
echo 等待后端服务启动 (约10秒)...
timeout /t 10 /nobreak >nul

:: 启动前端服务
echo [4/4] 启动前端服务 (端口 3000)...
cd /d "%PROJECT_ROOT%\frontend"
start "前端服务 - Vite" cmd /k "title 前端服务 - Vite && npm run dev"
echo ✓ 前端服务启动中...
echo.

:: 等待前端启动
echo 等待前端服务启动 (约5秒)...
timeout /t 5 /nobreak >nul

:: 打开浏览器
echo 正在打开浏览器...
start http://localhost:3000

echo.
echo ========================================
echo ✓ 系统启动完成！
echo.
echo 访问地址: http://localhost:3000
echo.
echo 测试账号:
echo   学员: test_student / test123
echo   教练: test_coach / test123  
echo   管理员: test_admin / test123
echo.
echo 提示: 
echo   - 后端服务窗口请勿关闭
echo   - 前端服务窗口请勿关闭
echo   - 按 Ctrl+C 可停止服务
echo ========================================
echo.
pause
