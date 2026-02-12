import argparse
import json
import math
import random
from datetime import date, datetime, timedelta

import bcrypt
import pymysql


DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "gym_fitness_analytics",
    "charset": "utf8mb4",
    "autocommit": False,
}


RANDOM_SEED = 20260211
random.seed(RANDOM_SEED)


SURNAMES = [
    "王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
    "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
]

GIVEN_NAMES = [
    "伟", "强", "磊", "洋", "勇", "军", "杰", "涛", "明", "超",
    "芳", "娜", "敏", "静", "丽", "艳", "娟", "霞", "平", "婷",
]


GOAL_TARGET_RANGE = {
    "WEIGHT_LOSS": (3.0, 10.0),
    "FAT_LOSS": (2.0, 8.0),
    "MUSCLE_GAIN": (1.0, 5.0),
}


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def random_name():
    return random.choice(SURNAMES) + random.choice(GIVEN_NAMES)


def normalize_goal(goal):
    normalized = (goal or "").strip().upper()
    if normalized in {"WEIGHT_LOSS", "FAT_LOSS", "MUSCLE_GAIN"}:
        return normalized
    return "WEIGHT_LOSS"


def build_weekly_schedule(goal_type):
    goal_key = normalize_goal(goal_type)
    templates = {
        "WEIGHT_LOSS": {
            "monday": ["跑步 45 分钟", "核心训练 20 分钟"],
            "tuesday": ["快走 40 分钟", "拉伸放松 15 分钟"],
            "wednesday": ["动感单车 35 分钟", "力量循环 20 分钟"],
            "thursday": ["瑜伽 40 分钟"],
            "friday": ["跑步间歇 30 分钟", "臀腿训练 25 分钟"],
            "saturday": ["户外有氧 60 分钟"],
            "sunday": ["主动恢复 / 休息"],
        },
        "FAT_LOSS": {
            "monday": ["HIIT 25 分钟", "力量训练 25 分钟"],
            "tuesday": ["快走 45 分钟", "核心训练 20 分钟"],
            "wednesday": ["游泳 40 分钟"],
            "thursday": ["动感单车 35 分钟", "上肢训练 20 分钟"],
            "friday": ["跑步 40 分钟", "拉伸 15 分钟"],
            "saturday": ["综合循环训练 60 分钟"],
            "sunday": ["主动恢复 / 休息"],
        },
        "MUSCLE_GAIN": {
            "monday": ["胸肩力量 60 分钟", "核心训练 15 分钟"],
            "tuesday": ["背部力量 60 分钟", "有氧 20 分钟"],
            "wednesday": ["下肢力量 65 分钟"],
            "thursday": ["休息 / 拉伸恢复"],
            "friday": ["全身复合力量 60 分钟"],
            "saturday": ["功能性训练 45 分钟", "有氧 20 分钟"],
            "sunday": ["休息 / 轻量拉伸"],
        },
    }
    return json.dumps(templates[goal_key], ensure_ascii=False)


def ensure_admin_accounts(cursor, admin_count, password_hash):
    created = 0
    existing = 0
    admin_ids = []

    for index in range(1, admin_count + 1):
        username = f"admin_auto_{index:03d}"
        email = f"{username}@gym.local"

        cursor.execute("SELECT id FROM users WHERE username=%s", (username,))
        row = cursor.fetchone()
        if row:
            row_id = row["id"]
            admin_ids.append(row_id)
            existing += 1
            cursor.execute(
                """
                UPDATE users
                SET user_role='ADMIN', password=%s, real_name=%s,
                    show_in_leaderboard=0, allow_coach_view=1, updated_at=NOW()
                WHERE id=%s
                """,
                (password_hash, f"系统管理员{index}", row_id),
            )
            continue

        cursor.execute(
            """
            INSERT INTO users (
                username, password, email, phone, real_name, age, gender,
                user_role, show_in_leaderboard, allow_coach_view, created_at, updated_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,'ADMIN',0,1,NOW(),NOW())
            """,
            (
                username,
                password_hash,
                email,
                f"186{random.randint(10000000, 99999999)}",
                f"系统管理员{index}",
                random.randint(28, 45),
                random.choice(["MALE", "FEMALE"]),
            ),
        )
        admin_ids.append(cursor.lastrowid)
        created += 1

    return admin_ids, created, existing


def ensure_coaches(cursor, coach_count, password_hash):
    created = 0
    existing = 0
    coach_ids = []

    for index in range(1, coach_count + 1):
        username = f"coach_auto_{index:03d}"
        email = f"{username}@gym.local"
        cursor.execute("SELECT id FROM users WHERE username=%s", (username,))
        row = cursor.fetchone()

        if row:
            row_id = row["id"]
            coach_ids.append(row_id)
            existing += 1
            cursor.execute(
                """
                UPDATE users
                SET user_role='COACH', password=%s, real_name=%s, age=%s, gender=%s,
                    show_in_leaderboard=0, allow_coach_view=1, updated_at=NOW()
                WHERE id=%s
                """,
                (
                    password_hash,
                    random_name(),
                    random.randint(24, 46),
                    random.choice(["MALE", "FEMALE"]),
                    row_id,
                ),
            )
            continue

        cursor.execute(
            """
            INSERT INTO users (
                username, password, email, phone, real_name, age, gender,
                user_role, show_in_leaderboard, allow_coach_view, created_at, updated_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,'COACH',0,1,NOW(),NOW())
            """,
            (
                username,
                password_hash,
                email,
                f"139{random.randint(10000000, 99999999)}",
                random_name(),
                random.randint(24, 46),
                random.choice(["MALE", "FEMALE"]),
            ),
        )
        coach_ids.append(cursor.lastrowid)
        created += 1

    return coach_ids, created, existing


