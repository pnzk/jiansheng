import os
import random
import sys
import time
from datetime import date, timedelta

import pymysql
import requests


BASE = os.getenv("API_BASE_URL", "http://localhost:8080").rstrip("/")
DB_HOST = os.getenv("MYSQL_HOST", "localhost")
DB_PORT = int(os.getenv("MYSQL_PORT", "3306"))
DB_USER = os.getenv("MYSQL_USER", "root")
DB_PASSWORD = os.getenv("MYSQL_PASSWORD", "123456")
DB_NAME = os.getenv("MYSQL_DB", "gym_fitness_analytics")
DEFAULT_PASSWORD = os.getenv("SMOKE_PASSWORD", "123456")
TIMEOUT = float(os.getenv("API_TIMEOUT", "10"))


class Checker:
    def __init__(self):
        self.total = 0
        self.failures = []

    def run(self, name, fn):
        self.total += 1
        try:
            detail = fn()
            if detail is None:
                detail = "OK"
            print(f"PASS | {name} | {detail}")
        except Exception as exc:
            detail = str(exc)
            self.failures.append((name, detail))
            print(f"FAIL | {name} | {detail}")

    def summary(self):
        print(f"TOTAL_CHECKS: {self.total}")
        print(f"FAIL_COUNT: {len(self.failures)}")
        for name, detail in self.failures:
            print(f"FAIL_DETAIL | {name} | {detail}")
        return 1 if self.failures else 0


def ensure_json_response(response, method, path):
    try:
        return response.json()
    except Exception as exc:
        raise RuntimeError(f"{method} {path} returned non-JSON response: {exc}") from exc


def api_call(session, method, path, params=None, json_body=None):
    response = session.request(
        method=method,
        url=f"{BASE}{path}",
        params=params,
        json=json_body,
        timeout=TIMEOUT,
    )
    payload = ensure_json_response(response, method, path)
    if not payload.get("success"):
        raise RuntimeError(f"{method} {path} failed: code={payload.get('code')} message={payload.get('message')}")
    return payload.get("data")


def login_session(username, password):
    try:
        response = requests.post(
            f"{BASE}/api/auth/login",
            json={"username": username, "password": password},
            timeout=TIMEOUT,
        )
        payload = ensure_json_response(response, "POST", "/api/auth/login")
    except Exception:
        return None

    if not payload.get("success"):
        return None
    token = (payload.get("data") or {}).get("token")
    if not token:
        return None
    session = requests.Session()
    session.headers.update({"Authorization": f"Bearer {token}"})
    return session


def is_backend_available():
    try:
        requests.post(f"{BASE}/api/auth/login", json={"username": "", "password": ""}, timeout=2)
        return True
    except Exception:
        return False


def pick_login_user(cursor, role, extra_where="", extra_params=()):
    sql = (
        "SELECT username FROM users "
        "WHERE user_role=%s "
        f"{extra_where} "
        "ORDER BY id LIMIT 200"
    )
    cursor.execute(sql, (role, *extra_params))
    for (username,) in cursor.fetchall():
        session = login_session(username, DEFAULT_PASSWORD)
        if session is not None:
            return username, session
    return None, None


def rand_suffix():
    return f"{int(time.time())}{random.randint(1000, 9999)}"


def rand_phone():
    return "1" + str(random.randint(3000000000, 9999999999))


def query_user_by_username(cursor, username):
    cursor.execute(
        "SELECT id, username, email, phone, real_name, user_role, coach_id, fitness_goal "
        "FROM users WHERE username=%s LIMIT 1",
        (username,),
    )
    return cursor.fetchone()


def query_count(cursor, sql, params=()):
    cursor.execute(sql, params)
    result = cursor.fetchone()
    if not result:
        return 0
    return int(result[0] or 0)


