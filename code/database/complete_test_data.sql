-- 完整测试数据脚本
-- 包含：多个用户（学员、教练、管理员）、运动记录、身体指标、训练计划、成就等

USE gym_fitness_analytics;

-- 清空现有数据（保留表结构）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_achievements;
TRUNCATE TABLE leaderboards;
TRUNCATE TABLE training_plans;
TRUNCATE TABLE body_metrics;
TRUNCATE TABLE exercise_records;
TRUNCATE TABLE users;
TRUNCATE TABLE achievements;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 1. 用户数据
-- ============================================

-- 管理员账号
INSERT INTO users (username, password, email, phone, real_name, age, gender, user_role, created_at) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@gym.com', '13800000001', '系统管理员', 35, 'MALE', 'ADMIN', NOW());

-- 教练账号（3个）
INSERT INTO users (username, password, email, phone, real_name, age, gender, user_role, created_at) VALUES
('coach_zhang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'zhang@gym.com', '13800000002', '张教练', 30, 'MALE', 'COACH', NOW()),
('coach_li', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'li@gym.com', '13800000003', '李教练', 28, 'FEMALE', 'COACH', NOW()),
('coach_wang', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'wang@gym.com', '13800000004', '王教练', 32, 'MALE', 'COACH', NOW());

-- 学员账号（10个）
INSERT INTO users (username, password, email, phone, real_name, age, gender, user_role, created_at) VALUES
('student01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student01@gym.com', '13900000001', '张三', 25, 'MALE', 'STUDENT', NOW()),
('student02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student02@gym.com', '13900000002', '李四', 23, 'FEMALE', 'STUDENT', NOW()),
('student03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student03@gym.com', '13900000003', '王五', 27, 'MALE', 'STUDENT', NOW()),
('student04', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student04@gym.com', '13900000004', '赵六', 24, 'FEMALE', 'STUDENT', NOW()),
('student05', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student05@gym.com', '13900000005', '钱七', 26, 'MALE', 'STUDENT', NOW()),
('student06', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student06@gym.com', '13900000006', '孙八', 22, 'FEMALE', 'STUDENT', NOW()),
('student07', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student07@gym.com', '13900000007', '周九', 28, 'MALE', 'STUDENT', NOW()),
('student08', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student08@gym.com', '13900000008', '吴十', 25, 'FEMALE', 'STUDENT', NOW()),
('student09', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student09@gym.com', '13900000009', '郑十一', 29, 'MALE', 'STUDENT', NOW()),
('student10', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'student10@gym.com', '13900000010', '陈十二', 24, 'FEMALE', 'STUDENT', NOW());

-- ============================================
-- 2. 运动记录数据（为前5个学员添加）
-- ============================================

-- 学员1的运动记录（最近30天）
INSERT INTO exercise_records (user_id, exercise_type, exercise_date, duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used, notes, created_at) VALUES
(5, '跑步', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 30, 250, 120, 150, '跑步机', '状态良好', NOW()),
(5, '力量训练', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 45, 180, 110, 135, '哑铃', '增加重量', NOW()),
(5, '游泳', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 40, 300, 115, 140, '泳池', '自由泳', NOW()),
(5, '跑步', DATE_SUB(CURDATE(), INTERVAL 5 DAY), 35, 280, 125, 155, '跑步机', '提速训练', NOW()),
(5, '瑜伽', DATE_SUB(CURDATE(), INTERVAL 7 DAY), 60, 150, 90, 110, '瑜伽垫', '柔韧性训练', NOW());

-- 学员2的运动记录
INSERT INTO exercise_records (user_id, exercise_type, exercise_date, duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used, notes, created_at) VALUES
(6, '动感单车', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 45, 350, 130, 160, '动感单车', '高强度', NOW()),
(6, '普拉提', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 50, 200, 100, 120, '普拉提器械', '核心训练', NOW()),
(6, '跑步', DATE_SUB(CURDATE(), INTERVAL 4 DAY), 30, 240, 118, 145, '跑步机', '有氧训练', NOW()),
(6, '力量训练', DATE_SUB(CURDATE(), INTERVAL 6 DAY), 40, 190, 105, 130, '器械', '上肢训练', NOW());

-- 学员3的运动记录
INSERT INTO exercise_records (user_id, exercise_type, exercise_date, duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used, notes, created_at) VALUES
(7, '篮球', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 60, 400, 135, 170, '篮球场', '对抗训练', NOW()),
(7, '力量训练', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 50, 220, 115, 140, '杠铃', '深蹲训练', NOW()),
(7, '跑步', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 40, 320, 128, 158, '户外', '长跑', NOW());

-- 学员4的运动记录
INSERT INTO exercise_records (user_id, exercise_type, exercise_date, duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used, notes, created_at) VALUES
(8, '瑜伽', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 55, 160, 88, 105, '瑜伽垫', '放松训练', NOW()),
(8, '游泳', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 45, 310, 120, 145, '泳池', '蛙泳', NOW()),
(8, '普拉提', DATE_SUB(CURDATE(), INTERVAL 5 DAY), 50, 195, 95, 115, '普拉提器械', '塑形', NOW());

-- 学员5的运动记录
INSERT INTO exercise_records (user_id, exercise_type, exercise_date, duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used, notes, created_at) VALUES
(9, '拳击', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 40, 380, 140, 175, '拳击沙袋', '爆发力训练', NOW()),
(9, '跑步', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 35, 290, 125, 152, '跑步机', '间歇跑', NOW()),
(9, '力量训练', DATE_SUB(CURDATE(), INTERVAL 4 DAY), 45, 210, 112, 138, '器械', '全身训练', NOW());

-- ============================================
-- 3. 身体指标数据
-- ============================================

-- 学员1的身体指标（最近3个月）
INSERT INTO body_metrics (user_id, measurement_date, weight_kg, height_cm, body_fat_percentage, muscle_mass_kg, bmi, created_at) VALUES
(5, DATE_SUB(CURDATE(), INTERVAL 90 DAY), 75.0, 175, 22.0, 52.0, 24.49, NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 73.5, 175, 20.5, 53.0, 24.00, NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 72.0, 175, 19.0, 54.0, 23.51, NOW()),
(5, CURDATE(), 70.5, 175, 18.0, 55.0, 23.02, NOW());

-- 学员2的身体指标
INSERT INTO body_metrics (user_id, measurement_date, weight_kg, height_cm, body_fat_percentage, muscle_mass_kg, bmi, created_at) VALUES
(6, DATE_SUB(CURDATE(), INTERVAL 90 DAY), 58.0, 165, 25.0, 38.0, 21.30, NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 57.0, 165, 23.5, 39.0, 20.94, NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 56.0, 165, 22.0, 40.0, 20.57, NOW()),
(6, CURDATE(), 55.0, 165, 20.5, 41.0, 20.20, NOW());

-- 学员3的身体指标
INSERT INTO body_metrics (user_id, measurement_date, weight_kg, height_cm, body_fat_percentage, muscle_mass_kg, bmi, created_at) VALUES
(7, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 82.0, 180, 20.0, 60.0, 25.31, NOW()),
(7, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 80.5, 180, 18.5, 61.5, 24.85, NOW()),
(7, CURDATE(), 79.0, 180, 17.0, 63.0, 24.38, NOW());

-- 学员4的身体指标
INSERT INTO body_metrics (user_id, measurement_date, weight_kg, height_cm, body_fat_percentage, muscle_mass_kg, bmi, created_at) VALUES
(8, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 52.0, 160, 24.0, 35.0, 20.31, NOW()),
(8, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 51.0, 160, 22.5, 36.0, 19.92, NOW()),
(8, CURDATE(), 50.0, 160, 21.0, 37.0, 19.53, NOW());

-- 学员5的身体指标
INSERT INTO body_metrics (user_id, measurement_date, weight_kg, height_cm, body_fat_percentage, muscle_mass_kg, bmi, created_at) VALUES
(9, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 78.0, 178, 19.0, 58.0, 24.62, NOW()),
(9, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 76.5, 178, 17.5, 59.5, 24.15, NOW()),
(9, CURDATE(), 75.0, 178, 16.0, 61.0, 23.67, NOW());

-- ============================================
-- 4. 训练计划数据
-- ============================================

-- 为前5个学员创建训练计划（教练分配）
INSERT INTO training_plans (student_id, coach_id, plan_name, goal_type, target_value, start_date, end_date, status, completion_rate, weekly_schedule, description, created_at) VALUES
(5, 2, '减脂塑形计划', 'WEIGHT_LOSS', 68.0, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'ACTIVE', 45.5, 
'周一：有氧30分钟+力量训练\n周三：游泳40分钟\n周五：跑步35分钟+拉伸', 
'目标：3个月减重5kg，降低体脂率至15%以下', NOW()),

(6, 2, '塑形美体计划', 'BODY_SHAPING', 53.0, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 70 DAY), 'ACTIVE', 35.2,
'周二：普拉提50分钟\n周四：动感单车45分钟\n周六：瑜伽60分钟',
'目标：塑造完美身材曲线，提升核心力量', NOW()),

(7, 3, '增肌强化计划', 'MUSCLE_GAIN', 82.0, DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 75 DAY), 'ACTIVE', 28.8,
'周一：胸+三头\n周三：背+二头\n周五：腿+肩',
'目标：增加肌肉量3kg，提升力量水平', NOW()),

(8, 3, '健康维护计划', 'HEALTH_MAINTENANCE', 50.0, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 80 DAY), 'ACTIVE', 18.5,
'周二：瑜伽55分钟\n周四：游泳45分钟\n周六：普拉提50分钟',
'目标：保持健康体重，提升身体柔韧性', NOW()),

(9, 4, '综合体能提升', 'FITNESS_IMPROVEMENT', 100.0, DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_ADD(CURDATE(), INTERVAL 65 DAY), 'ACTIVE', 42.0,
'周一：力量训练45分钟\n周三：拳击40分钟\n周五：跑步35分钟',
'目标：全面提升体能水平，增强爆发力', NOW());

-- ============================================
-- 5. 成就系统数据
-- ============================================

-- 创建成就类型（使用schema中定义的列名）
INSERT INTO achievements (achievement_name, description, achievement_type, threshold_value, icon_url, created_at) VALUES
('初来乍到', '完成第一次运动记录', 'EXERCISE_COUNT', 1, '🎯', NOW()),
('坚持不懈', '连续运动7天', 'CONSECUTIVE_DAYS', 7, '🔥', NOW()),
('运动达人', '累计运动30次', 'EXERCISE_COUNT', 30, '💪', NOW()),
('马拉松挑战者', '单次跑步超过10公里', 'SINGLE_DISTANCE', 10, '🏃', NOW()),
('减脂小能手', '成功减重5kg', 'WEIGHT_LOSS', 5, '⚖️', NOW()),
('肌肉猛男', '增加肌肉量3kg', 'MUSCLE_GAIN', 3, '💪', NOW()),
('体脂杀手', '体脂率降低5%', 'BODY_FAT_REDUCTION', 5, '🔥', NOW()),
('健身新星', '累计运动时长达到50小时', 'TOTAL_DURATION', 3000, '⭐', NOW());

-- 为学员分配成就（使用schema中定义的列名）
INSERT INTO user_achievements (user_id, achievement_id, unlocked_at) VALUES
(5, 1, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(5, 2, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(5, 3, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, 1, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(6, 2, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(7, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(7, 3, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(8, 1, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(9, 1, DATE_SUB(NOW(), INTERVAL 22 DAY)),
(9, 2, DATE_SUB(NOW(), INTERVAL 12 DAY));

-- ============================================
-- 6. 排行榜数据（使用schema中定义的表名和列名）
-- ============================================

INSERT INTO leaderboards (leaderboard_type, user_id, `rank`, value, period_start, period_end, updated_at) VALUES
('TOTAL_DURATION', 5, 1, 450, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_DURATION', 7, 2, 420, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_DURATION', 9, 3, 380, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_DURATION', 6, 4, 350, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_DURATION', 8, 5, 300, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_CALORIES', 5, 1, 3500, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_CALORIES', 7, 2, 3200, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_CALORIES', 9, 3, 2900, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_CALORIES', 6, 4, 2700, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW()),
('TOTAL_CALORIES', 8, 5, 2200, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), NOW());

-- ============================================
-- 完成！
-- ============================================

SELECT '数据导入完成！' AS status;
SELECT '管理员账号: admin / 123456' AS admin_account;
SELECT '教练账号: coach_zhang / 123456, coach_li / 123456, coach_wang / 123456' AS coach_accounts;
SELECT '学员账号: student01~student10 / 123456' AS student_accounts;
SELECT CONCAT('共创建 ', COUNT(*), ' 个用户') AS user_count FROM users;
SELECT CONCAT('共创建 ', COUNT(*), ' 条运动记录') AS exercise_count FROM exercise_records;
SELECT CONCAT('共创建 ', COUNT(*), ' 条身体指标') AS metric_count FROM body_metrics;
SELECT CONCAT('共创建 ', COUNT(*), ' 个训练计划') AS plan_count FROM training_plans;
SELECT CONCAT('共创建 ', COUNT(*), ' 个成就') AS achievement_count FROM achievements;