def assign_students_to_coaches(cursor, students, coach_ids):
    assignments = []
    for index, student in enumerate(students):
        coach_id = coach_ids[index % len(coach_ids)]
        assignments.append((coach_id, student["id"]))

    cursor.executemany("UPDATE users SET coach_id=%s, updated_at=NOW() WHERE id=%s", assignments)
    return assignments


def ensure_active_plans(cursor, students, student_to_coach):
    inserted = 0
    updated = 0

    for student in students:
        student_id = student["id"]
        goal_type = normalize_goal(student["fitness_goal"])
        coach_id = student_to_coach[student_id]

        target_min, target_max = GOAL_TARGET_RANGE[goal_type]
        target_value = round(random.uniform(target_min, target_max), 1)
        start_date = date.today() - timedelta(days=random.randint(30, 90))
        end_date = date.today() + timedelta(days=random.randint(45, 150))
        completion_rate = round(random.uniform(15, 92), 1)
        schedule = build_weekly_schedule(goal_type)
        description = f"根据学员当前状态制定的{goal_type}阶段性计划，按周执行并每周复盘。"

        cursor.execute(
            """
            SELECT id FROM training_plans
            WHERE student_id=%s AND status='ACTIVE'
            ORDER BY updated_at DESC LIMIT 1
            """,
            (student_id,),
        )
        row = cursor.fetchone()

        if row:
            row_id = row["id"]
            cursor.execute(
                """
                UPDATE training_plans
                SET coach_id=%s, plan_name=%s, goal_type=%s, target_value=%s,
                    start_date=%s, end_date=%s, completion_rate=%s,
                    weekly_schedule=%s, description=%s, updated_at=NOW()
                WHERE id=%s
                """,
                (
                    coach_id,
                    f"系统计划-{student_id}",
                    goal_type,
                    target_value,
                    start_date,
                    end_date,
                    completion_rate,
                    schedule,
                    description,
                    row_id,
                ),
            )
            updated += 1
            continue

        cursor.execute(
            """
            INSERT INTO training_plans (
                student_id, coach_id, plan_name, goal_type, target_value,
                start_date, end_date, status, completion_rate,
                weekly_schedule, description, created_at, updated_at
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,'ACTIVE',%s,%s,%s,NOW(),NOW())
            """,
            (
                student_id,
                coach_id,
                f"系统计划-{student_id}",
                goal_type,
                target_value,
                start_date,
                end_date,
                completion_rate,
                schedule,
                description,
            ),
        )
        inserted += 1

    return inserted, updated


def refresh_leaderboards(cursor):
    period_start = date.today() - timedelta(days=30)
    period_end = date.today()
    cursor.execute("CALL update_leaderboards(%s, %s)", (period_start, period_end))
    return period_start, period_end


def parse_args():
    parser = argparse.ArgumentParser(description="按比例生成教练并分配学员，同时补齐训练计划和排行榜")
    parser.add_argument("--students-per-coach", type=int, default=30, help="每位教练负责学员数，默认30")
    parser.add_argument("--coach-count", type=int, default=0, help="指定教练数量；>0 时覆盖比例计算")
    parser.add_argument("--admin-count", type=int, default=1, help="自动创建管理员数量")
    parser.add_argument("--password", default="123456", help="自动创建账号密码（默认123456）")
    return parser.parse_args()


def main():
    args = parse_args()

    conn = get_connection()
    try:
        cursor = conn.cursor(pymysql.cursors.DictCursor)

        cursor.execute("SELECT id, fitness_goal FROM users WHERE user_role='STUDENT' ORDER BY id")
        students = cursor.fetchall()
        if not students:
            print("[X] 当前没有学员数据，请先导入 users/exercise/body_metrics。")
            return

        student_count = len(students)
        coach_count = args.coach_count if args.coach_count > 0 else max(1, math.ceil(student_count / max(args.students_per_coach, 1)))

        password_hash = bcrypt.hashpw(args.password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

        admin_ids, admin_created, admin_existing = ensure_admin_accounts(cursor, args.admin_count, password_hash)
        coach_ids, coach_created, coach_existing = ensure_coaches(cursor, coach_count, password_hash)

        assignments = assign_students_to_coaches(cursor, students, coach_ids)
        student_to_coach = {student_id: coach_id for coach_id, student_id in assignments}

        plan_inserted, plan_updated = ensure_active_plans(cursor, students, student_to_coach)
        period_start, period_end = refresh_leaderboards(cursor)

        conn.commit()

        print("=" * 72)
        print("  教练/管理员生成 + 学员分配 + 计划补齐 完成")
        print("=" * 72)
        print(f"学员总数            : {student_count}")
        print(f"教练总数（目标）    : {coach_count}")
        print(f"教练新增/已存在     : {coach_created}/{coach_existing}")
        print(f"管理员新增/已存在   : {admin_created}/{admin_existing}")
        print(f"学员分配完成        : {len(assignments)}")
        print(f"计划新增/更新       : {plan_inserted}/{plan_updated}")
        print(f"排行榜刷新周期      : {period_start} ~ {period_end}")
        print("=" * 72)
        print(f"统一密码            : {args.password}")
        print("示例账号            :")
        if admin_ids:
            print("  - admin_auto_001")
        if coach_ids:
            print("  - coach_auto_001")

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
