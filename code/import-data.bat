@echo off
chcp 65001 >nul

echo ========================================
echo    健身管理系统 - 导入清洗后的数据
echo ========================================
echo.

:: 获取脚本所在目录
cd /d "%~dp0"

:: 检查Python
python --version >nul 2>&1
if errorlevel 1 (
    echo ✗ Python 未安装，请先安装 Python 3.8+
    pause
    exit /b 1
)

:: 检查依赖
python -c "import pymysql, pandas, bcrypt" >nul 2>&1
if errorlevel 1 (
    echo 正在安装必要的Python依赖...
    pip install pymysql pandas bcrypt -q
)

:: 运行导入脚本
python database\import_cleaned_data.py

echo.
pause