def get_unused_measurement_date(cursor, user_id):
    for offset in range(365, 3650):
        target = date.today() - timedelta(days=offset)
        cursor.execute(
            "SELECT COUNT(*) FROM body_metrics WHERE user_id=%s AND measurement_date=%s",
            (user_id, target),
        )
        if int(cursor.fetchone()[0]) == 0:
            return target
    raise RuntimeError("No available measurement date for body_metric update test")


def main():
    conn = pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset="utf8mb4",
        autocommit=True,
    )
    cursor = conn.cursor()
    checker = Checker()

    if not is_backend_available():
        checker.total = 1
        checker.failures.append(("backend_available", f"{BASE} is unreachable, start backend first"))
        print(f"FAIL | backend_available | {BASE} is unreachable, start backend first")
        conn.close()
        return checker.summary()

    created_ids = {
        "coach_user_id": None,
        "student_user_id": None,
        "plan_id": None,
        "exercise_id": None,
    }

    admin_username, admin_session = pick_login_user(cursor, "ADMIN")
    coach_username, coach_session = pick_login_user(
        cursor,
        "COACH",
        "AND EXISTS (SELECT 1 FROM users s WHERE s.coach_id=users.id AND s.user_role='STUDENT')",
    )
    student_username, student_session = pick_login_user(cursor, "STUDENT")

    checker.run("login_admin", lambda: f"username={admin_username}" if admin_session else (_ for _ in ()).throw(RuntimeError("admin login failed")))
    checker.run("login_coach", lambda: f"username={coach_username}" if coach_session else (_ for _ in ()).throw(RuntimeError("coach login failed")))
    checker.run("login_student", lambda: f"username={student_username}" if student_session else (_ for _ in ()).throw(RuntimeError("student login failed")))

    if not admin_session or not coach_session or not student_session:
        print("FATAL: missing required role account login, stop CRUD checks")
        conn.close()
        return checker.summary() or 1

    suffix = rand_suffix()

    # Preload coach list for student assignment.
    coaches = api_call(admin_session, "GET", "/api/admin/coaches") or []
    if not coaches:
        checker.run("admin_coach_list_non_empty", lambda: (_ for _ in ()).throw(RuntimeError("no coach available")))
        conn.close()
        return checker.summary() or 1
    default_coach_id = coaches[0]["id"]
    reassigned_coach_id = coaches[1]["id"] if len(coaches) > 1 else default_coach_id

    coach_username_new = f"crud_coach_{suffix}"
    coach_email_new = f"{coach_username_new}@example.com"
    student_username_new = f"crud_student_{suffix}"
    student_email_new = f"{student_username_new}@example.com"

    checker.run(
        "admin_create_coach",
        lambda: (
            api_call(
                admin_session,
                "POST",
                "/api/admin/coaches",
                json_body={
                    "username": coach_username_new,
                    "password": DEFAULT_PASSWORD,
                    "realName": "CRUD Coach",
                    "email": coach_email_new,
                    "phone": rand_phone(),
                    "age": 30,
                    "gender": "MALE",
                },
            ),
            "created",
        )[1],
    )

    def _verify_created_coach():
        row = query_user_by_username(cursor, coach_username_new)
        if row is None:
            raise RuntimeError("coach row not found in DB")
        if str(row[5]).upper() != "COACH":
            raise RuntimeError(f"unexpected role={row[5]}")
        created_ids["coach_user_id"] = int(row[0])
        return f"id={row[0]}"

    checker.run("admin_create_coach_db_consistency", _verify_created_coach)

    checker.run(
        "admin_update_coach",
        lambda: (
            api_call(
                admin_session,
                "PUT",
                f"/api/admin/coaches/{created_ids['coach_user_id']}",
                json_body={
                    "username": coach_username_new,
                    "password": None,
                    "realName": "CRUD Coach Updated",
                    "email": coach_email_new,
                    "phone": rand_phone(),
                    "age": 31,
                    "gender": "FEMALE",
                },
            ),
            "updated",
        )[1],
    )

    def _verify_updated_coach():
        row = query_user_by_username(cursor, coach_username_new)
        if row is None:
            raise RuntimeError("coach row missing after update")
        if row[4] != "CRUD Coach Updated":
            raise RuntimeError(f"real_name not updated: {row[4]}")
        return f"real_name={row[4]}"

    checker.run("admin_update_coach_db_consistency", _verify_updated_coach)

    checker.run(
        "admin_delete_coach",
        lambda: (
            api_call(admin_session, "DELETE", f"/api/admin/coaches/{created_ids['coach_user_id']}"),
            "deleted",
        )[1],
    )

    def _verify_deleted_coach():
        count = query_count(cursor, "SELECT COUNT(*) FROM users WHERE id=%s", (created_ids["coach_user_id"],))
        if count != 0:
            raise RuntimeError(f"coach still exists, count={count}")
        created_ids["coach_user_id"] = None
        return "deleted_in_db"

    checker.run("admin_delete_coach_db_consistency", _verify_deleted_coach)

    checker.run(
        "admin_create_student",
        lambda: (
            api_call(
                admin_session,
                "POST",
                "/api/admin/students",
                json_body={
                    "username": student_username_new,
                    "password": DEFAULT_PASSWORD,
                    "realName": "CRUD Student",
                    "email": student_email_new,
                    "phone": rand_phone(),
                    "age": 24,
                    "gender": "MALE",
                    "fitnessGoal": "WEIGHT_LOSS",
                    "coachId": default_coach_id,
                },
            ),
            "created",
        )[1],
    )

    def _verify_created_student():
        row = query_user_by_username(cursor, student_username_new)
        if row is None:
            raise RuntimeError("student row not found in DB")
        if str(row[5]).upper() != "STUDENT":
            raise RuntimeError(f"unexpected role={row[5]}")
        if int(row[6] or 0) != int(default_coach_id):
            raise RuntimeError(f"coach_id mismatch, expected={default_coach_id} actual={row[6]}")
        created_ids["student_user_id"] = int(row[0])
        return f"id={row[0]}"

    checker.run("admin_create_student_db_consistency", _verify_created_student)

    checker.run(
        "admin_update_student",
        lambda: (
            api_call(
                admin_session,
                "PUT",
                f"/api/admin/students/{created_ids['student_user_id']}",
                json_body={
                    "username": student_username_new,
                    "password": None,
                    "realName": "CRUD Student Updated",
                    "email": student_email_new,
                    "phone": rand_phone(),
                    "age": 25,
                    "gender": "FEMALE",
                    "fitnessGoal": "MUSCLE_GAIN",
                    "coachId": default_coach_id,
                },
            ),
            "updated",
        )[1],
    )

    def _verify_updated_student():
        row = query_user_by_username(cursor, student_username_new)
        if row is None:
            raise RuntimeError("student row missing after update")
        if row[4] != "CRUD Student Updated":
            raise RuntimeError(f"real_name not updated: {row[4]}")
        if str(row[7]).upper() != "MUSCLE_GAIN":
            raise RuntimeError(f"fitness_goal not updated: {row[7]}")
        return f"fitness_goal={row[7]}"

    checker.run("admin_update_student_db_consistency", _verify_updated_student)

    checker.run(
        "admin_assign_student_coach",
        lambda: (
            api_call(
                admin_session,
                "PUT",
                f"/api/admin/students/{created_ids['student_user_id']}/assign-coach",
                json_body={"coachId": reassigned_coach_id},
            ),
            f"coach_id={reassigned_coach_id}",
        )[1],
    )

    def _verify_student_coach_assigned():
        cursor.execute("SELECT coach_id FROM users WHERE id=%s", (created_ids["student_user_id"],))
        row = cursor.fetchone()
        if row is None:
            raise RuntimeError("student row missing after assign")
        actual = row[0]
        if int(actual or 0) != int(reassigned_coach_id):
            raise RuntimeError(f"coach_id mismatch, expected={reassigned_coach_id} actual={actual}")
        return f"coach_id={actual}"

    checker.run("admin_assign_student_coach_db_consistency", _verify_student_coach_assigned)

    checker.run(
        "admin_delete_student",
        lambda: (
            api_call(admin_session, "DELETE", f"/api/admin/students/{created_ids['student_user_id']}"),
            "deleted",
        )[1],
    )

    def _verify_deleted_student():
        count = query_count(cursor, "SELECT COUNT(*) FROM users WHERE id=%s", (created_ids["student_user_id"],))
        if count != 0:
            raise RuntimeError(f"student still exists, count={count}")
        created_ids["student_user_id"] = None
        return "deleted_in_db"

    checker.run("admin_delete_student_db_consistency", _verify_deleted_student)

    # Coach training plan CRUD
    coach_students = api_call(coach_session, "GET", "/api/user/coach/students") or []
    if not coach_students:
        checker.run("coach_has_students_for_plan_crud", lambda: (_ for _ in ()).throw(RuntimeError("coach has no students")))
    else:
        student_id_for_plan = coach_students[0]["id"]

        def _create_plan():
            data = api_call(
                coach_session,
                "POST",
                "/api/plan/create",
                json_body={
                    "studentId": student_id_for_plan,
                    "planName": f"CRUD Plan {suffix}",
                    "goalType": "WEIGHT_LOSS",
                    "targetValue": 3.0,
                    "startDate": str(date.today()),
                    "endDate": str(date.today() + timedelta(days=14)),
                    "weeklySchedule": "Mon:Run;Wed:Strength;Fri:Cycle",
                    "description": "CRUD consistency test",
                },
            )
            created_ids["plan_id"] = int(data["id"])
            return f"plan_id={created_ids['plan_id']}"

        checker.run("coach_create_plan", _create_plan)

        def _verify_plan_created():
            count = query_count(cursor, "SELECT COUNT(*) FROM training_plans WHERE id=%s", (created_ids["plan_id"],))
            if count != 1:
                raise RuntimeError(f"plan not found in DB, count={count}")
            return "exists_in_db"

        checker.run("coach_create_plan_db_consistency", _verify_plan_created)

        if created_ids["plan_id"] is None:
            checker.run(
                "coach_plan_followup_checks",
                lambda: (_ for _ in ()).throw(RuntimeError("skipped because create_plan failed")),
            )
            goto_like_skip = True
        else:
            goto_like_skip = False

        if not goto_like_skip:
            checker.run(
                "coach_update_plan",
                lambda: (
                    api_call(
                        coach_session,
                        "PUT",
                        f"/api/plan/{created_ids['plan_id']}",
                        json_body={
                            "planName": f"CRUD Plan Updated {suffix}",
                            "goalType": "FAT_LOSS",
                            "targetValue": 2.5,
                            "endDate": str(date.today() + timedelta(days=21)),
                            "weeklySchedule": "Tue:Run;Thu:Strength",
                            "description": "updated by CRUD check",
                            "status": "ACTIVE",
                        },
                    ),
                    "updated",
                )[1],
            )

            checker.run(
                "coach_update_plan_progress",
                lambda: (
                    api_call(
                        coach_session,
                        "PUT",
                        f"/api/plan/{created_ids['plan_id']}/progress",
                        json_body={"completionRate": 66.0},
                    ),
                    "progress=66",
                )[1],
            )

            def _verify_plan_updated():
                cursor.execute(
                    "SELECT plan_name, goal_type, completion_rate FROM training_plans WHERE id=%s",
                    (created_ids["plan_id"],),
                )
                row = cursor.fetchone()
                if row is None:
                    raise RuntimeError("plan missing after update")
                if "Updated" not in str(row[0]):
                    raise RuntimeError(f"plan_name not updated: {row[0]}")
                if str(row[1]).upper() != "FAT_LOSS":
                    raise RuntimeError(f"goal_type not updated: {row[1]}")
                completion_rate = float(row[2] or 0)
                if abs(completion_rate - 66.0) > 0.01:
                    raise RuntimeError(f"completion_rate mismatch: {completion_rate}")
                return f"completion_rate={completion_rate}"

            checker.run("coach_update_plan_db_consistency", _verify_plan_updated)

            checker.run(
                "coach_delete_plan",
                lambda: (
                    api_call(coach_session, "DELETE", f"/api/plan/{created_ids['plan_id']}"),
                    "deleted",
                )[1],
            )

            def _verify_plan_deleted():
                count = query_count(cursor, "SELECT COUNT(*) FROM training_plans WHERE id=%s", (created_ids["plan_id"],))
                if count != 0:
                    raise RuntimeError(f"plan still exists, count={count}")
                created_ids["plan_id"] = None
                return "deleted_in_db"

            checker.run("coach_delete_plan_db_consistency", _verify_plan_deleted)

    # Student exercise + body metric CRUD
    student_profile = api_call(student_session, "GET", "/api/user/profile")
    student_id = int(student_profile["id"])

    def _create_exercise():
        data = api_call(
            student_session,
            "POST",
            "/api/exercise/add",
            json_body={
                "exerciseType": "RUNNING",
                "exerciseDate": str(date.today()),
                "durationMinutes": 36,
                "caloriesBurned": 280.5,
                "averageHeartRate": 128,
                "maxHeartRate": 152,
                "equipmentUsed": "TREADMILL",
                "notes": "crud consistency test",
            },
        )
        created_ids["exercise_id"] = int(data["id"])
        return f"exercise_id={created_ids['exercise_id']}"

    checker.run("student_add_exercise", _create_exercise)

    def _verify_exercise_created():
        count = query_count(
            cursor,
            "SELECT COUNT(*) FROM exercise_records WHERE id=%s AND user_id=%s",
            (created_ids["exercise_id"], student_id),
        )
        if count != 1:
            raise RuntimeError(f"exercise row not found in DB, count={count}")
        return "exists_in_db"

    checker.run("student_add_exercise_db_consistency", _verify_exercise_created)

    checker.run(
        "student_get_exercise_records_contains_new",
        lambda: (
            (lambda records: (
                (_ for _ in ()).throw(RuntimeError("new exercise record not found in records response"))
                if not any(int(item["id"]) == created_ids["exercise_id"] for item in (records or []))
                else f"records={len(records or [])}"
            ))(
                api_call(
                    student_session,
                    "GET",
                    "/api/exercise/records",
                    params={"startDate": str(date.today() - timedelta(days=1)), "endDate": str(date.today())},
                )
            )
        ),
    )

    checker.run(
        "student_delete_exercise",
        lambda: (
            api_call(student_session, "DELETE", f"/api/exercise/{created_ids['exercise_id']}"),
            "deleted",
        )[1],
    )

    def _verify_exercise_deleted():
        count = query_count(cursor, "SELECT COUNT(*) FROM exercise_records WHERE id=%s", (created_ids["exercise_id"],))
        if count != 0:
            raise RuntimeError(f"exercise still exists, count={count}")
        created_ids["exercise_id"] = None
        return "deleted_in_db"

    checker.run("student_delete_exercise_db_consistency", _verify_exercise_deleted)

    measurement_date = get_unused_measurement_date(cursor, student_id)

    checker.run(
        "student_add_body_metric",
        lambda: (
            api_call(
                student_session,
                "POST",
                "/api/bodymetric/add",
                json_body={
                    "measurementDate": str(measurement_date),
                    "weightKg": 70.2,
                    "bodyFatPercentage": 19.8,
                    "heightCm": 172.0,
                    "muscleMassKg": 50.1,
                },
            ),
            f"measurement_date={measurement_date}",
        )[1],
    )

    checker.run(
        "student_update_body_metric_same_date",
        lambda: (
            api_call(
                student_session,
                "POST",
                "/api/bodymetric/add",
                json_body={
                    "measurementDate": str(measurement_date),
                    "weightKg": 69.6,
                    "bodyFatPercentage": 19.2,
                    "heightCm": 172.0,
                    "muscleMassKg": 50.4,
                },
            ),
            "updated_same_day",
        )[1],
    )

    def _verify_body_metric_consistency():
        cursor.execute(
            "SELECT COUNT(*), MIN(weight_kg), MAX(weight_kg) FROM body_metrics "
            "WHERE user_id=%s AND measurement_date=%s",
            (student_id, measurement_date),
        )
        row = cursor.fetchone()
        count = int(row[0] or 0)
        if count != 1:
            raise RuntimeError(f"body_metrics row count should be 1, actual={count}")
        max_weight = float(row[2] or 0)
        if abs(max_weight - 69.6) > 0.01:
            raise RuntimeError(f"body_metrics weight not updated, actual={max_weight}")
        return f"count={count}, weight={max_weight}"

    checker.run("student_body_metric_db_consistency", _verify_body_metric_consistency)

    checker.run(
        "student_get_body_metric_history_contains_new",
        lambda: (
            (lambda rows: (
                (_ for _ in ()).throw(RuntimeError("measurement date not found in history response"))
                if not any(str(item.get("measurementDate")) == str(measurement_date) for item in (rows or []))
                else f"records={len(rows or [])}"
            ))(
                api_call(
                    student_session,
                    "GET",
                    "/api/bodymetric/history",
                    params={"startDate": str(measurement_date), "endDate": str(measurement_date)},
                )
            )
        ),
    )

    # User profile + privacy checks
    checker.run(
        "student_update_profile",
        lambda: (
            (lambda profile: (
                api_call(
                    student_session,
                    "PUT",
                    "/api/user/profile",
                    json_body={
                        "realName": profile.get("realName") or profile.get("username") or "Student",
                        "email": profile.get("email"),
                        "phone": profile.get("phone"),
                        "age": profile.get("age"),
                        "gender": profile.get("gender") or "MALE",
                        "fitnessGoal": profile.get("fitnessGoal"),
                    },
                ),
                "updated",
            ))(api_call(student_session, "GET", "/api/user/profile"))[1],
        ),
    )

    def _toggle_privacy():
        profile = api_call(student_session, "GET", "/api/user/profile")
        original_show = bool(profile.get("showInLeaderboard"))
        original_allow = bool(profile.get("allowCoachView"))

        api_call(
            student_session,
            "PUT",
            "/api/user/privacy",
            json_body={
                "showInLeaderboard": not original_show,
                "allowCoachView": not original_allow,
            },
        )
        changed = api_call(student_session, "GET", "/api/user/profile")
        if bool(changed.get("showInLeaderboard")) == original_show:
            raise RuntimeError("showInLeaderboard was not toggled")
        if bool(changed.get("allowCoachView")) == original_allow:
            raise RuntimeError("allowCoachView was not toggled")

        # Revert to keep test side-effects minimal.
        api_call(
            student_session,
            "PUT",
            "/api/user/privacy",
            json_body={
                "showInLeaderboard": original_show,
                "allowCoachView": original_allow,
            },
        )
        return "toggled_and_reverted"

    checker.run("student_update_privacy", _toggle_privacy)

    # Best-effort cleanup if any previous step failed before API deletes.
    try:
        if created_ids["plan_id"] is not None:
            cursor.execute("DELETE FROM training_plans WHERE id=%s", (created_ids["plan_id"],))
        if created_ids["exercise_id"] is not None:
            cursor.execute("DELETE FROM exercise_records WHERE id=%s", (created_ids["exercise_id"],))
        if created_ids["student_user_id"] is not None:
            cursor.execute("DELETE FROM users WHERE id=%s", (created_ids["student_user_id"],))
        if created_ids["coach_user_id"] is not None:
            cursor.execute("DELETE FROM users WHERE id=%s", (created_ids["coach_user_id"],))
    except Exception as cleanup_exc:
        print(f"WARN | cleanup | {cleanup_exc}")

    conn.close()
    return checker.summary()


if __name__ == "__main__":
    sys.exit(main())
