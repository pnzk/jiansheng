import random
from datetime import date, datetime, time, timedelta

import pymysql


DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "gym_fitness_analytics",
    "charset": "utf8mb4",
    "autocommit": False,
}


RANDOM_SEED = 20260518
random.seed(RANDOM_SEED)

EXERCISE_TYPES = [
    "跑步", "动感单车", "力量训练", "HIIT", "游泳", "瑜伽", "椭圆机", "划船机"
]

EQUIPMENT_BY_TYPE = {
    "跑步": "跑步机",
    "动感单车": "动感单车",
    "力量训练": "哑铃",
    "HIIT": "无器材",
    "游泳": "泳池",
    "瑜伽": "瑜伽垫",
    "椭圆机": "椭圆机",
    "划船机": "划船机",
}


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def build_in_clause(values):
    return ",".join(["%s"] * len(values))


def load_existing_exercise_dates(cursor, student_ids, start_day, end_day):
    if not student_ids:
        return set()

    sql = f"""
        SELECT user_id, exercise_date
        FROM exercise_records
        WHERE user_id IN ({build_in_clause(student_ids)})
          AND exercise_date BETWEEN %s AND %s
    """
    cursor.execute(sql, (*student_ids, start_day, end_day))
    return {(row[0], row[1]) for row in cursor.fetchall()}


def seed_recent_exercise_records(cursor, student_ids):
    start_day = date.today() - timedelta(days=29)
    end_day = date.today()
    existing_dates = load_existing_exercise_dates(cursor, student_ids, start_day, end_day)
    rows_to_insert = []

    for student_id in student_ids:
        active_days = random.sample(range(30), k=random.randint(8, 16))
        for offset in active_days:
            exercise_day = start_day + timedelta(days=offset)
            if (student_id, exercise_day) in existing_dates:
                continue

            exercise_type = random.choice(EXERCISE_TYPES)
            duration = random.randint(30, 90)
            calories = int(duration * random.uniform(6.0, 10.5))
            avg_hr = random.randint(95, 155)
            max_hr = min(avg_hr + random.randint(10, 35), 195)
            created_at = datetime.combine(
                exercise_day,
                time(hour=random.randint(6, 22), minute=random.randint(0, 59), second=random.randint(0, 59)),
            )

            rows_to_insert.append(
                (
                    student_id,
                    exercise_type,
                    exercise_day,
                    duration,
                    calories,
                    avg_hr,
                    max_hr,
                    EQUIPMENT_BY_TYPE[exercise_type],
                    created_at,
                )
            )
            existing_dates.add((student_id, exercise_day))

    if rows_to_insert:
        cursor.executemany(
            """
            INSERT INTO exercise_records (
                user_id, exercise_type, exercise_date, duration_minutes,
                calories_burned, average_heart_rate, max_heart_rate,
                equipment_used, created_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """,
            rows_to_insert,
        )

    return len(rows_to_insert)


def load_latest_metrics(cursor, student_ids):
    if not student_ids:
        return {}

    sql = f"""
        SELECT bm.user_id, bm.weight_kg, bm.body_fat_percentage, bm.height_cm, bm.muscle_mass_kg
        FROM body_metrics bm
        INNER JOIN (
            SELECT user_id, MAX(measurement_date) AS latest_date
            FROM body_metrics
            WHERE user_id IN ({build_in_clause(student_ids)})
            GROUP BY user_id
        ) latest
            ON latest.user_id = bm.user_id
           AND latest.latest_date = bm.measurement_date
    """
    cursor.execute(sql, tuple(student_ids))

    latest_map = {}
    for row in cursor.fetchall():
        user_id = row[0]
        if user_id not in latest_map:
            latest_map[user_id] = row[1:]
    return latest_map


def load_existing_metric_dates(cursor, student_ids, metric_days):
    if not student_ids or not metric_days:
        return set()

    sql = f"""
        SELECT user_id, measurement_date
        FROM body_metrics
        WHERE user_id IN ({build_in_clause(student_ids)})
          AND measurement_date IN ({build_in_clause(metric_days)})
    """
    cursor.execute(sql, (*student_ids, *metric_days))
    return {(row[0], row[1]) for row in cursor.fetchall()}


def seed_recent_body_metrics(cursor, student_ids):
    start_day = date.today() - timedelta(days=29)
    metric_days = [start_day + timedelta(days=offset) for offset in (0, 9, 19, 29)]
    latest_metrics_map = load_latest_metrics(cursor, student_ids)
    existing_metric_dates = load_existing_metric_dates(cursor, student_ids, metric_days)
    rows_to_insert = []

    for student_id in student_ids:
        latest = latest_metrics_map.get(student_id)
        if latest:
            base_weight = float(latest[0] or 70.0)
            base_fat = float(latest[1] or 22.0)
            base_height = float(latest[2] or 170.0)
            base_muscle = float(latest[3] or max(base_weight * 0.42, 20.0))
        else:
            base_weight = random.uniform(55, 85)
            base_fat = random.uniform(14, 28)
            base_height = random.uniform(158, 182)
            base_muscle = max(base_weight * 0.42, 20.0)

        for idx, measure_day in enumerate(metric_days):
            if (student_id, measure_day) in existing_metric_dates:
                continue

            trend_weight = base_weight + random.uniform(0.4, 2.4) - idx * random.uniform(0.3, 0.8)
            trend_fat = max(base_fat + random.uniform(0.2, 1.2) - idx * random.uniform(0.2, 0.6), 5.0)
            trend_muscle = max(base_muscle + random.uniform(-0.5, 0.5) + idx * random.uniform(0.0, 0.4), 18.0)
            bmi = round(trend_weight / ((base_height / 100.0) ** 2), 2)

            rows_to_insert.append(
                (
                    student_id,
                    measure_day,
                    round(trend_weight, 2),
                    round(trend_fat, 2),
                    round(base_height, 2),
                    bmi,
                    round(trend_muscle, 2),
                )
            )
            existing_metric_dates.add((student_id, measure_day))

    if rows_to_insert:
        cursor.executemany(
            """
            INSERT INTO body_metrics (
                user_id, measurement_date, weight_kg, body_fat_percentage,
                height_cm, bmi, muscle_mass_kg, created_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,NOW())
            ON DUPLICATE KEY UPDATE
                weight_kg = VALUES(weight_kg),
                body_fat_percentage = VALUES(body_fat_percentage),
                height_cm = VALUES(height_cm),
                bmi = VALUES(bmi),
                muscle_mass_kg = VALUES(muscle_mass_kg)
            """,
            rows_to_insert,
        )

    return len(rows_to_insert)


def main():
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                SELECT id
                FROM users
                WHERE user_role='STUDENT'
                ORDER BY id
                LIMIT 400
                """
            )
            student_ids = [row[0] for row in cursor.fetchall()]

            if not student_ids:
                print("[WARN] no student users found")
                return

            records_inserted = seed_recent_exercise_records(cursor, student_ids)
            metrics_inserted = seed_recent_body_metrics(cursor, student_ids)

        conn.commit()
        print(f"[OK] admin behavior demo seeded: exercise_records+={records_inserted}, body_metrics+={metrics_inserted}, students={len(student_ids)}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
