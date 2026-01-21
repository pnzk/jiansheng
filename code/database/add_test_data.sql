-- 添加测试数据脚本
-- 用于快速测试运动日历、效果追踪、训练计划功能

USE gym_fitness_analytics;

-- 注意：请先确认你的用户ID
-- 可以运行: SELECT id, username, user_role FROM users;
-- 然后将下面的 @student_id 和 @coach_id 替换为实际的ID

-- 设置变量（请根据实际情况修改）
SET @student_id = 1;  -- 学生用户ID
SET @coach_id = 2;    -- 教练用户ID（如果没有教练，可以先创建一个）

-- 如果没有教练账号，先创建一个
INSERT IGNORE INTO users (username, password, email, real_name, user_role, created_at)
VALUES ('coach1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z2EHdHlALq.1eBl.1eTI8.K6', 'coach1@gym.com', '张教练', 'COACH', NOW());

-- 获取刚创建的教练ID（如果是新创建的）
SET @coach_id = (SELECT id FROM users WHERE username = 'coach1' LIMIT 1);

-- 1. 添加运动记录（最近30天的数据）
INSERT INTO exercise_records (user_id, exercise_type, exercise_date, duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used, notes, created_at)
VALUES 
-- 今天
(@student_id, 'RUNNING', CURDATE(), 30, 250, 120, 150, '跑步机', '今天状态不错', NOW()),
(@student_id, 'STRENGTH_TRAINING', CURDATE(), 45, 180, 100, 130, '哑铃', '力量训练', NOW()),

-- 昨天
(@student_id, 'CYCLING', CURDATE() - INTERVAL 1 DAY, 40, 280, 115, 145, '动感单车', '有氧运动', NOW()),

-- 前天
(@student_id, 'YOGA', CURDATE() - INTERVAL 2 DAY, 60, 150, 90, 110, '瑜伽垫', '放松训练', NOW()),

-- 3天前
(@student_id, 'RUNNING', CURDATE() - INTERVAL 3 DAY, 35, 270, 125, 155, '跑步机', '', NOW()),

-- 4天前
(@student_id, 'SWIMMING', CURDATE() - INTERVAL 4 DAY, 50, 320, 110, 140, '游泳池', '游泳训练', NOW()),

-- 5天前
(@student_id, 'STRENGTH_TRAINING', CURDATE() - INTERVAL 5 DAY, 50, 200, 105, 135, '杠铃', '深蹲训练', NOW()),

-- 7天前
(@student_id, 'RUNNING', CURDATE() - INTERVAL 7 DAY, 30, 240, 118, 148, '跑步机', '', NOW()),
(@student_id, 'CYCLING', CURDATE() - INTERVAL 7 DAY, 30, 220, 112, 142, '动感单车', '', NOW()),

-- 10天前
(@student_id, 'STRENGTH_TRAINING', CURDATE() - INTERVAL 10 DAY, 45, 190, 102, 132, '哑铃', '', NOW()),

-- 14天前
(@student_id, 'RUNNING', CURDATE() - INTERVAL 14 DAY, 35, 260, 122, 152, '跑步机', '长跑训练', NOW()),
(@student_id, 'YOGA', CURDATE() - INTERVAL 14 DAY, 60, 160, 92, 112, '瑜伽垫', '', NOW()),

-- 21天前
(@student_id, 'CYCLING', CURDATE() - INTERVAL 21 DAY, 45, 300, 118, 148, '动感单车', '', NOW()),
(@student_id, 'SWIMMING', CURDATE() - INTERVAL 21 DAY, 40, 280, 108, 138, '游泳池', '', NOW()),

-- 28天前
(@student_id, 'RUNNING', CURDATE() - INTERVAL 28 DAY, 30, 230, 115, 145, '跑步机', '开始训练', NOW());

-- 2. 添加身体指标记录（每周一次，共5周）
INSERT INTO body_metrics (user_id, measurement_date, weight_kg, body_fat_percentage, height_cm, bmi, muscle_mass_kg, created_at)
VALUES 
-- 今天
(@student_id, CURDATE(), 68.5, 18.5, 175, 22.4, 52.0, NOW()),

-- 1周前
(@student_id, CURDATE() - INTERVAL 7 DAY, 69.0, 19.0, 175, 22.5, 51.5, NOW()),

-- 2周前
(@student_id, CURDATE() - INTERVAL 14 DAY, 69.5, 19.5, 175, 22.7, 51.0, NOW()),

-- 3周前
(@student_id, CURDATE() - INTERVAL 21 DAY, 70.0, 20.0, 175, 22.9, 50.5, NOW()),

-- 4周前
(@student_id, CURDATE() - INTERVAL 28 DAY, 70.5, 20.5, 175, 23.0, 50.0, NOW());

-- 3. 添加训练计划
INSERT INTO training_plans (student_id, coach_id, plan_name, goal_type, target_value, start_date, end_date, status, completion_rate, weekly_schedule, description, created_at, updated_at)
VALUES 
(@student_id, @coach_id, '30天减脂塑形计划', 'FAT_LOSS', 5.0, 
 CURDATE() - INTERVAL 15 DAY, CURDATE() + INTERVAL 15 DAY, 
 'ACTIVE', 50.0, 
 '{"monday": "有氧运动30分钟 + 核心训练15分钟", "tuesday": "休息或轻度拉伸", "wednesday": "力量训练45分钟（上肢）", "thursday": "有氧运动40分钟", "friday": "力量训练45分钟（下肢）", "saturday": "游泳或骑行60分钟", "sunday": "瑜伽或休息"}',
 '这是一个为期30天的减脂塑形计划，结合有氧运动和力量训练，帮助你降低体脂率并增加肌肉量。每周训练5-6天，注意休息和营养补充。目标是在30天内降低体脂率2-3%，同时保持或增加肌肉量。',
 NOW(), NOW());

-- 4. 验证数据
SELECT '=== 数据添加完成 ===' AS message;
SELECT CONCAT('用户ID: ', @student_id) AS info;
SELECT CONCAT('教练ID: ', @coach_id) AS info;
SELECT CONCAT('运动记录数: ', COUNT(*)) AS info FROM exercise_records WHERE user_id = @student_id;
SELECT CONCAT('身体指标数: ', COUNT(*)) AS info FROM body_metrics WHERE user_id = @student_id;
SELECT CONCAT('训练计划数: ', COUNT(*)) AS info FROM training_plans WHERE student_id = @student_id;

-- 5. 显示最近的记录
SELECT '=== 最近的运动记录 ===' AS message;
SELECT exercise_date, exercise_type, duration_minutes, calories_burned 
FROM exercise_records 
WHERE user_id = @student_id 
ORDER BY exercise_date DESC 
LIMIT 5;

SELECT '=== 最近的身体指标 ===' AS message;
SELECT measurement_date, weight_kg, body_fat_percentage, bmi 
FROM body_metrics 
WHERE user_id = @student_id 
ORDER BY measurement_date DESC 
LIMIT 5;

SELECT '=== 训练计划 ===' AS message;
SELECT plan_name, goal_type, start_date, end_date, status, completion_rate 
FROM training_plans 
WHERE student_id = @student_id;
