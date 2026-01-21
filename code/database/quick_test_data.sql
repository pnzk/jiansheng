USE gym_fitness_analytics;

-- Add exercise records for user 1
INSERT INTO exercise_records (user_id, exercise_type, exercise_date, duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used, notes, created_at)
VALUES 
(1, 'RUNNING', CURDATE(), 30, 250, 120, 150, 'Treadmill', 'Good workout', NOW()),
(1, 'CYCLING', CURDATE() - INTERVAL 1 DAY, 40, 280, 115, 145, 'Bike', 'Cardio', NOW()),
(1, 'YOGA', CURDATE() - INTERVAL 2 DAY, 60, 150, 90, 110, 'Mat', 'Relaxing', NOW()),
(1, 'RUNNING', CURDATE() - INTERVAL 3 DAY, 35, 270, 125, 155, 'Treadmill', '', NOW()),
(1, 'SWIMMING', CURDATE() - INTERVAL 4 DAY, 50, 320, 110, 140, 'Pool', 'Swimming', NOW());

-- Add body metrics for user 1
INSERT INTO body_metrics (user_id, measurement_date, weight_kg, body_fat_percentage, height_cm, bmi, muscle_mass_kg, created_at)
VALUES 
(1, CURDATE(), 68.5, 18.5, 175, 22.4, 52.0, NOW()),
(1, CURDATE() - INTERVAL 7 DAY, 69.0, 19.0, 175, 22.5, 51.5, NOW()),
(1, CURDATE() - INTERVAL 14 DAY, 69.5, 19.5, 175, 22.7, 51.0, NOW()),
(1, CURDATE() - INTERVAL 21 DAY, 70.0, 20.0, 175, 22.9, 50.5, NOW());

-- Add training plan for user 1
INSERT INTO training_plans (student_id, coach_id, plan_name, goal_type, target_value, start_date, end_date, status, completion_rate, weekly_schedule, description, created_at, updated_at)
VALUES 
(1, 2, '30-Day Fat Loss Plan', 'FAT_LOSS', 5.0, 
 CURDATE() - INTERVAL 15 DAY, CURDATE() + INTERVAL 15 DAY, 
 'ACTIVE', 50.0, 
 '{"monday": "Cardio 30min", "wednesday": "Strength 45min", "friday": "Cardio 30min"}',
 'A 30-day fat loss plan combining cardio and strength training.',
 NOW(), NOW());

SELECT 'Data added successfully!' as message;
SELECT COUNT(*) as exercise_records FROM exercise_records WHERE user_id = 1;
SELECT COUNT(*) as body_metrics FROM body_metrics WHERE user_id = 1;
SELECT COUNT(*) as training_plans FROM training_plans WHERE student_id = 1;
