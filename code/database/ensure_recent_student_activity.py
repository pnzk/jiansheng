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


RANDOM_SEED = 20260212
random.seed(RANDOM_SEED)


GOAL_EXERCISE_TYPES = {
    "WEIGHT_LOSS": ["跑步", "动感单车", "快走", "HIIT"],
    "FAT_LOSS": ["HIIT", "有氧训练", "跑步", "力量训练"],
    "MUSCLE_GAIN": ["力量训练", "深蹲", "卧推", "硬拉"],
}

GOAL_EQUIPMENT = {
    "WEIGHT_LOSS": ["跑步机", "椭圆机", "单车"],
    "FAT_LOSS": ["壶铃", "单车", "划船机"],
    "MUSCLE_GAIN": ["杠铃", "哑铃", "史密斯机"],
}


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def build_created_at(exercise_day):
    return datetime.combine(
        exercise_day,
        time(
            hour=random.randint(6, 22),
            minute=random.randint(0, 59),
            second=random.randint(0, 59),
        ),
    )


def normalize_goal(goal):
    text = (goal or "").strip().upper()
    if text in {"WEIGHT_LOSS", "FAT_LOSS", "MUSCLE_GAIN"}:
        return text
    return "WEIGHT_LOSS"


def insert_exercise_record(cursor, student_id, goal_key, exercise_day):
    types = GOAL_EXERCISE_TYPES[goal_key]
    equipment = GOAL_EQUIPMENT[goal_key]

    duration = random.randint(30, 75)
    calories = int(duration * random.uniform(5.5, 10.0))
    avg_hr = random.randint(95, 150)
    max_hr = min(avg_hr + random.randint(15, 35), 195)

    cursor.execute(
        """
        INSERT INTO exercise_records (
            user_id, exercise_type, exercise_date, duration_minutes,
            calories_burned, average_heart_rate, max_heart_rate,
            equipment_used, created_at
        ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """,
        (
            student_id,
            random.choice(types),
            exercise_day,
            duration,
            calories,
            avg_hr,
            max_hr,
            random.choice(equipment),
            build_created_at(exercise_day),
        ),
    )


def build_recent_records(cursor, student_id, goal):
    goal_key = normalize_goal(goal)
    inserted = 0

    cursor.execute("SELECT MAX(exercise_date) FROM exercise_records WHERE user_id=%s", (student_id,))
    latest_date = cursor.fetchone()[0]
    needs_recent = latest_date is None or latest_date < (date.today() - timedelta(days=30))

    if not needs_recent:
        cursor.execute(
            "SELECT COUNT(*) FROM exercise_records WHERE user_id=%s AND exercise_date>=DATE_SUB(CURDATE(), INTERVAL 30 DAY)",
            (student_id,),
        )
        recent_count = cursor.fetchone()[0]
        if recent_count >= 6:
            target_count = 0
        else:
            target_count = 6 - recent_count
    else:
        target_count = 8

    for _ in range(target_count):
        day = date.today() - timedelta(days=random.randint(1, 28))
        insert_exercise_record(cursor, student_id, goal_key, day)
        inserted += 1

    cursor.execute(
        """
        SELECT COUNT(*)
        FROM exercise_records
        WHERE user_id=%s
          AND exercise_date BETWEEN DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
                               AND DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)
        """,
        (student_id,),
    )
    week_count = cursor.fetchone()[0]
    if week_count == 0:
        weekday = date.today().weekday()
        week_start = date.today() - timedelta(days=weekday)
        day = week_start + timedelta(days=random.randint(0, max(weekday, 0)))
        insert_exercise_record(cursor, student_id, goal_key, day)
        inserted += 1

    return inserted


