import argparse
import math
import random
from datetime import datetime, timedelta
from pathlib import Path

import pandas as pd


RANDOM_SEED = 2026
random.seed(RANDOM_SEED)


SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
WORKSPACE_ROOT = PROJECT_ROOT.parent

DEFAULT_DATASET_DIR = WORKSPACE_ROOT / "健身房运动数据集"
DEFAULT_OUTPUT_DIR = PROJECT_ROOT / "data-processing" / "cleaned"


def to_safe_str(value):
    if value is None or (isinstance(value, float) and math.isnan(value)):
        return ""
    return str(value).strip()


def to_int(value, default=0):
    try:
        if value is None or (isinstance(value, float) and math.isnan(value)):
            return default
        return int(float(value))
    except Exception:
        return default


def to_float(value, default=0.0):
    try:
        if value is None or (isinstance(value, float) and math.isnan(value)):
            return default
        return float(value)
    except Exception:
        return default


def normalize_gender(value):
    text = to_safe_str(value).lower()
    if text in {"female", "f", "女"}:
        return "女"
    return "男"


def normalize_age(value):
    age_map = {
        "15 to 18": 17,
        "19 to 25": 22,
        "26 to 30": 28,
        "30 to 40": 35,
        "40 and above": 45,
    }
    text = to_safe_str(value)
    if text in age_map:
        return age_map[text]
    age = to_int(value, 25)
    return min(max(age, 15), 70)


def normalize_goal_from_text(text):
    raw = to_safe_str(text).lower()
    if "muscle" in raw or "strength" in raw or "增肌" in raw:
        return "增肌"
    if "lose weight" in raw or "fat" in raw or "减重" in raw or "减脂" in raw:
        return "减重"
    return "减脂"


def normalize_exercise_type(text):
    raw = to_safe_str(text).lower()
    mapping = {
        "walking or jogging": "跑步",
        "jog": "跑步",
        "run": "跑步",
        "gym": "力量训练",
        "lifting weights": "力量训练",
        "strength": "力量训练",
        "yoga": "瑜伽",
        "swimming": "游泳",
        "team sport": "团体运动",
        "zumba": "跳操",
        "hiit": "HIIT",
        "cardio": "有氧训练",
    }
    for key, value in mapping.items():
        if key in raw:
            return value
    return "有氧训练"


def infer_duration_from_text(text):
    raw = to_safe_str(text).lower()
    if "3 hour" in raw:
        return 180
    if "2 hour" in raw:
        return 120
    if "1 hour" in raw:
        return 60
    if "30 minute" in raw:
        return 30
    if "don't really exercise" in raw:
        return 20
    return 45


def infer_frequency_per_week(text):
    raw = to_safe_str(text).lower()
    mapping = {
        "never": 0,
        "1 to 2 times a week": 2,
        "2 to 3 times a week": 3,
        "3 to 4 times a week": 4,
        "5 to 6 times a week": 6,
        "everyday": 7,
    }
    return mapping.get(raw, 2)


def random_date_within(days=120):
    return (datetime.now() - timedelta(days=random.randint(0, days))).date()


def build_reference_data(mega_df):
    cleaned = mega_df.rename(
        columns={
            "Title": "exercise_name_en",
            "Type": "exercise_type",
            "BodyPart": "body_part",
            "Equipment": "equipment",
            "Level": "level",
            "Desc": "description",
        }
    )
    cleaned = cleaned[[
        "exercise_name_en",
        "exercise_type",
        "body_part",
        "equipment",
        "level",
        "description",
    ]]
    cleaned = cleaned.dropna(subset=["exercise_name_en"])
    cleaned["exercise_name_en"] = cleaned["exercise_name_en"].astype(str).str.strip()
    cleaned = cleaned[cleaned["exercise_name_en"] != ""]
    cleaned = cleaned.drop_duplicates(subset=["exercise_name_en"], keep="first")

    for column in ["exercise_type", "body_part", "equipment", "level", "description"]:
        cleaned[column] = cleaned[column].astype(str).replace("nan", "").str.strip()

    return cleaned


