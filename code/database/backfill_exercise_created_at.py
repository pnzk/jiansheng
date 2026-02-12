import random
from datetime import datetime, timedelta

import pymysql


DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '123456',
    'database': 'gym_fitness_analytics',
    'charset': 'utf8mb4',
    'autocommit': False,
}


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def random_activity_time(base_date):
    return datetime.combine(base_date, datetime.min.time()) + timedelta(
        hours=random.randint(6, 22),
        minutes=random.randint(0, 59),
        seconds=random.randint(0, 59),
    )


def main():
    random.seed(20260212)
    conn = get_connection()
    try:
        cursor = conn.cursor()

        cursor.execute(
            """
            SELECT id, exercise_date, created_at
            FROM exercise_records
            WHERE created_at IS NULL
               OR DATE(created_at) <> exercise_date
            """
        )
        rows = cursor.fetchall()

        total = len(rows)
        updated = 0

        for record_id, exercise_date, _ in rows:
            if not exercise_date:
                continue
            created_at = random_activity_time(exercise_date)
            cursor.execute(
                "UPDATE exercise_records SET created_at=%s WHERE id=%s",
                (created_at, record_id),
            )
            updated += 1
            if updated % 2000 == 0:
                conn.commit()

        conn.commit()

        print('=' * 72)
        print('exercise_records.created_at 回填完成')
        print('=' * 72)
        print(f'待修复记录数          : {total}')
        print(f'已修复记录数          : {updated}')
        print('=' * 72)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == '__main__':
    main()