def ensure_recent_metrics(cursor, student_id, goal):
    goal_key = normalize_goal(goal)
    inserted = 0

    cursor.execute(
        """
        SELECT measurement_date, weight_kg, body_fat_percentage, height_cm, muscle_mass_kg
        FROM body_metrics
        WHERE user_id=%s
        ORDER BY measurement_date DESC, id DESC
        LIMIT 1
        """,
        (student_id,),
    )
    latest = cursor.fetchone()

    if latest:
        latest_weight = float(latest[1] or 70.0)
        latest_fat = float(latest[2] or 22.0)
        latest_height = float(latest[3] or 170.0)
        latest_muscle = float(latest[4] or max(latest_weight * 0.42, 20.0))
    else:
        latest_weight = random.uniform(55, 82)
        latest_fat = random.uniform(16, 30)
        latest_height = random.uniform(158, 180)
        latest_muscle = max(latest_weight * 0.42, 20.0)

    d_start = date.today() - timedelta(days=27)
    d_end = date.today() - timedelta(days=1)

    cursor.execute("SELECT COUNT(*) FROM body_metrics WHERE user_id=%s AND measurement_date=%s", (student_id, d_start))
    has_start = cursor.fetchone()[0] > 0
    cursor.execute("SELECT COUNT(*) FROM body_metrics WHERE user_id=%s AND measurement_date=%s", (student_id, d_end))
    has_end = cursor.fetchone()[0] > 0

    if goal_key in {"WEIGHT_LOSS", "FAT_LOSS"}:
        start_weight = latest_weight + random.uniform(1.0, 3.5)
        end_weight = max(start_weight - random.uniform(0.8, 2.8), 40.0)
        start_fat = latest_fat + random.uniform(0.5, 2.5)
        end_fat = max(start_fat - random.uniform(0.4, 1.8), 5.0)
        start_muscle = max(latest_muscle - random.uniform(0.2, 0.8), 18.0)
        end_muscle = start_muscle + random.uniform(0.0, 0.5)
    else:
        start_weight = max(latest_weight - random.uniform(0.6, 1.6), 40.0)
        end_weight = start_weight + random.uniform(0.6, 2.0)
        start_fat = latest_fat + random.uniform(0.2, 1.0)
        end_fat = max(start_fat - random.uniform(0.1, 1.0), 5.0)
        start_muscle = max(latest_muscle - random.uniform(0.3, 0.8), 18.0)
        end_muscle = start_muscle + random.uniform(0.6, 1.8)

    if not has_start:
        start_bmi = round(start_weight / ((latest_height / 100.0) ** 2), 2)
        cursor.execute(
            """
            INSERT INTO body_metrics (
                user_id, measurement_date, weight_kg, body_fat_percentage,
                height_cm, bmi, muscle_mass_kg, created_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,NOW())
            """,
            (
                student_id,
                d_start,
                round(start_weight, 2),
                round(start_fat, 2),
                round(latest_height, 2),
                start_bmi,
                round(start_muscle, 2),
            ),
        )
        inserted += 1

    if not has_end:
        end_bmi = round(end_weight / ((latest_height / 100.0) ** 2), 2)
        cursor.execute(
            """
            INSERT INTO body_metrics (
                user_id, measurement_date, weight_kg, body_fat_percentage,
                height_cm, bmi, muscle_mass_kg, created_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,NOW())
            """,
            (
                student_id,
                d_end,
                round(end_weight, 2),
                round(end_fat, 2),
                round(latest_height, 2),
                end_bmi,
                round(end_muscle, 2),
            ),
        )
        inserted += 1

    return inserted


def refresh_leaderboards(cursor):
    period_start = date.today() - timedelta(days=30)
    period_end = date.today()
    cursor.execute("CALL update_leaderboards(%s, %s)", (period_start, period_end))


def get_coverage(cursor):
    cursor.execute("SELECT COUNT(*) FROM users WHERE user_role='STUDENT'")
    student_total = cursor.fetchone()[0]

    cursor.execute(
        """
        SELECT COUNT(DISTINCT er.user_id)
        FROM exercise_records er
        JOIN users u ON u.id=er.user_id AND u.user_role='STUDENT'
        WHERE er.exercise_date>=DATE_SUB(CURDATE(), INTERVAL 30 DAY)
        """
    )
    students_with_records_30d = cursor.fetchone()[0]

    cursor.execute(
        """
        SELECT COUNT(DISTINCT er.user_id)
        FROM exercise_records er
        JOIN users u ON u.id=er.user_id AND u.user_role='STUDENT'
        WHERE er.exercise_date BETWEEN DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
                                  AND DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)
        """
    )
    students_with_records_week = cursor.fetchone()[0]

    cursor.execute(
        """
        SELECT COUNT(DISTINCT bm.user_id)
        FROM body_metrics bm
        JOIN users u ON u.id=bm.user_id AND u.user_role='STUDENT'
        WHERE bm.measurement_date>=DATE_SUB(CURDATE(), INTERVAL 30 DAY)
        """
    )
    students_with_metrics_30d = cursor.fetchone()[0]

    cursor.execute("SELECT COUNT(*) FROM leaderboards")
    leaderboard_total = cursor.fetchone()[0]

    return {
        "student_total": student_total,
        "students_with_records_30d": students_with_records_30d,
        "students_with_records_week": students_with_records_week,
        "students_with_metrics_30d": students_with_metrics_30d,
        "leaderboard_total": leaderboard_total,
    }


def main():
    conn = get_connection()
    try:
        cursor = conn.cursor()

        before = get_coverage(cursor)

        cursor.execute("SELECT id, fitness_goal FROM users WHERE user_role='STUDENT'")
        students = cursor.fetchall()

        record_inserted = 0
        metric_inserted = 0

        for student_id, fitness_goal in students:
            record_inserted += build_recent_records(cursor, student_id, fitness_goal)
            metric_inserted += ensure_recent_metrics(cursor, student_id, fitness_goal)

        refresh_leaderboards(cursor)
        conn.commit()

        after = get_coverage(cursor)

        print("=" * 72)
        print("近期活跃数据补齐完成")
        print("=" * 72)
        print(f"学员总数               : {len(students)}")
        print(f"新增运动记录           : {record_inserted}")
        print(f"新增体测记录           : {metric_inserted}")
        print(f"本周运动覆盖           : {after['students_with_records_week']}/{after['student_total']} (before {before['students_with_records_week']})")
        print(f"近30天运动覆盖         : {after['students_with_records_30d']}/{after['student_total']} (before {before['students_with_records_30d']})")
        print(f"近30天体测覆盖         : {after['students_with_metrics_30d']}/{after['student_total']} (before {before['students_with_metrics_30d']})")
        print(f"排行榜总行数           : {after['leaderboard_total']} (before {before['leaderboard_total']})")
        print("=" * 72)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
