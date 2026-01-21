"""创建测试账号"""
import subprocess
import sys

# 安装依赖
subprocess.run([sys.executable, '-m', 'pip', 'install', 'pymysql', 'bcrypt', '-q'], check=True)

import pymysql
import bcrypt

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '123456',
    'database': 'gym_fitness_analytics',
    'charset': 'utf8mb4'
}

# 生成BCrypt密码
def hash_password(password):
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

# 测试账号
TEST_USERS = [
    {
        'username': 'test_student',
        'password': 'test123',
        'real_name': '测试学员',
        'email': 'student@test.com',
        'phone': '13800000001',
        'age': 25,
        'gender': '男',
        'height_cm': 175,
        'initial_weight_kg': 70,
        'fitness_goal': '减重',
        'role': 'STUDENT'
    },
    {
        'username': 'test_coach',
        'password': 'test123',
        'real_name': '测试教练',
        'email': 'coach@test.com',
        'phone': '13800000002',
        'age': 30,
        'gender': '男',
        'height_cm': 180,
        'initial_weight_kg': 75,
        'fitness_goal': None,
        'role': 'COACH'
    },
    {
        'username': 'test_admin',
        'password': 'test123',
        'real_name': '测试管理员',
        'email': 'admin@test.com',
        'phone': '13800000003',
        'age': 35,
        'gender': '男',
        'height_cm': 178,
        'initial_weight_kg': 72,
        'fitness_goal': None,
        'role': 'ADMIN'
    }
]

def main():
    print("连接数据库...")
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    try:
        # 删除已存在的测试账号
        print("清理旧的测试账号...")
        cursor.execute("DELETE FROM users WHERE username IN ('test_student', 'test_coach', 'test_admin')")
        
        # 插入新的测试账号
        print("创建测试账号...")
        for user in TEST_USERS:
            hashed_pwd = hash_password(user['password'])
            
            sql = """
            INSERT INTO users (username, password, real_name, email, phone, age, gender, 
                             height_cm, initial_weight_kg, fitness_goal, role, created_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
            """
            cursor.execute(sql, (
                user['username'], hashed_pwd, user['real_name'], user['email'],
                user['phone'], user['age'], user['gender'], user['height_cm'],
                user['initial_weight_kg'], user['fitness_goal'], user['role']
            ))
            print(f"  ✅ 创建账号: {user['username']} ({user['role']})")
        
        conn.commit()
        
        # 验证
        cursor.execute("SELECT username, role FROM users WHERE username LIKE 'test_%'")
        results = cursor.fetchall()
        print(f"\n已创建 {len(results)} 个测试账号:")
        for row in results:
            print(f"  - {row[0]} ({row[1]})")
        
        print("\n测试账号信息:")
        print("  用户名: test_student / test_coach / test_admin")
        print("  密码: test123")
        
    except Exception as e:
        print(f"错误: {e}")
        conn.rollback()
    finally:
        cursor.close()
        conn.close()

if __name__ == '__main__':
    main()
