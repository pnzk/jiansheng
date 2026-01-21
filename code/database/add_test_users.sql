-- 添加测试账号
-- 密码都是 test123，使用BCrypt加密后的值

-- 清空现有测试账号（如果存在）
DELETE FROM users WHERE username IN ('test_student', 'test_coach', 'test_admin');

-- 插入测试学员账号
INSERT INTO users (username, password, real_name, email, phone, age, gender, height_cm, initial_weight_kg, fitness_goal, role, created_at)
VALUES ('test_student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试学员', 'student@test.com', '13800000001', 25, '男', 175, 70, '减重', 'STUDENT', NOW());

-- 插入测试教练账号
INSERT INTO users (username, password, real_name, email, phone, age, gender, height_cm, initial_weight_kg, fitness_goal, role, created_at)
VALUES ('test_coach', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试教练', 'coach@test.com', '13800000002', 30, '男', 180, 75, NULL, 'COACH', NOW());

-- 插入测试管理员账号
INSERT INTO users (username, password, real_name, email, phone, age, gender, height_cm, initial_weight_kg, fitness_goal, role, created_at)
VALUES ('test_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试管理员', 'admin@test.com', '13800000003', 35, '男', 178, 72, NULL, 'ADMIN', NOW());

-- 查看插入结果
SELECT user_id, username, real_name, role FROM users WHERE username LIKE 'test_%';
