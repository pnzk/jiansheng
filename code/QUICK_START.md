# 快速开始指南

## 一、环境准备

### 必需软件
请确保已安装以下软件：

| 软件 | 版本要求 | 下载地址 |
|------|----------|----------|
| Java JDK | 8+ (推荐11或17) | https://adoptium.net/ |
| Maven | 3.6+ | https://maven.apache.org/download.cgi |
| Node.js | 16+ (推荐LTS) | https://nodejs.org/ |
| MySQL | 8.0+ | https://dev.mysql.com/downloads/mysql/ |

### 检测环境
双击运行 `check-env.bat` 检测是否安装完成。

---

## 二、一键启动（推荐）

1. 确保 MySQL 服务已启动
2. 双击运行 `quick-start.bat`
3. 等待脚本自动完成所有配置
4. 浏览器自动打开 http://localhost:3000

---

## 三、手动启动

### 步骤1：安装依赖
```bash
install-deps.bat
```

### 步骤2：初始化数据库
```bash
init-database.bat
```
> 默认数据库配置：用户名 `root`，密码 `123456`
> 如需修改，请编辑脚本中的 `DB_USER` 和 `DB_PASS`

### 步骤3：导入数据（可选）
```bash
import-data.bat
```
> 导入清洗后的真实健身数据（约42000条记录）

### 步骤4：启动系统
```bash
start-all.bat
```

### 停止系统
```bash
stop.bat
```

---

## 四、访问系统

- **前端地址**: http://localhost:3000
- **后端API**: http://localhost:8080

### 测试账号
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 学员 | test_student | test123 |
| 教练 | test_coach | test123 |
| 管理员 | test_admin | test123 |

---

## 五、常见问题

### Q: 数据库连接失败
**A**: 
1. 确保 MySQL 服务已启动
2. 确认用户名密码正确（默认 root/123456）
3. 如需修改密码，编辑各 `.bat` 脚本中的 `DB_PASS`

### Q: 后端启动失败
**A**: 
1. 检查 8080 端口是否被占用
2. 运行 `netstat -ano | findstr 8080` 查看占用进程
3. 确保 Java 和 Maven 已正确安装

### Q: 前端页面空白
**A**: 
1. 确保后端服务正在运行
2. 按 F12 打开浏览器控制台查看错误
3. 尝试清除浏览器缓存（Ctrl+Shift+R）

### Q: Maven/npm 下载慢
**A**: 配置国内镜像
```bash
# npm 淘宝镜像
npm config set registry https://registry.npmmirror.com

# Maven 阿里云镜像（编辑 ~/.m2/settings.xml）
```

---

## 六、项目结构

```
code/
├── backend/          # 后端 Spring Boot 项目
├── frontend/         # 前端 Vue 3 项目
├── database/         # 数据库脚本
├── data-collection/  # 数据收集脚本
├── data-processing/  # 数据清洗脚本
│   └── cleaned/      # 清洗后的CSV数据
├── csv/              # 原始数据文件
├── quick-start.bat   # 一键启动（推荐）
├── start-all.bat     # 启动服务
├── stop.bat          # 停止服务
├── check-env.bat     # 环境检测
├── install-deps.bat  # 安装依赖
├── init-database.bat # 初始化数据库
├── import-data.bat   # 导入数据
├── README.md         # 项目说明
└── 项目功能清单.md    # 功能清单
```

---

## 七、技术栈

### 前端
- Vue 3.3.4 + Vue Router + Pinia
- Element Plus UI组件库
- ECharts 数据可视化
- Vite 构建工具

### 后端
- Spring Boot 2.7.18
- MyBatis Plus 3.5.3.1
- JWT 认证
- MySQL 8.0

### 数据处理
- Python + Pandas
- 支持Spark大数据分析
