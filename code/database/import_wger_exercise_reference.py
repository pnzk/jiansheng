import csv
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


LEVEL_BY_CATEGORY = {
    "Abs": "Beginner",
    "Arms": "Intermediate",
    "Back": "Intermediate",
    "Calves": "Intermediate",
    "Chest": "Intermediate",
    "Legs": "Intermediate",
    "Shoulders": "Intermediate",
}


BODY_PART_BY_CATEGORY = {
    "Abs": "核心",
    "Arms": "手臂",
    "Back": "背部",
    "Calves": "小腿",
    "Chest": "胸部",
    "Legs": "下肢",
    "Shoulders": "肩部",
}


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def normalize_equipment(raw):
    text = (raw or "").strip()
    if not text or text == "[]":
        return "无器材"
    cleaned = text.replace("[", "").replace("]", "").replace("'", "").strip()
    return cleaned or "无器材"


def normalize_description(name, category, equipment):
    parts = [
        f"{name} 标准训练动作",
        f"主要训练部位：{category or '未分类'}",
        f"推荐器材：{equipment}",
    ]
    return "；".join(parts)


def load_rows():
    if not WGER_CSV.exists():
        raise FileNotFoundError(f"wger csv not found: {WGER_CSV}")

    rows = []
    seen_names = set()
    with WGER_CSV.open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        for row in reader:
            category = (row.get("category_name") or "").strip()
            name = (row.get("name") or "").strip()
            if not category or not name:
                continue
            if name in seen_names:
                continue
            seen_names.add(name)

            equipment = normalize_equipment(row.get("equipment_names"))
            body_part = BODY_PART_BY_CATEGORY.get(category, "全身")
            level = LEVEL_BY_CATEGORY.get(category, "Intermediate")
            description = normalize_description(name, category, equipment)

            rows.append(
                {
                    "exercise_name_en": name[:200],
                    "exercise_type": category[:50],
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
        print(f"[OK] imported exercise_reference from wger: inserted={inserted}, skipped={skipped}, total_source={len(rows)}")
    finally:
        conn.close()


def main():
    rows = load_rows()
    import_rows(rows)


if __name__ == "__main__":
    main()