def generate_from_fitness_analysis(df, start_user_id, start_record_id, start_metric_id):
    users = []
    records = []
    metrics = []

    for idx, row in df.iterrows():
        user_id = start_user_id + idx

        real_name = to_safe_str(row.get("Your name ")) or f"问卷用户{user_id}"
        gender = normalize_gender(row.get("Your gender "))
        age = normalize_age(row.get("Your age "))
        motivation = row.get("What motivates you to exercise?         (Please select all that applies )")
        fitness_goal = normalize_goal_from_text(motivation)

        username = f"survey_user_{user_id}"
        users.append(
            {
                "user_id": user_id,
                "username": username,
                "real_name": real_name,
                "email": f"{username}@gym.local",
                "phone": f"13{random.randint(100000000, 999999999)}",
                "age": age,
                "gender": gender,
                "fitness_goal": fitness_goal,
                "role": "STUDENT",
                "created_at": (datetime.now() - timedelta(days=random.randint(90, 360))).strftime("%Y-%m-%d %H:%M:%S"),
            }
        )

        base_height = random.uniform(160, 183) if gender == "男" else random.uniform(150, 172)
        base_weight = random.uniform(58, 88) if gender == "男" else random.uniform(45, 72)
        if fitness_goal == "减重":
            current_weight = base_weight - random.uniform(0.3, 3.0)
        elif fitness_goal == "增肌":
            current_weight = base_weight + random.uniform(0.2, 2.0)
        else:
            current_weight = base_weight + random.uniform(-1.0, 1.0)

        base_fat = random.uniform(16, 28) if gender == "男" else random.uniform(21, 33)
        current_fat = max(base_fat - random.uniform(0.2, 2.5), 8)

        for offset, (weight, fat) in enumerate([(base_weight, base_fat), (current_weight, current_fat)]):
            measure_date = datetime.now().date() - timedelta(days=120 - offset * 90)
            metrics.append(
                {
                    "metric_id": start_metric_id + len(metrics) + 1,
                    "user_id": user_id,
                    "measurement_date": measure_date.strftime("%Y-%m-%d"),
                    "weight_kg": round(weight, 1),
                    "body_fat_percentage": round(fat, 1),
                    "height_cm": round(base_height, 1),
                    "muscle_mass_kg": round(weight * (1 - fat / 100) * 0.5, 1),
                }
            )

        freq = infer_frequency_per_week(row.get("How often do you exercise?"))
        duration = infer_duration_from_text(row.get("How long do you spend exercising per day ?"))
        exercise_type = normalize_exercise_type(
            row.get("What form(s) of exercise do you currently participate in ?                        (Please select all that apply)")
        )

        total_sessions = max(freq * 6, 1 if freq > 0 else 0)
        for _ in range(total_sessions):
            duration_minutes = max(int(random.gauss(duration, 10)), 15)
            calories = max(int(duration_minutes * random.uniform(5.0, 9.5)), 80)
            avg_hr = random.randint(95, 145)
            max_hr = min(avg_hr + random.randint(15, 35), 190)

            records.append(
                {
                    "record_id": start_record_id + len(records) + 1,
                    "user_id": user_id,
                    "exercise_type": exercise_type,
                    "exercise_date": random_date_within(120).strftime("%Y-%m-%d"),
                    "duration_minutes": duration_minutes,
                    "calories_burned": calories,
                    "average_heart_rate": avg_hr,
                    "max_heart_rate": max_hr,
                    "equipment_used": "综合训练器械",
                }
            )

    return users, records, metrics


def generate_from_gym_members(df, start_user_id, start_record_id, start_metric_id):
    users = []
    records = []
    metrics = []

    for idx, row in df.iterrows():
        user_id = start_user_id + idx

        gender = normalize_gender(row.get("Gender"))
        age = min(max(to_int(row.get("Age"), 30), 16), 70)
        workout_type = to_safe_str(row.get("Workout_Type"))
        fat = to_float(row.get("Fat_Percentage"), 25.0)

        if workout_type.lower() == "strength":
            fitness_goal = "增肌"
        elif fat >= 26:
            fitness_goal = "减脂"
        else:
            fitness_goal = "减重"

        username = f"member_user_{user_id}"
        users.append(
            {
                "user_id": user_id,
                "username": username,
                "real_name": f"会员{user_id}",
                "email": f"{username}@gym.local",
                "phone": f"15{random.randint(100000000, 999999999)}",
                "age": age,
                "gender": gender,
                "fitness_goal": fitness_goal,
                "role": "STUDENT",
                "created_at": (datetime.now() - timedelta(days=random.randint(60, 300))).strftime("%Y-%m-%d %H:%M:%S"),
            }
        )

        weight = to_float(row.get("Weight (kg)"), 70.0)
        height_cm = to_float(row.get("Height (m)"), 1.70) * 100
        current_weight = weight + random.uniform(-1.8, 1.8)

        for offset, metric_weight in enumerate([weight, current_weight]):
            measure_date = datetime.now().date() - timedelta(days=90 - offset * 60)
            metric_fat = max(fat + random.uniform(-1.8, 1.8), 7)
            metrics.append(
                {
                    "metric_id": start_metric_id + len(metrics) + 1,
                    "user_id": user_id,
                    "measurement_date": measure_date.strftime("%Y-%m-%d"),
                    "weight_kg": round(metric_weight, 1),
                    "body_fat_percentage": round(metric_fat, 1),
                    "height_cm": round(height_cm, 1),
                    "muscle_mass_kg": round(metric_weight * (1 - metric_fat / 100) * 0.5, 1),
                }
            )

        frequency = min(max(to_int(row.get("Workout_Frequency (days/week)"), 3), 1), 7)
        duration_minutes = max(int(to_float(row.get("Session_Duration (hours)"), 1.0) * 60), 20)
        calories = max(int(to_float(row.get("Calories_Burned"), 350.0)), 80)
        avg_hr = min(max(to_int(row.get("Avg_BPM"), 120), 70), 190)
        max_hr = min(max(to_int(row.get("Max_BPM"), avg_hr + 10), avg_hr + 5), 210)
        records_count = frequency * 8

        for _ in range(records_count):
            records.append(
                {
                    "record_id": start_record_id + len(records) + 1,
                    "user_id": user_id,
                    "exercise_type": normalize_exercise_type(workout_type),
                    "exercise_date": random_date_within(120).strftime("%Y-%m-%d"),
                    "duration_minutes": max(int(random.gauss(duration_minutes, 8)), 15),
                    "calories_burned": max(int(random.gauss(calories, 80)), 60),
                    "average_heart_rate": avg_hr,
                    "max_heart_rate": max_hr,
                    "equipment_used": "心率设备",
                }
            )

    return users, records, metrics


