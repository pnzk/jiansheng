import csv
from collections import defaultdict
from datetime import datetime
from pathlib import Path

import pymysql


DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "gym_fitness_analytics",
    "charset": "utf8mb4",
    "autocommit": False,
}


PROJECT_ROOT = Path(__file__).resolve().parents[1]
WGER_CSV = PROJECT_ROOT / "data-collection" / "data-collection" / "crawled_data" / "wger_exercises.csv"


BODY_PART_BY_CATEGORY = {
    "Abs": "核心",
    "Arms": "手臂",
    "Back": "背部",
    "Calves": "小腿",
    "Cardio": "全身",
    "Chest": "胸部",
    "Legs": "下肢",
    "Shoulders": "肩部",
}

LEVEL_BY_CATEGORY = {
    "Abs": "Beginner",
    "Arms": "Intermediate",
    "Back": "Intermediate",
    "Calves": "Intermediate",
    "Cardio": "Beginner",
    "Chest": "Intermediate",
    "Legs": "Intermediate",
    "Shoulders": "Intermediate",
}

CN_NAME_BY_CATEGORY = {
    "Abs": "核心训练",
    "Arms": "手臂训练",
    "Back": "背部训练",
    "Calves": "小腿训练",
    "Cardio": "有氧训练",
    "Chest": "胸部训练",
    "Legs": "下肢训练",
    "Shoulders": "肩部训练",
}


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def normalize_equipment(raw):
    text = (raw or "").strip()
    if not text or text == "[]":
        return "无器材"
    return text.replace("[", "").replace("]", "").replace("'", "").strip() or "无器材"


def load_seed_combinations():
    combinations = defaultdict(set)
    with WGER_CSV.open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        for row in reader:
            category = (row.get("category_name") or "").strip()
            if not category:
                continue
            combinations[category].add(normalize_equipment(row.get("equipment_names")))
    return combinations


def build_rows():
    combinations = load_seed_combinations()
    rows = []
    for category, equipment_set in combinations.items():
        body_part = BODY_PART_BY_CATEGORY.get(category, "全身")
        level = LEVEL_BY_CATEGORY.get(category, "Intermediate")
        base_name = CN_NAME_BY_CATEGORY.get(category, f"{category}训练")

        for index, equipment in enumerate(sorted(equipment_set), start=1):
            exercise_name = f"{base_name}{index:02d}"
            description = f"{base_name}标准动作，推荐器材：{equipment}，适合{body_part}强化与动作学习。"
            rows.append(
                {
                    "exercise_name_en": exercise_name[:200],
                    "exercise_type": base_name[:50],
                    "body_part": body_part[:50],
                    "equipment": equipment[:100],
                    "level": level[:50],
                    "description": description,
                }
            )
    return rows


def import_rows(rows):
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            inserted = 0
            skipped = 0
            for row in rows:
                cursor.execute(
                    "SELECT id FROM exercise_reference WHERE exercise_name_en = %s",
                    (row["exercise_name_en"],),
                )
                if cursor.fetchone():
                    skipped += 1
                    continue

                cursor.execute(
                    """
                    INSERT INTO exercise_reference (
                        exercise_name_en, exercise_type, body_part,
                        equipment, level, description, created_at
                    ) VALUES (%s,%s,%s,%s,%s,%s,%s)
                    """,
                    (
                        row["exercise_name_en"],
                        row["exercise_type"],
                        row["body_part"],
                        row["equipment"],
                        row["level"],
                        row["description"],
                        datetime.now(),
                    ),
                )
                inserted += 1
        conn.commit()
        print(f"[OK] expanded exercise_reference: inserted={inserted}, skipped={skipped}, total_generated={len(rows)}")
    finally:
        conn.close()


def main():
    rows = build_rows()
    import_rows(rows)


if __name__ == "__main__":
    main()
