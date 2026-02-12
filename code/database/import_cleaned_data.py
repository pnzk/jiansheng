import os
import argparse
from datetime import datetime, timedelta

import bcrypt
import pandas as pd
import pymysql


DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '123456',
    'database': 'gym_fitness_analytics',
    'charset': 'utf8mb4',
    'autocommit': False,
}

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
CLEANED_DATA_DIR = os.path.join(PROJECT_ROOT, 'data-processing', 'cleaned')


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def read_csv(path):
    return pd.read_csv(path, encoding='utf-8-sig')


def to_safe_str(value):
    if pd.isna(value):
        return ''
    return str(value).strip()


def to_int(value, default=None):
    if pd.isna(value):
        return default
    try:
        return int(value)
    except Exception:
        return default


def to_float(value, default=None):
    if pd.isna(value):
        return default
    try:
        return float(value)
    except Exception:
        return default


def normalize_email(email, username):
    text = to_safe_str(email).lower()
    if not text:
        return f"{username}@gym.local"
    return text


def normalize_gender(value):
    mapping = {
        '男': 'MALE',
        '女': 'FEMALE',
        'MALE': 'MALE',
        'FEMALE': 'FEMALE',
    }
    return mapping.get(to_safe_str(value), 'MALE')


def normalize_goal(value):
    mapping = {
        '减重': 'WEIGHT_LOSS',
        '减脂': 'FAT_LOSS',
        '增肌': 'MUSCLE_GAIN',
        'WEIGHT_LOSS': 'WEIGHT_LOSS',
        'FAT_LOSS': 'FAT_LOSS',
        'MUSCLE_GAIN': 'MUSCLE_GAIN',
    }
    return mapping.get(to_safe_str(value), 'WEIGHT_LOSS')


def normalize_role(value):
    mapping = {
        'STUDENT': 'STUDENT',
        'COACH': 'COACH',
        'ADMIN': 'ADMIN',
        '学员': 'STUDENT',
        '教练': 'COACH',
        '管理员': 'ADMIN',
    }
    return mapping.get(to_safe_str(value), 'STUDENT')


def normalize_date(value):
    if pd.isna(value) or value is None:
        return datetime.now().strftime('%Y-%m-%d')
    text = to_safe_str(value)
    if not text:
        return datetime.now().strftime('%Y-%m-%d')
    if ' ' in text:
        text = text.split(' ')[0]
    return text


def normalize_datetime(value, fallback_date=None):
    if value is None or pd.isna(value):
        base = normalize_date(fallback_date) if fallback_date is not None else datetime.now().strftime('%Y-%m-%d')
        dt = datetime.strptime(base, '%Y-%m-%d')
        return dt + timedelta(hours=18)

    if isinstance(value, datetime):
        return value

    text = to_safe_str(value)
    if not text:
        base = normalize_date(fallback_date) if fallback_date is not None else datetime.now().strftime('%Y-%m-%d')
        dt = datetime.strptime(base, '%Y-%m-%d')
        return dt + timedelta(hours=18)

    text = text.replace('T', ' ')
    patterns = ('%Y-%m-%d %H:%M:%S', '%Y-%m-%d %H:%M', '%Y-%m-%d')
    for pattern in patterns:
        try:
            dt = datetime.strptime(text, pattern)
            if pattern == '%Y-%m-%d':
                dt = dt + timedelta(hours=18)
            return dt
        except ValueError:
            continue

    try:
        dt = datetime.fromisoformat(text)
        if dt.hour == 0 and dt.minute == 0 and dt.second == 0:
            dt = dt + timedelta(hours=18)
        return dt
    except ValueError:
        base = normalize_date(fallback_date) if fallback_date is not None else datetime.now().strftime('%Y-%m-%d')
        dt = datetime.strptime(base, '%Y-%m-%d')
        return dt + timedelta(hours=18)


def build_source_to_db_mapping(cursor):
    cursor.execute("SELECT id, username FROM users")
    return {row[1]: row[0] for row in cursor.fetchall()}