def run_cleaning(dataset_dir: Path, output_dir: Path):
    fitness_path = dataset_dir / "fitness analysis.csv"
    members_path = dataset_dir / "gym_members_exercise_tracking.csv"
    mega_path = dataset_dir / "megaGymDataset.csv"

    for path in [fitness_path, members_path, mega_path]:
        if not path.exists():
            raise FileNotFoundError(f"数据文件不存在: {path}")

    fitness_df = pd.read_csv(fitness_path, encoding="utf-8-sig").drop_duplicates()
    members_df = pd.read_csv(members_path, encoding="utf-8-sig").drop_duplicates()
    mega_df = pd.read_csv(mega_path, encoding="utf-8-sig").drop_duplicates()

    users_a, records_a, metrics_a = generate_from_fitness_analysis(
        fitness_df,
        start_user_id=1,
        start_record_id=1,
        start_metric_id=1,
    )
    users_b, records_b, metrics_b = generate_from_gym_members(
        members_df,
        start_user_id=len(users_a) + 1,
        start_record_id=len(records_a) + 1,
        start_metric_id=len(metrics_a) + 1,
    )

    users_df = pd.DataFrame(users_a + users_b).drop_duplicates(subset=["username"], keep="first")
    records_df = pd.DataFrame(records_a + records_b)
    metrics_df = pd.DataFrame(metrics_a + metrics_b)
    reference_df = build_reference_data(mega_df)

    output_dir.mkdir(parents=True, exist_ok=True)
    users_df.to_csv(output_dir / "users.csv", index=False, encoding="utf-8-sig")
    records_df.to_csv(output_dir / "exercise_records.csv", index=False, encoding="utf-8-sig")
    metrics_df.to_csv(output_dir / "body_metrics.csv", index=False, encoding="utf-8-sig")
    reference_df.to_csv(output_dir / "exercise_reference.csv", index=False, encoding="utf-8-sig")

    print("=" * 70)
    print("清洗完成")
    print("=" * 70)
    print(f"dataset_dir: {dataset_dir}")
    print(f"output_dir : {output_dir}")
    print(f"users               : {len(users_df)}")
    print(f"exercise_records    : {len(records_df)}")
    print(f"body_metrics        : {len(metrics_df)}")
    print(f"exercise_reference  : {len(reference_df)}")


def parse_args():
    parser = argparse.ArgumentParser(description="清洗‘健身房运动数据集’并生成导入CSV")
    parser.add_argument(
        "--dataset-dir",
        default=str(DEFAULT_DATASET_DIR),
        help="原始数据集目录（包含 fitness analysis.csv / megaGymDataset.csv / gym_members_exercise_tracking.csv）",
    )
    parser.add_argument(
        "--output-dir",
        default=str(DEFAULT_OUTPUT_DIR),
        help="清洗后CSV输出目录",
    )
    return parser.parse_args()


def main():
    args = parse_args()
    dataset_dir = Path(args.dataset_dir)
    output_dir = Path(args.output_dir)
    run_cleaning(dataset_dir, output_dir)


if __name__ == "__main__":
    main()

