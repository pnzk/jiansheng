import argparse
import json
from collections import defaultdict

import pymysql


DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "gym_fitness_analytics",
    "charset": "utf8mb4",
    "autocommit": False,
}


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def normalize_equipment(value):
    text = (value or "").strip()
    return text if text else "未知器材"


def seed_equipment_usage(cursor, reset=False):
    if reset:
        cursor.execute("DELETE FROM equipment_usage")

    cursor.execute(
        """
        SELECT
            equipment_used,
            exercise_date,
            IFNULL(HOUR(created_at), 18) AS usage_hour,
            IFNULL(duration_minutes, 0) AS duration_minutes
        FROM exercise_records
        """
    )
    rows = cursor.fetchall()

    aggregate = defaultdict(lambda: {"usage_count": 0, "total_duration": 0})
    for equipment_used, exercise_date, usage_hour, duration_minutes in rows:
        if not exercise_date:
            continue
        key = (normalize_equipment(equipment_used), exercise_date, int(usage_hour) % 24)
        aggregate[key]["usage_count"] += 1
        aggregate[key]["total_duration"] += int(duration_minutes or 0)

    insert_rows = [
        (equipment_name, usage_date, usage_hour, value["usage_count"], value["total_duration"])
        for (equipment_name, usage_date, usage_hour), value in aggregate.items()
    ]

    if insert_rows:
        cursor.executemany(
            """
            INSERT INTO equipment_usage (
                equipment_name, usage_date, usage_hour, usage_count, total_duration_minutes
            ) VALUES (%s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                usage_count=VALUES(usage_count),
                total_duration_minutes=VALUES(total_duration_minutes)
            """,
            insert_rows,
        )

    return len(insert_rows)


def seed_user_behavior_analysis(cursor, reset=False):
    if reset:
        cursor.execute("DELETE FROM user_behavior_analysis")

    cursor.execute(
        """
        SELECT
            exercise_date,
            user_id,
            exercise_type,
            IFNULL(HOUR(created_at), 18) AS hour_val,
            IFNULL(duration_minutes, 0) AS duration_minutes
        FROM exercise_records
        ORDER BY exercise_date ASC, id ASC
        """
    )
    rows = cursor.fetchall()

    stats_by_date = defaultdict(
        lambda: {
            "exercise_count": 0,
            "duration_sum": 0,
            "users": set(),
            "type_counts": defaultdict(int),
            "hour_counts": defaultdict(int),
        }
    )

    for exercise_date, user_id, exercise_type, hour_val, duration_minutes in rows:
        if not exercise_date:
            continue

        day_stats = stats_by_date[exercise_date]
        day_stats["exercise_count"] += 1
        day_stats["duration_sum"] += int(duration_minutes or 0)
        day_stats["users"].add(user_id)

        exercise_key = (exercise_type or "未知运动").strip() or "未知运动"
        day_stats["type_counts"][exercise_key] += 1
        day_stats["hour_counts"][int(hour_val) % 24] += 1

    insert_rows = []
    for analysis_date, day_stats in stats_by_date.items():
        if day_stats["exercise_count"] <= 0:
            continue

        popular_exercise = max(day_stats["type_counts"].items(), key=lambda item: item[1])[0]
        peak_hour_start = max(day_stats["hour_counts"].items(), key=lambda item: item[1])[0]
        peak_hour_end = (peak_hour_start + 1) % 24
        avg_duration = round(day_stats["duration_sum"] / day_stats["exercise_count"], 2)
        active_user_count = len(day_stats["users"])

        distribution = dict(sorted(day_stats["type_counts"].items(), key=lambda item: item[1], reverse=True))
        distribution_json = json.dumps(distribution, ensure_ascii=False)

        insert_rows.append(
            (
                analysis_date,
                popular_exercise,
                peak_hour_start,
                peak_hour_end,
                avg_duration,
                active_user_count,
                distribution_json,
            )
        )

    if insert_rows:
        cursor.executemany(
            """
            INSERT INTO user_behavior_analysis (
                analysis_date,
                most_popular_exercise,
                peak_hour_start,
                peak_hour_end,
                average_duration_minutes,
                active_user_count,
                exercise_type_distribution
            ) VALUES (%s,%s,%s,%s,%s,%s,%s)
            ON DUPLICATE KEY UPDATE
                most_popular_exercise=VALUES(most_popular_exercise),
                peak_hour_start=VALUES(peak_hour_start),
                peak_hour_end=VALUES(peak_hour_end),
                average_duration_minutes=VALUES(average_duration_minutes),
                active_user_count=VALUES(active_user_count),
                exercise_type_distribution=VALUES(exercise_type_distribution)
            """,
            insert_rows,
        )

    return len(insert_rows)


def parse_args():
    parser = argparse.ArgumentParser(description="回填 analytics 结果表（equipment_usage / user_behavior_analysis）")
    parser.add_argument("--reset", action="store_true", help="先清空两张结果表再重建")
    return parser.parse_args()


def main():
    args = parse_args()

    conn = get_connection()
    try:
        cursor = conn.cursor()
        equipment_rows = seed_equipment_usage(cursor, reset=args.reset)
        behavior_rows = seed_user_behavior_analysis(cursor, reset=args.reset)
        conn.commit()

        cursor.execute("SELECT COUNT(*) FROM equipment_usage")
        equipment_total = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM user_behavior_analysis")
        behavior_total = cursor.fetchone()[0]

        print("=" * 72)
        print("analytics 结果表回填完成")
        print("=" * 72)
        print(f"本次写入 equipment_usage 维度组合数       : {equipment_rows}")
        print(f"本次写入 user_behavior_analysis 天数      : {behavior_rows}")
        print(f"equipment_usage 表总行数                 : {equipment_total}")
        print(f"user_behavior_analysis 表总行数          : {behavior_total}")
        print("=" * 72)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()

