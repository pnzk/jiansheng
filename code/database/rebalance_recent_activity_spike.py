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

WINDOW_DAYS = 7
RANDOM_SEED = 20260519


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def get_latest_exercise_date(cursor):
    cursor.execute("SELECT MAX(exercise_date) FROM exercise_records")
    row = cursor.fetchone()
    return row[0] if row else None


def fetch_window_activity(cursor, start_day, end_day):
    cursor.execute(
        """
        SELECT er.user_id, er.exercise_date
        FROM exercise_records er
        JOIN users u ON u.id = er.user_id
        WHERE u.user_role = 'STUDENT'
          AND er.exercise_date BETWEEN %s AND %s
        """,
        (start_day, end_day),
    )
    activity_by_user = defaultdict(set)
    for user_id, exercise_day in cursor.fetchall():
        activity_by_user[int(user_id)].add(exercise_day)
    return activity_by_user


def fetch_peak_day_records(cursor, peak_day):
    cursor.execute(
        """
        SELECT er.id, er.user_id, er.created_at
        FROM exercise_records er
        JOIN users u ON u.id = er.user_id
        WHERE u.user_role = 'STUDENT'
          AND er.exercise_date = %s
        ORDER BY er.id
        """,
        (peak_day,),
    )
    return cursor.fetchall()


def build_day_counts(activity_by_user, start_day, end_day):
    counts = {}
    cursor_day = start_day
    while cursor_day <= end_day:
        counts[cursor_day] = 0
        cursor_day += timedelta(days=1)

    for active_days in activity_by_user.values():
        for active_day in active_days:
            if active_day in counts:
                counts[active_day] += 1
    return counts


def build_target_counts(day_counts, peak_day):
    ordered_days = sorted(day_counts.keys())
    total_user_days = sum(day_counts.values())
    base = total_user_days // len(ordered_days)
    remainder = total_user_days % len(ordered_days)

    target_counts = {}
    for index, day in enumerate(ordered_days):
        target_counts[day] = base + (1 if index < remainder else 0)

    if peak_day in target_counts and target_counts[peak_day] > day_counts[peak_day]:
        target_counts[peak_day] = day_counts[peak_day]

    redistributed_total = sum(target_counts.values())
    if redistributed_total != total_user_days:
        difference = total_user_days - redistributed_total
        for day in ordered_days:
            if day == peak_day:
                continue
            target_counts[day] += 1
            difference -= 1
            if difference == 0:
                break

    return target_counts


def rebalance_records(activity_by_user, peak_day_records, previous_days, target_counts, day_counts):
    random.seed(RANDOM_SEED)

    remaining_capacity = {
        day: max(target_counts[day] - day_counts[day], 0)
        for day in previous_days
    }

    shuffled_records = list(peak_day_records)
    random.shuffle(shuffled_records)

    def option_count(record):
        user_id = int(record[1])
        active_days = activity_by_user.get(user_id, set())
        return sum(1 for day in previous_days if remaining_capacity[day] > 0 and day not in active_days)

    shuffled_records.sort(key=option_count)

    updates = []
    for record_id, user_id, created_at in shuffled_records:
        user_id = int(user_id)
        active_days = activity_by_user.get(user_id, set())
        available_days = [
            day for day in previous_days
            if remaining_capacity[day] > 0 and day not in active_days
        ]
        if not available_days:
            continue

        destination_day = max(available_days, key=lambda day: (remaining_capacity[day], -day.toordinal()))
        if isinstance(created_at, datetime):
            new_created_at = datetime.combine(destination_day, created_at.timetz().replace(tzinfo=None))
        else:
            new_created_at = datetime.combine(destination_day, datetime.min.time()).replace(hour=18)

        updates.append((destination_day, new_created_at, int(record_id)))
        remaining_capacity[destination_day] -= 1
        activity_by_user[user_id].discard(max(active_days))
        activity_by_user[user_id].add(destination_day)

    return updates, remaining_capacity


def apply_updates(cursor, updates):
    if not updates:
        return 0

    cursor.executemany(
        """
        UPDATE exercise_records
        SET exercise_date = %s,
            created_at = %s
        WHERE id = %s
        """,
        updates,
    )
    return len(updates)


def print_counts(title, counts):
    print(title)
    for day in sorted(counts.keys()):
        print(f"  {day}: {counts[day]}")


def main():
    conn = get_connection()
    try:
        cursor = conn.cursor()
        latest_day = get_latest_exercise_date(cursor)
        if latest_day is None:
            print("[WARN] no exercise records found")
            return

        window_start = latest_day - timedelta(days=WINDOW_DAYS - 1)
        activity_by_user = fetch_window_activity(cursor, window_start, latest_day)
        day_counts_before = build_day_counts(activity_by_user, window_start, latest_day)
        peak_day_records = fetch_peak_day_records(cursor, latest_day)

        target_counts = build_target_counts(day_counts_before, latest_day)
        previous_days = [day for day in sorted(day_counts_before.keys()) if day != latest_day]
        updates, remaining_capacity = rebalance_records(
            activity_by_user,
            peak_day_records,
            previous_days,
            target_counts,
            day_counts_before,
        )

        moved_rows = apply_updates(cursor, updates)
        conn.commit()

        activity_after = fetch_window_activity(cursor, window_start, latest_day)
        day_counts_after = build_day_counts(activity_after, window_start, latest_day)

        print("=" * 72)
        print("Recent activity spike rebalance completed")
        print("=" * 72)
        print(f"Window        : {window_start} -> {latest_day}")
        print(f"Peak day      : {latest_day}")
        print(f"Moved records : {moved_rows}")
        print_counts("Before:", day_counts_before)
        print_counts("Target:", target_counts)
        print_counts("After:", day_counts_after)
        unresolved = {day: count for day, count in remaining_capacity.items() if count > 0}
        print(f"Unresolved target gaps: {unresolved if unresolved else 'none'}")
        print("=" * 72)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
