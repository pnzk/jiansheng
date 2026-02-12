import argparse
import csv
import os
import random
from datetime import datetime, timedelta


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUTPUT_DIR = os.path.join(PROJECT_ROOT, 'data-processing', 'cleaned')


LAST_NAMES = [
    '王', '李', '张', '刘', '陈', '杨', '赵', '黄', '周', '吴',
    '徐', '孙', '朱', '马', '胡', '郭', '何', '高', '林', '罗',
]

GIVEN_NAMES = [
    '伟', '芳', '娜', '敏', '静', '秀英', '丽', '强', '磊', '洋',
    '艳', '勇', '杰', '娟', '涛', '明', '超', '秀兰', '霞', '平',
    '刚', '桂英', '丹', '萍', '鹏', '华', '慧', '婷', '波', '媛',
    '斌', '楠', '鑫', '琪', '凯', '雪', '瑶', '晨', '宇', '涵',
]

EXERCISE_TYPES = ['跑步', '动感单车', '力量训练', '游泳', '瑜伽', '椭圆机', '划船机']
EQUIPMENTS = ['跑步机', '哑铃', '杠铃', '动感单车', '壶铃', '弹力带', '椭圆机', '划船机']
FITNESS_GOALS = ['减重', '减脂', '增肌']


def ensure_output_dir(path):
    os.makedirs(path, exist_ok=True)


def random_real_name():
    return random.choice(LAST_NAMES) + random.choice(GIVEN_NAMES)


def random_phone():
    return '1' + ''.join(random.choices('3456789', k=1)) + ''.join(random.choices('0123456789', k=9))


def iso_datetime(date_value):
    dt = datetime.combine(date_value, datetime.min.time()) + timedelta(
        hours=random.randint(6, 22),
        minutes=random.randint(0, 59),
        seconds=random.randint(0, 59),
    )
    return dt.isoformat()


def generate_users(user_count):
    users = []
    today = datetime.now().date()

    for user_id in range(1, user_count + 1):
        gender_cn = '男' if random.random() < 0.52 else '女'
        age = random.randint(18, 58)
        height_cm = round(random.uniform(150, 190), 1)

        if gender_cn == '男':
            initial_weight = round(random.uniform(58, 95), 1)
        else:
            initial_weight = round(random.uniform(45, 78), 1)

        created_days_ago = random.randint(10, 720)
        created_at = datetime.combine(today - timedelta(days=created_days_ago), datetime.min.time())

        users.append({
            'user_id': user_id,
            'username': f'user{user_id:05d}',
            'real_name': random_real_name(),
            'email': f'user{user_id:05d}@gym.local',
            'phone': random_phone(),
            'age': age,
            'gender': gender_cn,
            'height_cm': height_cm,
            'initial_weight_kg': initial_weight,
            'fitness_goal': random.choice(FITNESS_GOALS),
            'role': 'STUDENT',
            'created_at': created_at.isoformat(),
        })

    return users


