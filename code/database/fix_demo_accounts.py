from __future__ import annotations

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


DEMO_STUDENT_USERNAME = "user00001"
DEMO_COACH_USERNAME = "coach_auto_001"

PLAN_NAME = "减脂强化训练计划"
PLAN_DESCRIPTION = "围绕减脂目标设计的演示计划，覆盖有氧、力量和恢复安排，适合课堂展示与页面联调。"
WEEKLY_SCHEDULE = (
    '{"monday":["跑步 30 分钟","核心训练 15 分钟"],'
    '"tuesday":["快走 40 分钟","拉伸放松 10 分钟"],'
    '"wednesday":["力量训练 45 分钟"],'
    '"thursday":["动感单车 35 分钟","上肢训练 20 分钟"],'
    '"friday":["HIIT 25 分钟","核心训练 15 分钟"],'
    '"saturday":["游泳 40 分钟"],'
    '"sunday":["主动恢复 / 休息"]}'
)

DEMO_RECORDS = [
    ("2026-05-12", "跑步", 42, 338.0, 128, 154, "跑步机", time(7, 40, 0)),
    ("2026-05-13", "力量训练", 55, 421.0, 121, 148, "哑铃", time(18, 15, 0)),
    ("2026-05-14", "HIIT", 36, 312.0, 136, 168, "壶铃", time(19, 5, 0)),
]

DEMO_METRICS = [
    ("2026-05-05", 84.4, 14.8, 156.0, 34.68, 40.8),
    ("2026-05-10", 83.9, 14.2, 156.0, 34.47, 41.0),
    ("2026-05-14", 83.2, 13.7, 156.0, 34.19, 41.3),
]


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def fetch_one_id(cursor, username: str) -> int:
    cursor.execute("SELECT id FROM users WHERE username=%s", (username,))
    row = cursor.fetchone()
    if not row:
        raise ValueError(f"user not found: {username}")
    return int(row[0])


def cleanup_recent_demo_plans(cursor, student_id: int) -> None:
    cursor.execute(
        """
        DELETE FROM training_plans
        WHERE student_id=%s
          AND (plan_name LIKE '????%%' OR description LIKE '????%%' OR weekly_schedule LIKE '\"????%%')
        """,
        (student_id,),
    )


def upsert_demo_plan(cursor, student_id: int, coach_id: int) -> None:
    cursor.execute(
        """
        SELECT id
        FROM training_plans
        WHERE student_id=%s
        ORDER BY updated_at DESC, id DESC
        LIMIT 1
        """,
        (student_id,),
    )
    row = cursor.fetchone()

    start_date = date(2026, 5, 12)
    end_date = date(2026, 6, 12)
    completion_rate = 38.0

    if row:
        cursor.execute(
            """
            UPDATE training_plans
            SET coach_id=%s,
                plan_name=%s,
                goal_type='FAT_LOSS',
                target_value=8.0,
                start_date=%s,
                end_date=%s,
                status='ACTIVE',
                completion_rate=%s,
                weekly_schedule=%s,
                description=%s,
                updated_at=NOW()
            WHERE id=%s
            """,
            (
                coach_id,
                PLAN_NAME,
                start_date,
                end_date,
                completion_rate,
                WEEKLY_SCHEDULE,
                PLAN_DESCRIPTION,
                int(row[0]),
            ),
        )
    else:
        cursor.execute(
            """
            INSERT INTO training_plans (
                student_id, coach_id, plan_name, goal_type, target_value,
                start_date, end_date, status, completion_rate,
                weekly_schedule, description, created_at, updated_at
            ) VALUES (%s,%s,%s,'FAT_LOSS',8.0,%s,%s,'ACTIVE',%s,%s,%s,NOW(),NOW())
            """,
            (
                student_id,
                coach_id,
                PLAN_NAME,
                start_date,
                end_date,
                completion_rate,
                WEEKLY_SCHEDULE,
                PLAN_DESCRIPTION,
            ),
        )


def replace_demo_records(cursor, student_id: int) -> None:
    cursor.execute(
        "DELETE FROM exercise_records WHERE user_id=%s AND exercise_date BETWEEN '2026-05-12' AND '2026-05-14'",
        (student_id,),
    )

    for exercise_date, exercise_type, duration, calories, avg_hr, max_hr, equipment, created_time in DEMO_RECORDS:
        created_at = datetime.combine(datetime.strptime(exercise_date, "%Y-%m-%d").date(), created_time)
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
                exercise_type,
                exercise_date,
                duration,
                calories,
                avg_hr,
                max_hr,
                equipment,
                created_at,
            ),
        )


def replace_demo_metrics(cursor, student_id: int) -> None:
    cursor.execute(
        "DELETE FROM body_metrics WHERE user_id=%s AND measurement_date BETWEEN '2026-05-05' AND '2026-05-14'",
        (student_id,),
    )

    for measurement_date, weight_kg, body_fat, height_cm, bmi, muscle_mass in DEMO_METRICS:
        cursor.execute(
            """
            INSERT INTO body_metrics (
                user_id, measurement_date, weight_kg, body_fat_percentage,
                height_cm, bmi, muscle_mass_kg, created_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,NOW())
            """,
            (
                student_id,
                measurement_date,
                weight_kg,
                body_fat,
                height_cm,
                bmi,
                muscle_mass,
            ),
        )


def refresh_leaderboards(cursor) -> None:
    period_start = date.today() - timedelta(days=30)
    period_end = date.today()
    cursor.execute("CALL update_leaderboards(%s, %s)", (period_start, period_end))


def main() -> None:
    conn = get_connection()
    try:
        cursor = conn.cursor()

        student_id = fetch_one_id(cursor, DEMO_STUDENT_USERNAME)
        coach_id = fetch_one_id(cursor, DEMO_COACH_USERNAME)

        cleanup_recent_demo_plans(cursor, student_id)
        upsert_demo_plan(cursor, student_id, coach_id)
        replace_demo_records(cursor, student_id)
        replace_demo_metrics(cursor, student_id)
        refresh_leaderboards(cursor)

        conn.commit()
        print("demo account data fixed")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
