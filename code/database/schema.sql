-- 健身房运动行为与健身效果分析系统 - 数据库Schema
-- MySQL 8.0+

-- 创建数据库
CREATE DATABASE IF NOT EXISTS gym_fitness_analytics 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE gym_fitness_analytics;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    real_name VARCHAR(50),
    age INT,
    gender ENUM('MALE', 'FEMALE') COMMENT '男/女',
    user_role ENUM('ADMIN', 'COACH', 'STUDENT') NOT NULL DEFAULT 'STUDENT',
    coach_id BIGINT COMMENT '学员所属教练ID',
    fitness_goal ENUM('WEIGHT_LOSS', 'FAT_LOSS', 'MUSCLE_GAIN') COMMENT '健身目标',
    show_in_leaderboard BOOLEAN DEFAULT TRUE COMMENT '是否在排行榜显示',
    allow_coach_view BOOLEAN DEFAULT TRUE COMMENT '是否允许教练查看',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (user_role),
    INDEX idx_coach_id (coach_id),
    FOREIGN KEY (coach_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 运动记录表
CREATE TABLE IF NOT EXISTS exercise_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    exercise_type VARCHAR(50) NOT NULL COMMENT '运动类型',
    exercise_date DATE NOT NULL,
    duration_minutes INT NOT NULL COMMENT '运动时长(分钟)',
    calories_burned DECIMAL(10,2) COMMENT '消耗卡路里',
    average_heart_rate INT COMMENT '平均心率',
    max_heart_rate INT COMMENT '最大心率',
    equipment_used VARCHAR(50) COMMENT '使用的器材',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_exercise_date (exercise_date),
    INDEX idx_exercise_type (exercise_type),
    INDEX idx_user_date (user_id, exercise_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运动记录表';

-- 3. 身体指标表
CREATE TABLE IF NOT EXISTS body_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    measurement_date DATE NOT NULL,
    weight_kg DECIMAL(5,2) NOT NULL COMMENT '体重(kg)',
    body_fat_percentage DECIMAL(4,2) COMMENT '体脂率(%)',
    height_cm DECIMAL(5,2) COMMENT '身高(cm)',
    bmi DECIMAL(4,2) COMMENT 'BMI指数',
    muscle_mass_kg DECIMAL(5,2) COMMENT '肌肉量(kg)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_measurement_date (measurement_date),
    INDEX idx_user_date (user_id, measurement_date),
    UNIQUE KEY uk_user_date (user_id, measurement_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='身体指标表';

-- 4. 训练计划表
CREATE TABLE IF NOT EXISTS training_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    coach_id BIGINT NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    goal_type ENUM('WEIGHT_LOSS', 'FAT_LOSS', 'MUSCLE_GAIN') NOT NULL,
    target_value DECIMAL(10,2) COMMENT '目标值',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'ACTIVE',
    completion_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '完成率(%)',
    weekly_schedule JSON COMMENT '周训练安排',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_coach_id (coach_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (coach_id) REFERENCES users(id) ON DELETE CASCADE,
    CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练计划表';

-- 5. 成就表
CREATE TABLE IF NOT EXISTS achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    achievement_name VARCHAR(100) NOT NULL,
    description TEXT,
    achievement_type VARCHAR(50) NOT NULL COMMENT '成就类型',
    threshold_value DECIMAL(10,2) NOT NULL COMMENT '解锁阈值',
    icon_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (achievement_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';

-- 6. 用户成就表
CREATE TABLE IF NOT EXISTS user_achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_achievement_id (achievement_id),
    UNIQUE KEY uk_user_achievement (user_id, achievement_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表';

-- 7. 器材使用记录表
CREATE TABLE IF NOT EXISTS equipment_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    equipment_name VARCHAR(50) NOT NULL,
    usage_date DATE NOT NULL,
    usage_hour INT NOT NULL COMMENT '使用时段(0-23)',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    total_duration_minutes INT DEFAULT 0 COMMENT '总使用时长',
    INDEX idx_equipment (equipment_name),
    INDEX idx_date (usage_date),
    INDEX idx_hour (usage_hour),
    UNIQUE KEY uk_equipment_date_hour (equipment_name, usage_date, usage_hour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='器材使用记录表';

-- 8. 用户行为分析结果表
CREATE TABLE IF NOT EXISTS user_behavior_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    analysis_date DATE NOT NULL,
    most_popular_exercise VARCHAR(50),
    peak_hour_start INT COMMENT '高峰时段开始',
    peak_hour_end INT COMMENT '高峰时段结束',
    average_duration_minutes DECIMAL(10,2),
    active_user_count INT,
    exercise_type_distribution JSON COMMENT '运动类型分布',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analysis_date (analysis_date),
    UNIQUE KEY uk_analysis_date (analysis_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为分析结果表';

-- 8.1 教练待办处理记录表
CREATE TABLE IF NOT EXISTS coach_todo_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coach_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    todo_key VARCHAR(100) NOT NULL COMMENT '待办唯一键（同一教练-学员-待办维度唯一）',
    todo_title VARCHAR(100) NOT NULL COMMENT '待办标题',
    todo_description VARCHAR(255) COMMENT '待办描述',
    handled_at DATETIME NOT NULL COMMENT '处理时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_coach_id (coach_id),
    INDEX idx_student_id (student_id),
    UNIQUE KEY uk_coach_student_todo (coach_id, student_id, todo_key),
    FOREIGN KEY (coach_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练待办处理记录表';

-- 9. 排行榜表
CREATE TABLE IF NOT EXISTS leaderboards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    leaderboard_type ENUM('TOTAL_DURATION', 'TOTAL_CALORIES', 'WEIGHT_LOSS') NOT NULL,
    user_id BIGINT NOT NULL,
    `rank` INT NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (leaderboard_type),
    INDEX idx_user_id (user_id),
    INDEX idx_rank (`rank`),
    INDEX idx_period (period_start, period_end),
    UNIQUE KEY uk_type_user_period (leaderboard_type, user_id, period_start, period_end),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜表';

-- 插入默认成就数据
INSERT INTO achievements (achievement_name, description, achievement_type, threshold_value, icon_url) VALUES
('健身新手', '完成第1次运动', 'EXERCISE_COUNT', 1, '/icons/beginner.png'),
('坚持不懈', '连续运动7天', 'CONSECUTIVE_DAYS', 7, '/icons/persistent.png'),
('运动达人', '累计运动50次', 'EXERCISE_COUNT', 50, '/icons/expert.png'),
('卡路里杀手', '累计消耗10000卡路里', 'TOTAL_CALORIES', 10000, '/icons/calorie_killer.png'),
('马拉松勇士', '累计跑步100公里', 'RUNNING_DISTANCE', 100, '/icons/marathon.png'),
('减重冠军', '成功减重5kg', 'WEIGHT_LOSS', 5, '/icons/weight_loss.png'),
('体脂杀手', '体脂率降低5%', 'FAT_LOSS', 5, '/icons/fat_loss.png'),
('肌肉之王', '肌肉量增加3kg', 'MUSCLE_GAIN', 3, '/icons/muscle_king.png'),
('百日挑战', '累计运动100天', 'EXERCISE_COUNT', 100, '/icons/100days.png'),
('时长大师', '单次运动超过2小时', 'SINGLE_DURATION', 120, '/icons/duration_master.png');

-- 创建视图：用户运动统计
CREATE OR REPLACE VIEW v_user_exercise_stats AS
SELECT 
    u.id AS user_id,
    u.username,
    u.real_name,
    COUNT(er.id) AS total_exercises,
    SUM(er.duration_minutes) AS total_duration_minutes,
    SUM(er.calories_burned) AS total_calories_burned,
    AVG(er.duration_minutes) AS avg_duration_minutes,
    MAX(er.exercise_date) AS last_exercise_date
FROM users u
LEFT JOIN exercise_records er ON u.id = er.user_id
WHERE u.user_role = 'STUDENT'
GROUP BY u.id, u.username, u.real_name;

-- 创建视图：用户体重变化
CREATE OR REPLACE VIEW v_user_weight_change AS
SELECT 
    u.id AS user_id,
    u.username,
    u.real_name,
    (SELECT weight_kg FROM body_metrics WHERE user_id = u.id ORDER BY measurement_date ASC LIMIT 1) AS initial_weight,
    (SELECT weight_kg FROM body_metrics WHERE user_id = u.id ORDER BY measurement_date DESC LIMIT 1) AS current_weight,
    (SELECT weight_kg FROM body_metrics WHERE user_id = u.id ORDER BY measurement_date ASC LIMIT 1) - 
    (SELECT weight_kg FROM body_metrics WHERE user_id = u.id ORDER BY measurement_date DESC LIMIT 1) AS weight_change
FROM users u
WHERE u.user_role = 'STUDENT';

-- 创建存储过程：更新排行榜
DELIMITER //

CREATE PROCEDURE update_leaderboards(
    IN p_period_start DATE,
    IN p_period_end DATE
)
BEGIN
    -- 清空当前周期的排行榜
    DELETE FROM leaderboards 
    WHERE period_start = p_period_start AND period_end = p_period_end;
    
    -- 总运动时长排行榜
    INSERT INTO leaderboards (leaderboard_type, user_id, `rank`, value, period_start, period_end)
    SELECT 
        'TOTAL_DURATION',
        user_id,
        ROW_NUMBER() OVER (ORDER BY total_duration DESC) AS `rank`,
        total_duration,
        p_period_start,
        p_period_end
    FROM (
        SELECT 
            er.user_id,
            SUM(er.duration_minutes) AS total_duration
        FROM exercise_records er
        JOIN users u ON er.user_id = u.id
        WHERE er.exercise_date BETWEEN p_period_start AND p_period_end
            AND u.show_in_leaderboard = TRUE
        GROUP BY er.user_id
        HAVING total_duration > 0
    ) AS duration_stats
    ORDER BY total_duration DESC
    LIMIT 100;
    
    -- 总消耗卡路里排行榜
    INSERT INTO leaderboards (leaderboard_type, user_id, `rank`, value, period_start, period_end)
    SELECT 
        'TOTAL_CALORIES',
        user_id,
        ROW_NUMBER() OVER (ORDER BY total_calories DESC) AS `rank`,
        total_calories,
        p_period_start,
        p_period_end
    FROM (
        SELECT 
            er.user_id,
            SUM(er.calories_burned) AS total_calories
        FROM exercise_records er
        JOIN users u ON er.user_id = u.id
        WHERE er.exercise_date BETWEEN p_period_start AND p_period_end
            AND u.show_in_leaderboard = TRUE
        GROUP BY er.user_id
        HAVING total_calories > 0
    ) AS calorie_stats
    ORDER BY total_calories DESC
    LIMIT 100;
    
    -- 减重排行榜
    INSERT INTO leaderboards (leaderboard_type, user_id, `rank`, value, period_start, period_end)
    SELECT 
        'WEIGHT_LOSS',
        user_id,
        ROW_NUMBER() OVER (ORDER BY weight_loss DESC) AS `rank`,
        weight_loss,
        p_period_start,
        p_period_end
    FROM (
        SELECT 
            bm1.user_id,
            (bm1.weight_kg - bm2.weight_kg) AS weight_loss
        FROM (
            SELECT user_id, weight_kg
            FROM body_metrics
            WHERE measurement_date = (
                SELECT MIN(measurement_date) 
                FROM body_metrics bm 
                WHERE bm.user_id = body_metrics.user_id 
                    AND measurement_date >= p_period_start
            )
        ) bm1
        JOIN (
            SELECT user_id, weight_kg
            FROM body_metrics
            WHERE measurement_date = (
                SELECT MAX(measurement_date) 
                FROM body_metrics bm 
                WHERE bm.user_id = body_metrics.user_id 
                    AND measurement_date <= p_period_end
            )
        ) bm2 ON bm1.user_id = bm2.user_id
        JOIN users u ON bm1.user_id = u.id
        WHERE u.show_in_leaderboard = TRUE
        HAVING weight_loss > 0
    ) AS weight_stats
    ORDER BY weight_loss DESC
    LIMIT 100;
END //

DELIMITER ;

-- 创建事件：每日自动更新排行榜
-- SET GLOBAL event_scheduler = ON;
-- CREATE EVENT IF NOT EXISTS daily_leaderboard_update
-- ON SCHEDULE EVERY 1 DAY
-- STARTS CURRENT_DATE + INTERVAL 1 DAY
-- DO CALL update_leaderboards(CURRENT_DATE - INTERVAL 30 DAY, CURRENT_DATE);
