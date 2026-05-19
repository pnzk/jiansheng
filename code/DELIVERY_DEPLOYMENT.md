# 健身分析系统交付部署文档

## 1. 交付内容

本次交付包包含以下内容：

- `backend/`
  - Spring Boot 后端源码
- `frontend/`
  - Vue 3 前端源码
- `database/`
  - 数据初始化、补数、导入、演示数据修复脚本
- `scripts/`
  - API 冒烟、CRUD 一致性、认证链路校验脚本
- 根目录批处理脚本
  - `start-all.bat`
  - `quick-start.bat`
  - `migrate-and-start.bat`
  - `start.bat`
  - `stop.bat`
  - `init-database.bat`
  - `install-deps.bat`
  - `check-env.bat`
  - `seed-all-db-artifacts.bat`
  - `auth-smoke-test.bat`
  - `crud-smoke-test.bat`
- 本文档
  - `DELIVERY_DEPLOYMENT.md`

## 2. 运行环境

部署机器建议满足以下条件：

- Windows 10 / Windows 11
- Java JDK 17 及以上
- Maven 3.6 及以上
- Node.js 16 及以上
- MySQL 8.0 及以上

默认数据库配置如下：

- Host: `localhost`
- Port: `3306`
- Database: `gym_fitness_analytics`
- Username: `root`
- Password: `123456`

如果客户机器数据库账号不同，需要同步修改：

- `backend/src/main/resources/application.yml`
- 根目录各 `.bat` 脚本中的数据库变量

## 3. 快速部署

### 方案 A：推荐，一键部署启动

在交付包根目录执行：

```bat
start-all.bat
```

该脚本会自动完成：

1. 数据库迁移与初始化
2. 演示数据补齐
3. 后端启动
4. 前端启动
5. 自动打开浏览器

启动完成后访问：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`

### 方案 B：只重建数据库，不启动服务

```bat
migrate-and-start.bat --db-only
```

### 方案 C：数据库补数与验收校验

```bat
seed-all-db-artifacts.bat
auth-smoke-test.bat
crud-smoke-test.bat
```

## 4. 默认演示账号

建议交付时直接提供以下账号给客户验收：

- 管理员：`admin_auto_001 / 123456`
- 教练：`coach_auto_001 / 123456`
- 学员：`user00001 / 123456`

系统也支持学员自助注册，注册后会自动写入 `users` 表，并自动登录。

## 5. 部署后校验项

建议部署完成后按以下顺序检查：

### 管理员端

- 登录成功
- 全局监控页可正常展示当天数据
- 行为分析页可正常切换口径
- 教练管理、学员管理列表可正常打开

### 教练端

- 首页待办、最近活跃学员、重点关注学员可正常展示
- 计划管理页支持查看、编辑、复制、新增、删除
- 效果报告页可生成学员对比结果

### 学员端

- 首页、训练计划、打卡、进度、成就、运动库可正常访问
- 运动库可检索动作
- 新注册账号可自动登录并进入学员首页

## 6. 当前交付版说明

本交付版已经完成以下修复与补齐：

- 管理员分析页当天数据已补齐到当前日期附近，不再默认回退到过旧日期
- 运动库标准动作已从 7 条扩充到 50+ 条，可支持演示检索和筛选
- 学员注册成功后可自动登录
- 教练计划页和效果报告页已补充移动端适配
- 学员/教练首页已接入移动端布局切换

## 7. 常见问题

### 7.1 启动时报数据库连接失败

请先确认：

- MySQL 服务已启动
- 数据库账号密码正确
- `gym_fitness_analytics` 数据库允许当前账号访问

### 7.2 启动时报端口占用

请检查：

- `3000` 端口是否已被前端程序占用
- `8080` 端口是否已被后端程序占用

可先执行：

```bat
stop.bat
```

再重新启动。

### 7.3 页面空白或接口异常

请优先执行：

```bat
check-env.bat
auth-smoke-test.bat
crud-smoke-test.bat
```

### 7.4 客户机器需要重新导入演示数据

可执行：

```bat
seed-all-db-artifacts.bat
```

## 8. 交付建议

建议最终交付给客户时同时提供：

- 源码包
- 一键部署脚本
- 本部署文档
- 默认演示账号

如果客户只做验收演示，建议直接使用：

```bat
start-all.bat
```

这是最稳妥的启动方式。
