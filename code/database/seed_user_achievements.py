import argparse
import random
from collections import defaultdict
from datetime import date, datetime, timedelta

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


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def compute_longest_streak(sorted_dates):
    if not sorted_dates:
        return 0
    longest = 1
    current = 1
    for index in range(1, len(sorted_dates)):
        prev = sorted_dates[index - 1]
        curr = sorted_dates[index]
        if (curr - prev).days == 1:
            current += 1
            if current > longest:
                longest = current
        else:
            current = 1
    return longest


def load_student_ids(cursor):
    cursor.execute("SELECT id FROM users WHERE user_role='STUDENT'")
    return [row[0] for row in cursor.fetchall()]


def load_achievements(cursor):
    cursor.execute("SELECT id, achievement_type, threshold_value FROM achievements")
    return [
        {
            "id": row[0],
            "type": (row[1] or "").strip().upper(),
            "threshold": float(row[2] or 0),
        }
        for row in cursor.fetchall()
    ]


def build_student_metrics(cursor, student_ids):
    metrics = {
        student_id: {
            "EXERCISE_COUNT": 0.0,
            "TOTAL_CALORIES": 0.0,
            "SINGLE_DURATION": 0.0,
            "CONSECUTIVE_DAYS": 0.0,
            "RUNNING_DISTANCE": 0.0,
            "WEIGHT_LOSS": 0.0,
            "FAT_LOSS": 0.0,
            "MUSCLE_GAIN": 0.0,
        }
        for student_id in student_ids
    }

    cursor.execute(
        """
        SELECT user_id, exercise_date, exercise_type, duration_minutes, calories_burned
        FROM exercise_records
        WHERE user_id IN (SELECT id FROM users WHERE user_role='STUDENT')
        ORDER BY user_id, exercise_date, id
        """
    )
    records_by_user_days = defaultdict(set)

    for user_id, exercise_date, exercise_type, duration_minutes, calories_burned in cursor.fetchall():
        if user_id not in metrics:
            continue
        duration = float(duration_minutes or 0)
        calories = float(calories_burned or 0)
        metrics[user_id]["EXERCISE_COUNT"] += 1
        metrics[user_id]["TOTAL_CALORIES"] += calories
        if duration > metrics[user_id]["SINGLE_DURATION"]:
            metrics[user_id]["SINGLE_DURATION"] = duration
        if exercise_date:
            records_by_user_days[user_id].add(exercise_date)

        type_text = (exercise_type or "").lower()
        if "跑" in type_text or "run" in type_text or "jog" in type_text:
            metrics[user_id]["RUNNING_DISTANCE"] += duration / 60.0 * 8.0

    for user_id, day_set in records_by_user_days.items():
        metrics[user_id]["CONSECUTIVE_DAYS"] = float(compute_longest_streak(sorted(day_set)))

    cursor.execute(
        """
        SELECT user_id, measurement_date, weight_kg, body_fat_percentage, muscle_mass_kg
        FROM body_metrics
        WHERE user_id IN (SELECT id FROM users WHERE user_role='STUDENT')
        ORDER BY user_id, measurement_date, id
        """
    )

    body_by_user = defaultdict(list)
    for user_id, measurement_date, weight_kg, body_fat, muscle_mass in cursor.fetchall():
        if user_id in metrics and measurement_date:
            body_by_user[user_id].append((measurement_date, float(weight_kg or 0), float(body_fat or 0), float(muscle_mass or 0)))

    for user_id, rows in body_by_user.items():
        if len(rows) < 2:
            continue
        first = rows[0]
        last = rows[-1]
        weight_loss = max(first[1] - last[1], 0)
        fat_loss = max(first[2] - last[2], 0)
        muscle_gain = max(last[3] - first[3], 0)
        metrics[user_id]["WEIGHT_LOSS"] = weight_loss
        metrics[user_id]["FAT_LOSS"] = fat_loss
        metrics[user_id]["MUSCLE_GAIN"] = muscle_gain

    return metrics


def resolve_metric_value(metric_map, achievement_type):
    if achievement_type in {"TOTAL_CALORIES", "CALORIES"}:
        return metric_map.get("TOTAL_CALORIES", 0.0)
    if achievement_type in {"SINGLE_DURATION", "DURATION"}:
        return metric_map.get("SINGLE_DURATION", 0.0)
    if achievement_type == "EXERCISE_COUNT":
        return metric_map.get("EXERCISE_COUNT", 0.0)
    if achievement_type == "CONSECUTIVE_DAYS":
        return metric_map.get("CONSECUTIVE_DAYS", 0.0)
    if achievement_type == "RUNNING_DISTANCE":
        return metric_map.get("RUNNING_DISTANCE", 0.0)
    if achievement_type == "WEIGHT_LOSS":
        return metric_map.get("WEIGHT_LOSS", 0.0)
    if achievement_type == "FAT_LOSS":
        return metric_map.get("FAT_LOSS", 0.0)
    if achievement_type == "MUSCLE_GAIN":
        return metric_map.get("MUSCLE_GAIN", 0.0)
    return 0.0


def seed_user_achievements(cursor, achievements, student_metrics):
    inserted = 0
    skip_exists = 0
    unlocked_students = set()

    for student_id, metric_map in student_metrics.items():
        for achievement in achievements:
            value = resolve_metric_value(metric_map, achievement["type"])
            if value < achievement["threshold"]:
                continue

            unlocked_at = datetime.now() - timedelta(days=random.randint(1, 60), hours=random.randint(0, 23))
            cursor.execute(
                """
                INSERT IGNORE INTO user_achievements (user_id, achievement_id, unlocked_at)
                VALUES (%s, %s, %s)
                """,
                (student_id, achievement["id"], unlocked_at),
            )
            if cursor.rowcount > 0:
                inserted += 1
                unlocked_students.add(student_id)
            else:
                skip_exists += 1

    return inserted, skip_exists, len(unlocked_students)


def parse_args():
    parser = argparse.ArgumentParser(description="根据真实数据批量解锁学员成就")
    parser.add_argument("--reset", action="store_true", help="先清空 user_achievements 再重算")
    return parser.parse_args()


def main():
    args = parse_args()

    conn = get_connection()
    try:
        cursor = conn.cursor()

        if args.reset:
            cursor.execute("DELETE FROM user_achievements")

        student_ids = load_student_ids(cursor)
        achievements = load_achievements(cursor)
        student_metrics = build_student_metrics(cursor, student_ids)

        inserted, skip_exists, unlocked_students = seed_user_achievements(cursor, achievements, student_metrics)
        conn.commit()

        cursor.execute("SELECT COUNT(*) FROM user_achievements")
        total_ua = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(DISTINCT user_id) FROM user_achievements")
        covered_students = cursor.fetchone()[0]

        print("=" * 72)
        print("学员成就回填完成")
        print("=" * 72)
        print(f"学生总数                 : {len(student_ids)}")
        print(f"成就总数                 : {len(achievements)}")
        print(f"本次新增 user_achievements: {inserted}")
        print(f"已存在跳过               : {skip_exists}")
        print(f"本次触达学员数           : {unlocked_students}")
        print(f"累计解锁记录数           : {total_ua}")
        print(f"累计有成就学员数         : {covered_students}")
        print("=" * 72)

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()

