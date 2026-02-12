# 数据库与落库数据说明（当前实现）

## 1. 基础信息
- 数据库：`gym_fitness_analytics`
- 默认连接：`localhost:3306`
- 默认账号：`root / 123456`
- 结构脚本：`database/schema.sql`

## 2. 推荐初始化流程

先初始化结构：

```bat
init-database.bat
```

再执行一键重建与校验：

```bat
seed-all-db-artifacts.bat
```

该脚本会按顺序执行（共 6 步）：
1. 生成管理员/教练并分配学员、补齐计划
2. 补齐近期运动与体测、刷新排行榜
3. 重建学员成就
4. 回填 `exercise_records.created_at`
5. 重建 analytics 结果表
6. 运行 API 冒烟检查（`scripts/api_smoke_check.py`）

> 第 6 步失败会直接返回非 0，便于一眼发现链路问题。

## 3. 关键数据修复说明

### 3.1 `exercise_records.created_at` 回填
文件：`database/backfill_exercise_created_at.py`

用途：修复历史导入中 `created_at` 与 `exercise_date` 不一致问题，避免时段分析图集中到 `0:00`。

### 3.2 近期活跃补齐
文件：`database/ensure_recent_student_activity.py`

用途：确保学员近期有落库记录，并保证本周覆盖，避免前端“本周概况为 0”。

## 4. 接口联通校验

文件：`scripts/api_smoke_check.py`

覆盖角色：`ADMIN / COACH / STUDENT`

覆盖范围：认证后核心页面接口（用户、计划、运动、体测、分析、排行榜、运动库等）。

执行：

```bat
python scripts\api_smoke_check.py
```

输出：
- `TOTAL_CHECKS`
- `FAIL_COUNT`

`FAIL_COUNT > 0` 时进程退出码为 `1`。

## 5. 当前账号口径

`init-database.bat` 纯净模式不自动写测试账号。

执行 `seed-all-db-artifacts.bat` 后，示例账号：
- 管理员：`admin_auto_001 / 123456`
- 教练：`coach_auto_001 / 123456`

学员账号为真实导入/生成账号（统一密码 `123456`）。

