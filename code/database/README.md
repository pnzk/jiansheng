# 数据库设置指南

## 前置要求

- MySQL 8.0+
- Python 3.8+
- 已完成数据生成和清洗步骤

## 快速开始

### 1. 创建数据库和表结构

```bash
# 方式1: 使用MySQL命令行
mysql -u root -p < database/schema.sql

# 方式2: 使用MySQL Workbench
# 打开 schema.sql 文件并执行
```

### 2. 配置数据库连接

修改以下文件中的数据库密码：
- `database/import_data.py`
- `data-processing/spark_analyzer.py`

```python
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'your_password',  # 修改这里
    'database': 'gym_fitness_analytics',
    'charset': 'utf8mb4'
}
```

### 3. 导入数据

```bash
# Windows
scripts\setup_database.bat

# 或直接运行Python脚本
python database/import_data.py
```

### 4. 运行Spark分析

```bash
# 安装依赖
pip install pyspark mysql-connector-python

# 运行分析
scripts\run_spark_analysis.bat

# 或直接运行Python脚本
python data-processing/spark_analyzer.py
```

## 数据库结构

### 核心表

1. **users** - 用户表
   - 存储用户基本信息
   - 支持三种角色：ADMIN, COACH, STUDENT

2. **exercise_records** - 运动记录表
   - 存储用户的运动记录
   - 包含运动类型、时长、卡路里等

3. **body_metrics** - 身体指标表
   - 存储用户的身体测量数据
   - 包含体重、体脂率、BMI等

4. **training_plans** - 训练计划表
   - 教练为学员制定的训练计划
   - 包含目标、进度等信息

5. **achievements** - 成就表
   - 系统预定义的成就
   - 包含解锁条件和图标

6. **user_achievements** - 用户成就表
   - 用户已解锁的成就记录

7. **equipment_usage** - 器材使用记录表
   - 统计器材使用情况

8. **user_behavior_analysis** - 用户行为分析结果表
   - 存储Spark分析的结果

9. **leaderboards** - 排行榜表
   - 存储各类排行榜数据

### 视图

- **v_user_exercise_stats** - 用户运动统计视图
- **v_user_weight_change** - 用户体重变化视图

### 存储过程

- **update_leaderboards** - 更新排行榜数据

## Spark分析功能

### 1. 用户行为分析
- 最受欢迎的运动类型
- 活跃时段分析
- 平均运动时长
- 活跃用户统计
- 运动类型分布

### 2. 健身效果分析
- 体重变化趋势
- 减重统计
- 用户减重排名

### 3. 运动关联分析
- 分析用户经常组合进行的运动
- 发现运动模式

### 4. 排行榜生成
- 总运动时长排行榜
- 总消耗卡路里排行榜
- 减重排行榜

## 数据验证

导入完成后，可以运行以下SQL验证数据：

```sql
-- 检查用户数量
SELECT COUNT(*) FROM users;

-- 检查运动记录数量
SELECT COUNT(*) FROM exercise_records;

-- 检查身体指标数量
SELECT COUNT(*) FROM body_metrics;

-- 查看用户运动统计
SELECT * FROM v_user_exercise_stats LIMIT 10;

-- 查看用户体重变化
SELECT * FROM v_user_weight_change LIMIT 10;

-- 查看排行榜
SELECT * FROM leaderboards WHERE leaderboard_type = 'TOTAL_DURATION' LIMIT 10;
```

## 注意事项

1. **默认密码**: 所有导入的用户默认密码为 `password123`（已BCrypt加密）
2. **数据隐私**: 确保在生产环境中修改默认密码
3. **性能优化**: 已创建必要的索引，如需进一步优化可根据查询模式调整
4. **定时任务**: 可以启用MySQL事件调度器自动更新排行榜

## 故障排除

### 连接失败
- 检查MySQL服务是否运行
- 验证用户名和密码
- 确认数据库已创建

### 导入错误
- 检查CSV文件路径是否正确
- 验证数据格式是否符合要求
- 查看错误日志定位问题

### Spark分析失败
- 确保已安装PySpark
- 检查内存配置是否足够
- 验证数据文件是否存在

## 下一步

数据库设置完成后，可以继续：
1. 开发Spring Boot后端API
2. 实现RESTful接口
3. 开发Vue.js前端界面
