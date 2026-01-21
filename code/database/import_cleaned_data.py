"""
将清洗后的CSV数据导入MySQL数据库
"""
import pandas as pd
import pymysql
from datetime import datetime
import os
import bcrypt

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '123456',
    'database': 'gym_fitness_analytics',
    'charset': 'utf8mb4'
}

# 获取脚本所在目录
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
CLEANED_DATA_DIR = os.path.join(PROJECT_ROOT, 'data-processing', 'cleaned')

def get_connection():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG)

def hash_password(password):
    """生成BCrypt密码哈希"""
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

def import_users(conn, csv_path):
    """导入用户数据"""
    print("\n[1/3] 导入用户数据...")
    
    if not os.path.exists(csv_path):
        print(f"  ✗ 文件不存在: {csv_path}")
        return 0
    
    df = pd.read_csv(csv_path)
    print(f"  读取到 {len(df)} 条用户记录")
    
    cursor = conn.cursor()
    
    # 默认密码
    default_password = hash_password('123456')
    
    # 性别映射
    gender_map = {'男': 'MALE', '女': 'FEMALE', 'MALE': 'MALE', 'FEMALE': 'FEMALE'}
    
    # 健身目标映射
    goal_map = {
        '减重': 'WEIGHT_LOSS', 
        '减脂': 'FAT_LOSS', 
        '增肌': 'MUSCLE_GAIN',
        'WEIGHT_LOSS': 'WEIGHT_LOSS',
        'FAT_LOSS': 'FAT_LOSS',
        'MUSCLE_GAIN': 'MUSCLE_GAIN'
    }
    
    # 角色映射
    role_map = {
        'STUDENT': 'STUDENT',
        'COACH': 'COACH',
        'ADMIN': 'ADMIN',
        '学员': 'STUDENT',
        '教练': 'COACH',
        '管理员': 'ADMIN'
    }
    
    inserted = 0
    skipped = 0
    
    for _, row in df.iterrows():
        try:
            username = str(row.get('username', f"user_{row['user_id']}"))
            email = str(row.get('email', f"{username}@gym.com"))
            
            # 检查用户是否已存在
            cursor.execute("SELECT id FROM users WHERE username = %s OR email = %s", (username, email))
            if cursor.fetchone():
                skipped += 1
                continue
            
            gender = gender_map.get(str(row.get('gender', '')), 'MALE')
            fitness_goal = goal_map.get(str(row.get('fitness_goal', '')), 'WEIGHT_LOSS')
            role = role_map.get(str(row.get('role', 'STUDENT')), 'STUDENT')
            
            sql = """
                INSERT INTO users (username, password, email, phone, real_name, age, gender, 
                                   user_role, fitness_goal, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            cursor.execute(sql, (
                username,
                default_password,
                email,
                str(row.get('phone', ''))[:20],
                str(row.get('real_name', ''))[:50],
                int(row.get('age', 25)) if pd.notna(row.get('age')) else 25,
                gender,
                role,
                fitness_goal,
                datetime.now()
            ))
            inserted += 1
            
        except Exception as e:
            print(f"  ⚠ 跳过用户 {row.get('username', 'unknown')}: {e}")
            skipped += 1
    
    conn.commit()
    print(f"  ✓ 成功导入 {inserted} 条用户记录，跳过 {skipped} 条")
    return inserted

def import_exercise_records(conn, csv_path):
    """导入运动记录数据"""
    print("\n[2/3] 导入运动记录...")
    
    if not os.path.exists(csv_path):
        print(f"  ✗ 文件不存在: {csv_path}")
        return 0
    
    df = pd.read_csv(csv_path)
    print(f"  读取到 {len(df)} 条运动记录")
    
    cursor = conn.cursor()
    
    # 获取所有用户ID
    cursor.execute("SELECT id FROM users")
    valid_user_ids = set(row[0] for row in cursor.fetchall())
    
    if not valid_user_ids:
        print("  ⚠ 数据库中没有用户，请先导入用户数据")
        return 0
    
    inserted = 0
    skipped = 0
    
    for _, row in df.iterrows():
        try:
            user_id = int(row.get('user_id', 1))
            
            # 如果用户ID不存在，随机分配一个有效的用户ID
            if user_id not in valid_user_ids:
                user_id = list(valid_user_ids)[hash(str(row.get('record_id', 0))) % len(valid_user_ids)]
            
            exercise_date = row.get('exercise_date', datetime.now().strftime('%Y-%m-%d'))
            if pd.isna(exercise_date):
                exercise_date = datetime.now().strftime('%Y-%m-%d')
            
            sql = """
                INSERT INTO exercise_records (user_id, exercise_type, exercise_date, 
                                              duration_minutes, calories_burned, 
                                              average_heart_rate, max_heart_rate, 
                                              equipment_used, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            cursor.execute(sql, (
                user_id,
                str(row.get('exercise_type', '跑步'))[:50],
                exercise_date,
                int(row.get('duration_minutes', 30)) if pd.notna(row.get('duration_minutes')) else 30,
                float(row.get('calories_burned', 200)) if pd.notna(row.get('calories_burned')) else 200,
                int(row.get('average_heart_rate', 120)) if pd.notna(row.get('average_heart_rate')) else None,
                int(row.get('max_heart_rate', 150)) if pd.notna(row.get('max_heart_rate')) else None,
                str(row.get('equipment_used', ''))[:50] if pd.notna(row.get('equipment_used')) else None,
                datetime.now()
            ))
            inserted += 1
            
            # 每1000条提交一次
            if inserted % 1000 == 0:
                conn.commit()
                print(f"  已导入 {inserted} 条...")
            
        except Exception as e:
            skipped += 1
            if skipped <= 5:
                print(f"  ⚠ 跳过记录: {e}")
    
    conn.commit()
    print(f"  ✓ 成功导入 {inserted} 条运动记录，跳过 {skipped} 条")
    return inserted

def import_body_metrics(conn, csv_path):
    """导入身体指标数据"""
    print("\n[3/3] 导入身体指标...")
    
    if not os.path.exists(csv_path):
        print(f"  ✗ 文件不存在: {csv_path}")
        return 0
    
    df = pd.read_csv(csv_path)
    print(f"  读取到 {len(df)} 条身体指标记录")
    
    cursor = conn.cursor()
    
    # 获取所有用户ID
    cursor.execute("SELECT id FROM users")
    valid_user_ids = set(row[0] for row in cursor.fetchall())
    
    if not valid_user_ids:
        print("  ⚠ 数据库中没有用户，请先导入用户数据")
        return 0
    
    inserted = 0
    skipped = 0
    
    for _, row in df.iterrows():
        try:
            user_id = int(row.get('user_id', 1))
            
            # 如果用户ID不存在，随机分配一个有效的用户ID
            if user_id not in valid_user_ids:
                user_id = list(valid_user_ids)[hash(str(row.get('metric_id', 0))) % len(valid_user_ids)]
            
            measurement_date = row.get('measurement_date', datetime.now().strftime('%Y-%m-%d'))
            if pd.isna(measurement_date):
                measurement_date = datetime.now().strftime('%Y-%m-%d')
            
            # 检查是否已存在相同用户和日期的记录
            cursor.execute(
                "SELECT id FROM body_metrics WHERE user_id = %s AND measurement_date = %s",
                (user_id, measurement_date)
            )
            if cursor.fetchone():
                skipped += 1
                continue
            
            weight_kg = float(row.get('weight_kg', 70)) if pd.notna(row.get('weight_kg')) else 70
            height_cm = float(row.get('height_cm', 170)) if pd.notna(row.get('height_cm')) else 170
            bmi = weight_kg / ((height_cm / 100) ** 2) if height_cm > 0 else None
            
            sql = """
                INSERT INTO body_metrics (user_id, measurement_date, weight_kg, 
                                          body_fat_percentage, height_cm, bmi, 
                                          muscle_mass_kg, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """
            
            cursor.execute(sql, (
                user_id,
                measurement_date,
                weight_kg,
                float(row.get('body_fat_percentage', 20)) if pd.notna(row.get('body_fat_percentage')) else None,
                height_cm,
                round(bmi, 2) if bmi else None,
                float(row.get('muscle_mass_kg', 30)) if pd.notna(row.get('muscle_mass_kg')) else None,
                datetime.now()
            ))
            inserted += 1
            
            # 每1000条提交一次
            if inserted % 1000 == 0:
                conn.commit()
                print(f"  已导入 {inserted} 条...")
            
        except Exception as e:
            skipped += 1
            if skipped <= 5:
                print(f"  ⚠ 跳过记录: {e}")
    
    conn.commit()
    print(f"  ✓ 成功导入 {inserted} 条身体指标记录，跳过 {skipped} 条")
    return inserted

def import_exercise_reference(conn, csv_path):
    """导入运动参考数据"""
    print("\n[4/4] 导入运动参考数据...")
    
    if not os.path.exists(csv_path):
        print(f"  ✗ 文件不存在: {csv_path}")
        return 0
    
    df = pd.read_csv(csv_path)
    print(f"  读取到 {len(df)} 条运动参考记录")
    
    cursor = conn.cursor()
    
    # 检查表是否存在，不存在则创建
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS exercise_reference (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            exercise_name_en VARCHAR(200),
            exercise_type VARCHAR(50),
            body_part VARCHAR(50),
            equipment VARCHAR(100),
            level VARCHAR(50),
            description TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """)
    
    inserted = 0
    skipped = 0
    
    for _, row in df.iterrows():
        try:
            exercise_name = str(row.get('exercise_name_en', ''))[:200]
            
            # 检查是否已存在
            cursor.execute("SELECT id FROM exercise_reference WHERE exercise_name_en = %s", (exercise_name,))
            if cursor.fetchone():
                skipped += 1
                continue
            
            sql = """
                INSERT INTO exercise_reference (exercise_name_en, exercise_type, body_part, 
                                                equipment, level, description)
                VALUES (%s, %s, %s, %s, %s, %s)
            """
            
            cursor.execute(sql, (
                exercise_name,
                str(row.get('exercise_type', ''))[:50],
                str(row.get('body_part', ''))[:50],
                str(row.get('equipment', ''))[:100],
                str(row.get('level', ''))[:50],
                str(row.get('description', '')) if pd.notna(row.get('description')) else None
            ))
            inserted += 1
            
        except Exception as e:
            skipped += 1
            if skipped <= 5:
                print(f"  ⚠ 跳过记录: {e}")
    
    conn.commit()
    print(f"  ✓ 成功导入 {inserted} 条运动参考记录，跳过 {skipped} 条")
    return inserted

def main():
    print("=" * 60)
    print("  健身管理系统 - 导入清洗后的数据")
    print("=" * 60)
    
    print(f"\n数据目录: {CLEANED_DATA_DIR}")
    
    # 检查数据文件
    users_csv = os.path.join(CLEANED_DATA_DIR, 'users.csv')
    exercise_csv = os.path.join(CLEANED_DATA_DIR, 'exercise_records.csv')
    metrics_csv = os.path.join(CLEANED_DATA_DIR, 'body_metrics.csv')
    reference_csv = os.path.join(CLEANED_DATA_DIR, 'exercise_reference.csv')
    
    files_exist = True
    for f, name in [(users_csv, '用户数据'), (exercise_csv, '运动记录'), (metrics_csv, '身体指标'), (reference_csv, '运动参考')]:
        if os.path.exists(f):
            print(f"  ✓ {name}: {f}")
        else:
            print(f"  ✗ {name}: 文件不存在")
            if name != '运动参考':
                files_exist = False
    
    if not files_exist:
        print("\n请先运行数据清洗脚本生成数据文件")
        return
    
    # 连接数据库
    try:
        conn = get_connection()
        print("\n✓ 数据库连接成功")
    except Exception as e:
        print(f"\n✗ 数据库连接失败: {e}")
        return
    
    try:
        # 导入数据
        users_count = import_users(conn, users_csv)
        exercise_count = import_exercise_records(conn, exercise_csv)
        metrics_count = import_body_metrics(conn, metrics_csv)
        reference_count = import_exercise_reference(conn, reference_csv)
        
        print("\n" + "=" * 60)
        print("  导入完成！")
        print("=" * 60)
        print(f"  用户数据: {users_count} 条")
        print(f"  运动记录: {exercise_count} 条")
        print(f"  身体指标: {metrics_count} 条")
        print(f"  运动参考: {reference_count} 条")
        print("=" * 60)
        
    finally:
        conn.close()

if __name__ == '__main__':
    main()