def import_users(conn, csv_path):
    print('\n[1/4] Importing users...')
    if not os.path.exists(csv_path):
        print(f'  [X] users.csv not found: {csv_path}')
        return 0, 0, 0, {}

    df = read_csv(csv_path)
    print(f'  Source rows: {len(df)}')

    cursor = conn.cursor()
    default_password = bcrypt.hashpw('123456'.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

    cursor.execute("SELECT email FROM users WHERE email IS NOT NULL AND email <> ''")
    used_emails = set((row[0] or '').strip().lower() for row in cursor.fetchall())

    inserted = 0
    updated = 0
    skipped = 0
    source_to_db = {}

    for _, row in df.iterrows():
        source_user_id = to_int(row.get('user_id'))
        username = to_safe_str(row.get('username', f'user_{source_user_id}'))
        if not username:
            skipped += 1
            continue

        cursor.execute('SELECT id FROM users WHERE username = %s', (username,))
        existing = cursor.fetchone()

        gender = normalize_gender(row.get('gender'))
        goal = normalize_goal(row.get('fitness_goal'))
        role = normalize_role(row.get('role'))
        age = to_int(row.get('age'), 25)
        real_name = to_safe_str(row.get('real_name'))[:50] or username
        phone = to_safe_str(row.get('phone'))[:20] or None
        email = normalize_email(row.get('email'), username)

        if existing is None:
            candidate_email = email
            if candidate_email in used_emails:
                suffix = 1
                while True:
                    candidate_email = f'{username}+{suffix}@gym.local'
                    if candidate_email not in used_emails:
                        break
                    suffix += 1
            used_emails.add(candidate_email)

            cursor.execute(
                """
                INSERT INTO users (
                    username, password, email, phone, real_name, age, gender,
                    user_role, fitness_goal, show_in_leaderboard, allow_coach_view,
                    created_at, updated_at
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
                """,
                (
                    username,
                    default_password,
                    candidate_email,
                    phone,
                    real_name,
                    age,
                    gender,
                    role,
                    goal,
                    1,
                    1,
                    datetime.now(),
                    datetime.now(),
                )
            )
            db_id = cursor.lastrowid
            inserted += 1
        else:
            db_id = existing[0]
            cursor.execute(
                """
                UPDATE users
                SET real_name=%s, phone=%s, age=%s, gender=%s,
                    user_role=%s, fitness_goal=%s, updated_at=%s
                WHERE id=%s
                """,
                (
                    real_name,
                    phone,
                    age,
                    gender,
                    role,
                    goal,
                    datetime.now(),
                    db_id,
                )
            )
            updated += 1

        if source_user_id is not None:
            source_to_db[source_user_id] = db_id

    conn.commit()
    print(f'  [OK] inserted={inserted}, updated={updated}, skipped={skipped}')
    return inserted, updated, skipped, source_to_db


def import_exercise_records(conn, csv_path, source_to_db):
    print('\n[2/4] Importing exercise_records...')
    if not os.path.exists(csv_path):
        print(f'  [X] exercise_records.csv not found: {csv_path}')
        return 0, 0

    df = read_csv(csv_path)
    print(f'  Source rows: {len(df)}')

    cursor = conn.cursor()
    inserted = 0
    skipped = 0

    for _, row in df.iterrows():
        source_user_id = to_int(row.get('user_id'))
        db_user_id = source_to_db.get(source_user_id)
        if db_user_id is None:
            skipped += 1
            continue

        try:
            cursor.execute(
                """
                INSERT INTO exercise_records (
                    user_id, exercise_type, exercise_date, duration_minutes,
                    calories_burned, average_heart_rate, max_heart_rate,
                    equipment_used, created_at
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)
                """,
                (
                    db_user_id,
                    to_safe_str(row.get('exercise_type'))[:50] or '跑步',
                    normalize_date(row.get('exercise_date')),
                    to_int(row.get('duration_minutes'), 30),
                    to_float(row.get('calories_burned'), 200.0),
                    to_int(row.get('average_heart_rate')),
                    to_int(row.get('max_heart_rate')),
                    to_safe_str(row.get('equipment_used'))[:50] or None,
                    normalize_datetime(row.get('created_at'), fallback_date=row.get('exercise_date')),
                )
            )
            inserted += 1
            if inserted % 2000 == 0:
                conn.commit()
                print(f'  imported {inserted} rows...')
        except Exception:
            skipped += 1

    conn.commit()
    print(f'  [OK] inserted={inserted}, skipped={skipped}')
    return inserted, skipped


def import_body_metrics(conn, csv_path, source_to_db):
    print('\n[3/4] Importing body_metrics...')
    if not os.path.exists(csv_path):
        print(f'  [X] body_metrics.csv not found: {csv_path}')
        return 0, 0

    df = read_csv(csv_path)
    print(f'  Source rows: {len(df)}')

    cursor = conn.cursor()
    inserted = 0
    skipped = 0

    for _, row in df.iterrows():
        source_user_id = to_int(row.get('user_id'))
        db_user_id = source_to_db.get(source_user_id)
        if db_user_id is None:
            skipped += 1
            continue

        measurement_date = normalize_date(row.get('measurement_date'))
        cursor.execute(
            "SELECT id FROM body_metrics WHERE user_id=%s AND measurement_date=%s",
            (db_user_id, measurement_date)
        )
        if cursor.fetchone():
            skipped += 1
            continue

        weight_kg = to_float(row.get('weight_kg'), 70.0)
        height_cm = to_float(row.get('height_cm'), 170.0)
        bmi = None
        if height_cm and height_cm > 0:
            bmi = round(weight_kg / ((height_cm / 100.0) ** 2), 2)

        try:
            cursor.execute(
                """
                INSERT INTO body_metrics (
                    user_id, measurement_date, weight_kg, body_fat_percentage,
                    height_cm, bmi, muscle_mass_kg, created_at
                ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
                """,
                (
                    db_user_id,
                    measurement_date,
                    weight_kg,
                    to_float(row.get('body_fat_percentage')),
                    height_cm,
                    bmi,
                    to_float(row.get('muscle_mass_kg')),
                    datetime.now(),
                )
            )
            inserted += 1
            if inserted % 2000 == 0:
                conn.commit()
                print(f'  imported {inserted} rows...')
        except Exception:
            skipped += 1

    conn.commit()
    print(f'  [OK] inserted={inserted}, skipped={skipped}')
    return inserted, skipped


def import_exercise_reference(conn, csv_path):
    print('\n[4/4] Importing exercise_reference...')
    if not os.path.exists(csv_path):
        print(f'  [WARN] exercise_reference.csv not found: {csv_path}')
        return 0, 0

    df = read_csv(csv_path)
    print(f'  Source rows: {len(df)}')

    cursor = conn.cursor()
    cursor.execute(
        """
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
        """
    )

    inserted = 0
    skipped = 0

    for _, row in df.iterrows():
        name = to_safe_str(row.get('exercise_name_en'))[:200]
        if not name:
            skipped += 1
            continue

        cursor.execute('SELECT id FROM exercise_reference WHERE exercise_name_en = %s', (name,))
        if cursor.fetchone():
            skipped += 1
            continue

        try:
            cursor.execute(
                """
                INSERT INTO exercise_reference (
                    exercise_name_en, exercise_type, body_part,
                    equipment, level, description
                ) VALUES (%s,%s,%s,%s,%s,%s)
                """,
                (
                    name,
                    to_safe_str(row.get('exercise_type'))[:50] or None,
                    to_safe_str(row.get('body_part'))[:50] or None,
                    to_safe_str(row.get('equipment'))[:100] or None,
                    to_safe_str(row.get('level'))[:50] or None,
                    to_safe_str(row.get('description')) or None,
                )
            )
            inserted += 1
        except Exception:
            skipped += 1

    conn.commit()
    print(f'  [OK] inserted={inserted}, skipped={skipped}')
    return inserted, skipped


def try_delete_table(cursor, table_name):
    try:
        cursor.execute(f'DELETE FROM {table_name}')
        return True
    except Exception:
        return False


def reset_target_tables(conn, reset_users=False):
    cursor = conn.cursor()
    print('\n[Reset] Clearing existing imported data tables...')

    deleted_tables = []
    cursor.execute('SET FOREIGN_KEY_CHECKS=0')
    try:
        base_tables = ['exercise_records', 'body_metrics', 'exercise_reference']
        if reset_users:
            base_tables = [
                'user_achievements',
                'training_plans',
                'leaderboards',
                'exercise_records',
                'body_metrics',
                'exercise_reference',
                'users',
            ]

        for table_name in base_tables:
            if try_delete_table(cursor, table_name):
                deleted_tables.append(table_name)
    finally:
        cursor.execute('SET FOREIGN_KEY_CHECKS=1')

    conn.commit()
    print(f"  [OK] Cleared tables: {', '.join(deleted_tables) if deleted_tables else 'none'}")


def parse_args():
    parser = argparse.ArgumentParser(description='Import cleaned CSV data into MySQL')
    parser.add_argument('--reset', action='store_true', help='Clear imported data tables before import')
    parser.add_argument('--reset-users', action='store_true', help='Clear users and related business tables before import')
    return parser.parse_args()


def main():
    args = parse_args()
    print('=' * 70)
    print('  Gym Fitness - Import Cleaned CSV Data')
    print('=' * 70)

    users_csv = os.path.join(CLEANED_DATA_DIR, 'users.csv')
    exercise_csv = os.path.join(CLEANED_DATA_DIR, 'exercise_records.csv')
    metrics_csv = os.path.join(CLEANED_DATA_DIR, 'body_metrics.csv')
    reference_csv = os.path.join(CLEANED_DATA_DIR, 'exercise_reference.csv')

    for file_path in [users_csv, exercise_csv, metrics_csv, reference_csv]:
        print(f'  - {file_path}: {"OK" if os.path.exists(file_path) else "MISSING"}')

    try:
        conn = get_connection()
    except Exception as e:
        print(f'\n[X] DB connection failed: {e}')
        return

    try:
        if args.reset or args.reset_users:
            reset_target_tables(conn, reset_users=args.reset_users)

        users_inserted, users_updated, users_skipped, source_to_db = import_users(conn, users_csv)
        ex_inserted, ex_skipped = import_exercise_records(conn, exercise_csv, source_to_db)
        bm_inserted, bm_skipped = import_body_metrics(conn, metrics_csv, source_to_db)
        ref_inserted, ref_skipped = import_exercise_reference(conn, reference_csv)

        print('\n' + '=' * 70)
        print('  Import summary')
        print('=' * 70)
        print(f'  users: inserted={users_inserted}, updated={users_updated}, skipped={users_skipped}')
        print(f'  exercise_records: inserted={ex_inserted}, skipped={ex_skipped}')
        print(f'  body_metrics: inserted={bm_inserted}, skipped={bm_skipped}')
        print(f'  exercise_reference: inserted={ref_inserted}, skipped={ref_skipped}')
        print('=' * 70)
    finally:
        conn.close()


if __name__ == '__main__':
    main()
