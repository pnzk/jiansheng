"""
初始化完整测试数据
确保所有API测试100%通过
"""
import pymysql
import bcrypt
from datetime import datetime, timedelta
import random

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '123456',
    'database': 'gym_fitness_analytics',
    'charset': 'utf8mb4'
}

def hash_password(password):
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

def main():
    print("="*60)
    print(" 初始化完整测试数据")
    print("="*60)
    
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    try:
        # 1. 创建测试用户
        print("\n1. 创建测试用户...")
        cursor.execute("DELETE FROM users WHERE username LIKE 'test_%'")
        
        hashed_pwd = hash_password('test123')
        
        # 学员
        cursor.execute("""
            INSERT INTO users (username, password, real_name, email, phone, age, gender, 
                             fitness_goal, user_role, show_in_leaderboard, allow_coach_view)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, ('test_student', hashed_pwd, '测试学员', 'student@test.com', '13800000001', 
              25, 'MALE', 'WEIGHT_LOSS', 'STUDENT', True, True))
        student_id = cursor.lastrowid
        print(f"   ✅ 创建学员: test_student (ID={student_id})")
        
        # 教练
        cursor.execute("""
            INSERT INTO users (username, password, real_name, email, phone, age, gender, 
                             fitness_goal, user_role, show_in_leaderboard, allow_coach_view)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, ('test_coach', hashed_pwd, '测试教练', 'coach@test.com', '13800000002', 
              30, 'MALE', None, 'COACH', True, True))
        coach_id = cursor.lastrowid
        print(f"   ✅ 创建教练: test_coach (ID={coach_id})")
        
        # 管理员
        cursor.execute("""
            INSERT INTO users (username, password, real_name, email, phone, age, gender, 
                             fitness_goal, user_role, show_in_leaderboard, allow_coach_view)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, ('test_admin', hashed_pwd, '测试管理员', 'admin@test.com', '13800000003', 
              35, 'MALE', None, 'ADMIN', True, True))
        admin_id = cursor.lastrowid
        print(f"   ✅ 创建管理员: test_admin (ID={admin_id})")
        
        # 更新学员的教练ID
        cursor.execute("UPDATE users SET coach_id = %s WHERE id = %s", (coach_id, student_id))
        
        # 2. 创建运动记录
        print("\n2. 创建运动记录...")
        cursor.execute("DELETE FROM exercise_records WHERE user_id = %s", (student_id,))
        
        exercise_types = ['跑步', '动感单车', '游泳', '力量训练', '瑜伽']
        equipment = ['跑步机', '动感单车', '哑铃', '杠铃', '瑜伽垫']
        
        for i in range(30):
            date = (datetime.now() - timedelta(days=i)).strftime('%Y-%m-%d')
            ex_type = random.choice(exercise_types)
            duration = random.randint(30, 90)
            calories = duration * random.randint(8, 12)
            avg_hr = random.randint(110, 150)
            max_hr = avg_hr + random.randint(20, 40)
            equip = random.choice(equipment)
            
            cursor.execute("""
                INSERT INTO exercise_records (user_id, exercise_type, exercise_date, 
                    duration_minutes, calories_burned, average_heart_rate, max_heart_rate, equipment_used)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """, (student_id, ex_type, date, duration, calories, avg_hr, max_hr, equip))
        print(f"   ✅ 创建30条运动记录")
        
        # 3. 创建身体指标
        print("\n3. 创建身体指标...")
        cursor.execute("DELETE FROM body_metrics WHERE user_id = %s", (student_id,))
        
        weight = 75.0
        body_fat = 22.0
        for i in range(12):
            date = (datetime.now() - timedelta(weeks=i)).strftime('%Y-%m-%d')
            weight -= random.uniform(0.2, 0.5)
            body_fat -= random.uniform(0.1, 0.3)
            height = 175.0
            bmi = weight / ((height/100) ** 2)
            muscle = weight * 0.4
            
            cursor.execute("""
                INSERT INTO body_metrics (user_id, measurement_date, weight_kg, 
                    body_fat_percentage, height_cm, bmi, muscle_mass_kg)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
            """, (student_id, date, round(weight, 2), round(body_fat, 2), 
                  height, round(bmi, 2), round(muscle, 2)))
        print(f"   ✅ 创建12条身体指标记录")
        
        # 4. 创建训练计划
        print("\n4. 创建训练计划...")
        cursor.execute("DELETE FROM training_plans WHERE student_id = %s", (student_id,))
        
        start_date = datetime.now().strftime('%Y-%m-%d')
        end_date = (datetime.now() + timedelta(days=90)).strftime('%Y-%m-%d')
        
        cursor.execute("""
            INSERT INTO training_plans (student_id, coach_id, plan_name, goal_type, 
                target_value, start_date, end_date, status, completion_rate, weekly_schedule)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (student_id, coach_id, '减脂塑形计划', 'WEIGHT_LOSS', 5.0, 
              start_date, end_date, 'ACTIVE', 35.0,
              '{"monday":["跑步30分钟","力量训练45分钟"],"wednesday":["游泳45分钟"],"friday":["动感单车40分钟","瑜伽30分钟"]}'))
        print(f"   ✅ 创建训练计划")
        
        # 5. 初始化成就
        print("\n5. 初始化成就...")
        cursor.execute("SELECT COUNT(*) FROM achievements")
        ach_count = cursor.fetchone()[0]
        if ach_count == 0:
            achievements = [
                ('健身新手', '完成第1次运动', 'EXERCISE_COUNT', 1),
                ('坚持不懈', '连续运动7天', 'CONSECUTIVE_DAYS', 7),
                ('运动达人', '累计运动50次', 'EXERCISE_COUNT', 50),
                ('卡路里杀手', '累计消耗10000卡路里', 'TOTAL_CALORIES', 10000),
                ('减重冠军', '成功减重5kg', 'WEIGHT_LOSS', 5),
                ('体脂杀手', '体脂率降低5%', 'FAT_LOSS', 5),
                ('肌肉之王', '肌肉量增加3kg', 'MUSCLE_GAIN', 3),
                ('百日挑战', '累计运动100天', 'EXERCISE_COUNT', 100),
                ('时长大师', '单次运动超过2小时', 'SINGLE_DURATION', 120),
            ]
            for a in achievements:
                cursor.execute("""
                    INSERT INTO achievements (achievement_name, description, achievement_type, threshold_value)
                    VALUES (%s, %s, %s, %s)
                """, a)
            print(f"   ✅ 创建{len(achievements)}个成就")
        else:
            print(f"   ✅ 成就已存在: {ach_count}个")
        
        # 6. 解锁用户成就
        print("\n6. 解锁用户成就...")
        cursor.execute("DELETE FROM user_achievements WHERE user_id = %s", (student_id,))
        cursor.execute("SELECT id FROM achievements WHERE threshold_value <= 30")
        unlocked = cursor.fetchall()
        for (ach_id,) in unlocked:
            cursor.execute("""
                INSERT INTO user_achievements (user_id, achievement_id, unlocked_at)
                VALUES (%s, %s, NOW())
            """, (student_id, ach_id))
        print(f"   ✅ 解锁{len(unlocked)}个成就")
        
        # 7. 生成排行榜数据
        print("\n7. 生成排行榜数据...")
        cursor.execute("DELETE FROM leaderboards")
        
        period_start = (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d')
        period_end = datetime.now().strftime('%Y-%m-%d')
        
        # 总运动时长排行
        cursor.execute("""
            INSERT INTO leaderboards (leaderboard_type, user_id, `rank`, value, period_start, period_end)
            SELECT 'TOTAL_DURATION', user_id, 
                   ROW_NUMBER() OVER (ORDER BY SUM(duration_minutes) DESC) as `rank`,
                   SUM(duration_minutes) as value,
                   %s, %s
            FROM exercise_records
            WHERE exercise_date BETWEEN %s AND %s
            GROUP BY user_id
            HAVING SUM(duration_minutes) > 0
            LIMIT 10
        """, (period_start, period_end, period_start, period_end))
        
        # 总卡路里排行
        cursor.execute("""
            INSERT INTO leaderboards (leaderboard_type, user_id, `rank`, value, period_start, period_end)
            SELECT 'TOTAL_CALORIES', user_id,
                   ROW_NUMBER() OVER (ORDER BY SUM(calories_burned) DESC) as `rank`,
                   SUM(calories_burned) as value,
                   %s, %s
            FROM exercise_records
            WHERE exercise_date BETWEEN %s AND %s
            GROUP BY user_id
            HAVING SUM(calories_burned) > 0
            LIMIT 10
        """, (period_start, period_end, period_start, period_end))
        
        cursor.execute("SELECT COUNT(*) FROM leaderboards")
        lb_count = cursor.fetchone()[0]
        print(f"   ✅ 生成{lb_count}条排行榜记录")
        
        # 8. 初始化器材使用数据
        print("\n8. 初始化器材使用数据...")
        cursor.execute("DELETE FROM equipment_usage")
        
        equipment_list = ['跑步机', '动感单车', '椭圆机', '划船机', '哑铃', '杠铃', '史密斯机']
        today = datetime.now().strftime('%Y-%m-%d')
        
        for eq_name in equipment_list:
            for hour in range(6, 23):
                usage_count = random.randint(5, 30)
                total_duration = usage_count * random.randint(20, 60)
                cursor.execute("""
                    INSERT INTO equipment_usage (equipment_name, usage_date, usage_hour, usage_count, total_duration_minutes)
                    VALUES (%s, %s, %s, %s, %s)
                """, (eq_name, today, hour, usage_count, total_duration))
        print(f"   ✅ 创建器材使用记录")
        
        conn.commit()
        
        # 打印统计
        print("\n" + "="*60)
        print(" 数据统计")
        print("="*60)
        
        tables = ['users', 'exercise_records', 'body_metrics', 'training_plans', 
                  'achievements', 'user_achievements', 'leaderboards', 'equipment_usage']
        for table in tables:
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            count = cursor.fetchone()[0]
            print(f"   {table}: {count}条")
        
        print("\n" + "="*60)
        print(" 测试账号")
        print("="*60)
        print("   用户名: test_student / test_coach / test_admin")
        print("   密码: test123")
        print("\n✅ 测试数据初始化完成！")
        
    except Exception as e:
        print(f"\n❌ 错误: {e}")
        conn.rollback()
        import traceback
        traceback.print_exc()
    finally:
        cursor.close()
        conn.close()

if __name__ == '__main__':
    main()