def generate_exercise_records(users, target_rows):
    records = []
    today = datetime.now().date()

    per_user = max(1, target_rows // len(users))
    remainder = target_rows % len(users)
    record_id = 1

    for idx, user in enumerate(users):
        row_count = per_user + (1 if idx < remainder else 0)
        for _ in range(row_count):
            day_offset = random.randint(0, 359)
            exercise_date = today - timedelta(days=day_offset)
            duration = random.randint(20, 120)
            calories = round(duration * random.uniform(6.0, 12.5), 1)
            avg_hr = random.randint(105, 160)
            max_hr = min(195, avg_hr + random.randint(8, 35))

            records.append({
                'record_id': record_id,
                'user_id': user['user_id'],
                'exercise_type': random.choice(EXERCISE_TYPES),
                'exercise_date': exercise_date.isoformat(),
                'duration_minutes': duration,
                'calories_burned': calories,
                'average_heart_rate': avg_hr,
                'max_heart_rate': max_hr,
                'equipment_used': random.choice(EQUIPMENTS),
                'created_at': iso_datetime(exercise_date),
            })
            record_id += 1

    return records


def generate_body_metrics(users, target_rows):
    metrics = []
    today = datetime.now().date()
    metric_id = 1

    per_user = max(1, target_rows // len(users))
    remainder = target_rows % len(users)

    for idx, user in enumerate(users):
        row_count = per_user + (1 if idx < remainder else 0)

        base_weight = float(user['initial_weight_kg'])
        height = float(user['height_cm'])
        is_loss = user['fitness_goal'] in ('减重', '减脂')

        offsets = sorted(random.sample(range(0, 360), k=min(row_count, 360)))
        if len(offsets) < row_count:
            offsets += [random.randint(0, 359) for _ in range(row_count - len(offsets))]
            offsets = sorted(offsets)

        for day_offset in offsets:
            date_value = today - timedelta(days=day_offset)

            trend = random.uniform(-0.15, 0.15)
            if is_loss:
                trend -= random.uniform(0.02, 0.10)
            else:
                trend += random.uniform(0.00, 0.08)

            base_weight = max(40.0, round(base_weight + trend, 1))

            if user['gender'] == '男':
                body_fat = round(random.uniform(12.0, 25.0), 1)
                muscle_mass = round(base_weight * random.uniform(0.40, 0.50), 1)
            else:
                body_fat = round(random.uniform(18.0, 33.0), 1)
                muscle_mass = round(base_weight * random.uniform(0.32, 0.42), 1)

            bmi = round(base_weight / ((height / 100) ** 2), 2)

            metrics.append({
                'metric_id': metric_id,
                'user_id': user['user_id'],
                'measurement_date': date_value.isoformat(),
                'weight_kg': base_weight,
                'body_fat_percentage': body_fat,
                'height_cm': height,
                'bmi': bmi,
                'muscle_mass_kg': muscle_mass,
                'created_at': iso_datetime(date_value),
            })
            metric_id += 1

    return metrics


def generate_exercise_reference():
    rows = []
    templates = [
        ('跑步', '下肢', '跑步机', 'Beginner', '中等强度有氧训练，提升心肺功能。'),
        ('动感单车', '下肢', '动感单车', 'Intermediate', '高效燃脂课程，增强下肢耐力。'),
        ('力量训练', '全身', '哑铃', 'Intermediate', '提高肌力和基础代谢水平。'),
        ('游泳', '全身', '泳池', 'Intermediate', '低冲击全身有氧，适合长期坚持。'),
        ('瑜伽', '核心', '瑜伽垫', 'Beginner', '改善柔韧性与核心稳定性。'),
        ('椭圆机', '下肢', '椭圆机', 'Beginner', '关节友好型有氧训练。'),
        ('划船机', '背部', '划船机', 'Intermediate', '提升背部力量和心肺耐力。'),
    ]

    for index, (exercise_type, body_part, equipment, level, description) in enumerate(templates, start=1):
        rows.append({
            'exercise_name_en': f'Exercise_{index:02d}_{exercise_type}',
            'exercise_type': exercise_type,
            'body_part': body_part,
            'equipment': equipment,
            'level': level,
            'description': description,
        })

    return rows


def write_csv(path, fieldnames, rows):
    with open(path, 'w', newline='', encoding='utf-8-sig') as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def parse_args():
    parser = argparse.ArgumentParser(description='Generate realistic CSV templates for one-click import')
    parser.add_argument('--users', type=int, default=10000, help='Number of users to generate')
    parser.add_argument('--exercise-records', type=int, default=100000, help='Number of exercise records to generate')
    parser.add_argument('--body-metrics', type=int, default=50000, help='Number of body metric records to generate')
    parser.add_argument('--output-dir', type=str, default=OUTPUT_DIR, help='Output directory for CSV files')
    parser.add_argument('--seed', type=int, default=20260211, help='Random seed for reproducibility')
    return parser.parse_args()


def main():
    args = parse_args()
    random.seed(args.seed)

    ensure_output_dir(args.output_dir)

    print('=' * 72)
    print('  Gym Fitness - Generate Large Realistic CSV Data')
    print('=' * 72)
    print(f'Output directory: {args.output_dir}')
    print(f'Users: {args.users}, Exercise records: {args.exercise_records}, Body metrics: {args.body_metrics}')

    users = generate_users(args.users)
    exercise_records = generate_exercise_records(users, args.exercise_records)
    body_metrics = generate_body_metrics(users, args.body_metrics)
    exercise_reference = generate_exercise_reference()

    users_path = os.path.join(args.output_dir, 'users.csv')
    exercise_path = os.path.join(args.output_dir, 'exercise_records.csv')
    metrics_path = os.path.join(args.output_dir, 'body_metrics.csv')
    reference_path = os.path.join(args.output_dir, 'exercise_reference.csv')

    write_csv(
        users_path,
        [
            'user_id', 'username', 'real_name', 'email', 'phone', 'age', 'gender',
            'height_cm', 'initial_weight_kg', 'fitness_goal', 'role', 'created_at',
        ],
        users,
    )

    write_csv(
        exercise_path,
        [
            'record_id', 'user_id', 'exercise_type', 'exercise_date', 'duration_minutes',
            'calories_burned', 'average_heart_rate', 'max_heart_rate', 'equipment_used', 'created_at',
        ],
        exercise_records,
    )

    write_csv(
        metrics_path,
        [
            'metric_id', 'user_id', 'measurement_date', 'weight_kg', 'body_fat_percentage',
            'height_cm', 'bmi', 'muscle_mass_kg', 'created_at',
        ],
        body_metrics,
    )

    write_csv(
        reference_path,
        ['exercise_name_en', 'exercise_type', 'body_part', 'equipment', 'level', 'description'],
        exercise_reference,
    )

    print('\n[OK] CSV files generated:')
    for path in [users_path, exercise_path, metrics_path, reference_path]:
        print(f'  - {path}')
    print('=' * 72)


if __name__ == '__main__':
    main()

