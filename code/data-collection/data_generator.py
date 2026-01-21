"""
健身数据生成器
生成模拟的健身房用户数据、运动记录和身体指标数据
"""
import random
import json
import os
from datetime import datetime, timedelta
from faker import Faker

fake = Faker('zh_CN')

# 运动类型
EXERCISE_TYPES = [
    '跑步', '动感单车', '游泳', '力量训练', '瑜伽', 
    '普拉提', '椭圆机', '划船机', '爬楼机', '拳击'
]

# 器材类型
EQUIPMENT = [
    '跑步机', '动感单车', '哑铃', '杠铃', '史密斯机',
    '龙门架', '椭圆机', '划船机', '瑜伽垫', '拳击沙袋'
]

# 健身目标
FITNESS_GOALS = ['减重', '减脂', '增肌']

def generate_users(count=500):
    """生成用户数据"""
    users = []
    for i in range(1, count + 1):
        gender = random.choice(['男', '女'])
        age = random.randint(18, 55)
        
        # 根据性别生成合理的身高体重
        if gender == '男':
            height = random.uniform(165, 185)
            weight = random.uniform(60, 95)
        else:
            height = random.uniform(155, 175)
            weight = random.uniform(45, 75)
        
        user = {
            'user_id': i,
            'username': fake.user_name() + str(i),
            'real_name': fake.name(),
            'email': fake.email(),
            'phone': fake.phone_number(),
            'age': age,
            'gender': gender,
            'height_cm': round(height, 1),
            'initial_weight_kg': round(weight, 1),
            'fitness_goal': random.choice(FITNESS_GOALS),
            'role': 'STUDENT',
            'created_at': (datetime.now() - timedelta(days=random.randint(30, 365))).isoformat()
        }
        users.append(user)
    
    # 添加教练
    for i in range(count + 1, count + 21):
        user = {
            'user_id': i,
            'username': f'coach{i}',
            'real_name': fake.name(),
            'email': fake.email(),
            'phone': fake.phone_number(),
            'age': random.randint(25, 45),
            'gender': random.choice(['男', '女']),
            'role': 'COACH',
            'created_at': (datetime.now() - timedelta(days=random.randint(180, 730))).isoformat()
        }
        users.append(user)
    
    # 添加管理员
    users.append({
        'user_id': count + 21,
        'username': 'admin',
        'real_name': '系统管理员',
        'email': 'admin@gym.com',
        'phone': '13800138000',
        'age': 35,
        'gender': '男',
        'role': 'ADMIN',
        'created_at': (datetime.now() - timedelta(days=730)).isoformat()
    })
    
    return users

def generate_exercise_records(users, records_per_user=50):
    """生成运动记录"""
    records = []
    record_id = 1
    
    students = [u for u in users if u['role'] == 'STUDENT']
    
    for user in students:
        user_id = user['user_id']
        created_date = datetime.fromisoformat(user['created_at'])
        
        # 生成该用户的运动记录
        num_records = random.randint(records_per_user - 20, records_per_user + 20)
        
        for _ in range(num_records):
            exercise_type = random.choice(EXERCISE_TYPES)
            equipment = random.choice(EQUIPMENT)
            
            # 运动日期在用户注册后
            days_since_created = (datetime.now() - created_date).days
            exercise_date = created_date + timedelta(days=random.randint(0, days_since_created))
            
            # 运动时长（分钟）
            duration = random.randint(20, 120)
            
            # 卡路里消耗（基于时长和运动类型）
            base_calories = {
                '跑步': 10, '动感单车': 12, '游泳': 11, '力量训练': 8,
                '瑜伽': 4, '普拉提': 5, '椭圆机': 9, '划船机': 10,
                '爬楼机': 11, '拳击': 13
            }
            calories = duration * base_calories.get(exercise_type, 8) * random.uniform(0.9, 1.1)
            
            # 心率
            avg_heart_rate = random.randint(110, 150)
            max_heart_rate = avg_heart_rate + random.randint(10, 30)
            
            record = {
                'record_id': record_id,
                'user_id': user_id,
                'exercise_type': exercise_type,
                'exercise_date': exercise_date.date().isoformat(),
                'duration_minutes': duration,
                'calories_burned': round(calories, 1),
                'average_heart_rate': avg_heart_rate,
                'max_heart_rate': max_heart_rate,
                'equipment_used': equipment,
                'created_at': exercise_date.isoformat()
            }
            records.append(record)
            record_id += 1
    
    return records

def generate_body_metrics(users, metrics_per_user=20):
    """生成身体指标数据"""
    metrics = []
    metric_id = 1
    
    students = [u for u in users if u['role'] == 'STUDENT']
    
    for user in students:
        user_id = user['user_id']
        created_date = datetime.fromisoformat(user['created_at'])
        initial_weight = user['initial_weight_kg']
        height = user['height_cm']
        goal = user['fitness_goal']
        
        # 生成该用户的身体指标记录
        num_metrics = random.randint(metrics_per_user - 5, metrics_per_user + 5)
        
        current_weight = initial_weight
        current_body_fat = random.uniform(18, 30) if user['gender'] == '男' else random.uniform(22, 35)
        
        for i in range(num_metrics):
            days_since_created = (datetime.now() - created_date).days
            measurement_date = created_date + timedelta(days=int(i * days_since_created / num_metrics))
            
            # 根据健身目标模拟体重变化
            if goal == '减重' or goal == '减脂':
                weight_change = random.uniform(-0.3, 0.1)
                body_fat_change = random.uniform(-0.2, 0.05)
            else:  # 增肌
                weight_change = random.uniform(-0.1, 0.3)
                body_fat_change = random.uniform(-0.15, 0.1)
            
            current_weight += weight_change
            current_body_fat += body_fat_change
            
            # 确保数值在合理范围内
            current_weight = max(40, min(120, current_weight))
            current_body_fat = max(10, min(40, current_body_fat))
            
            # 计算BMI
            bmi = current_weight / ((height / 100) ** 2)
            
            # 肌肉量
            muscle_mass = current_weight * (1 - current_body_fat / 100) * random.uniform(0.4, 0.5)
            
            metric = {
                'metric_id': metric_id,
                'user_id': user_id,
                'measurement_date': measurement_date.date().isoformat(),
                'weight_kg': round(current_weight, 1),
                'body_fat_percentage': round(current_body_fat, 1),
                'height_cm': height,
                'bmi': round(bmi, 1),
                'muscle_mass_kg': round(muscle_mass, 1),
                'created_at': measurement_date.isoformat()
            }
            metrics.append(metric)
            metric_id += 1
    
    return metrics

def save_to_json(data, filename):
    """保存数据到JSON文件"""
    # 确保目录存在
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f'已保存 {len(data)} 条记录到 {filename}')

def main():
    print('开始生成健身数据...')
    
    # 生成数据
    users = generate_users(500)
    exercise_records = generate_exercise_records(users, records_per_user=50)
    body_metrics = generate_body_metrics(users, metrics_per_user=20)
    
    # 保存数据
    save_to_json(users, 'data-collection/output/users.json')
    save_to_json(exercise_records, 'data-collection/output/exercise_records.json')
    save_to_json(body_metrics, 'data-collection/output/body_metrics.json')
    
    print(f'\n数据生成完成！')
    print(f'用户数: {len(users)}')
    print(f'运动记录数: {len(exercise_records)}')
    print(f'身体指标记录数: {len(body_metrics)}')
    print(f'\n注意：这是模拟数据，实际使用时需要与真实爬取的数据混合，确保模拟数据<30%')

if __name__ == '__main__':
    main()
