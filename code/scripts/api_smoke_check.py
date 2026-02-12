import requests
import pymysql
import sys
from datetime import date, timedelta


BASE = 'http://localhost:8080'
PASSWORD = '123456'


def pick_login_user(cursor, role):
    cursor.execute(
        "SELECT username FROM users WHERE user_role=%s ORDER BY id LIMIT 100",
        (role,),
    )
    usernames = [row[0] for row in cursor.fetchall()]
    for username in usernames:
        try:
            response = requests.post(
                f'{BASE}/api/auth/login',
                json={'username': username, 'password': PASSWORD},
                timeout=5,
            )
            payload = response.json()
            if payload.get('success') and payload.get('data', {}).get('token'):
                session = requests.Session()
                session.headers.update({'Authorization': 'Bearer ' + payload['data']['token']})
                return username, session
        except Exception:
            continue
    return None, None


def add_check(checks, role, method, path, params=None, json_body=None):
    checks.append((role, method, path, params, json_body))


def run_checks(role_sessions, coach_student_id):
    checks = []

    for path in [
        '/api/user/profile',
        '/api/admin/coaches',
        '/api/admin/students',
        '/api/analytics/dashboard',
        '/api/analytics/behavior',
        '/api/analytics/peak-hour',
        '/api/analytics/equipment-usage',
        '/api/analytics/exercise-preference',
        '/api/analytics/coach-workload',
        '/api/analytics/hourly-activity',
    ]:
        add_check(checks, 'ADMIN', 'GET', path)

    add_check(
        checks,
        'ADMIN',
        'GET',
        '/api/analytics/hourly-activity',
        {'startDate': str(date.today() - timedelta(days=29)), 'endDate': str(date.today())},
    )
    for kind in ['TOTAL_DURATION', 'TOTAL_CALORIES', 'WEIGHT_LOSS']:
        add_check(checks, 'ADMIN', 'GET', '/api/analytics/leaderboard', {'type': kind, 'limit': 10})

    for path in ['/api/user/profile', '/api/user/coach/students', '/api/plan/coach', '/api/analytics/coach-dashboard']:
        add_check(checks, 'COACH', 'GET', path)
    if coach_student_id:
        add_check(
            checks,
            'COACH',
            'POST',
            '/api/user/coach/todos/handle',
            json_body={'studentId': coach_student_id, 'todoDescription': 'api-smoke-check'},
        )

    for path in [
        '/api/user/profile',
        '/api/bodymetric/latest',
        '/api/bodymetric/history',
        '/api/exercise/statistics',
        '/api/exercise/records',
        '/api/plan/my',
        '/api/analytics/peak-hour',
        '/api/achievement/my',
        '/api/achievement/all',
    ]:
        add_check(checks, 'STUDENT', 'GET', path)

    add_check(
        checks,
        'STUDENT',
        'GET',
        '/api/analytics/fitness-effect',
        {'startDate': str(date.today() - timedelta(days=30)), 'endDate': str(date.today())},
    )
    for kind in ['TOTAL_DURATION', 'TOTAL_CALORIES', 'WEIGHT_LOSS']:
        add_check(checks, 'STUDENT', 'GET', '/api/analytics/leaderboard', {'type': kind, 'limit': 10})
    add_check(checks, 'STUDENT', 'GET', '/api/exercise-reference/list', {'pageNum': 1, 'pageSize': 10})
    add_check(checks, 'STUDENT', 'GET', '/api/exercise-reference/types')
    add_check(checks, 'STUDENT', 'GET', '/api/exercise-reference/body-parts')
    add_check(checks, 'STUDENT', 'GET', '/api/exercise-reference/levels')

    failures = []
    for role, method, path, params, json_body in checks:
        session = role_sessions.get(role)
        if not session:
            failures.append((role, path, 'NO_LOGIN', None))
            continue

        try:
            if method == 'GET':
                response = session.get(BASE + path, params=params, timeout=10)
            else:
                response = session.post(BASE + path, params=params, json=json_body, timeout=10)

            payload = response.json()
            if not payload.get('success'):
                failures.append((role, path, 'API_FAIL', payload.get('message')))
        except Exception as exc:
            failures.append((role, path, 'EXCEPTION', str(exc)))

    return checks, failures


def main():
    conn = pymysql.connect(
        host='localhost',
        user='root',
        password='123456',
        database='gym_fitness_analytics',
        charset='utf8mb4',
    )
    cursor = conn.cursor()

    role_sessions = {}
    for role in ['ADMIN', 'COACH', 'STUDENT']:
        username, session = pick_login_user(cursor, role)
        role_sessions[role] = session
        print(f'{role} login: {"OK" if session else "FAIL"} ({username})')

    coach_student_id = None
    if role_sessions.get('COACH'):
        try:
            data = role_sessions['COACH'].get(f'{BASE}/api/user/coach/students', timeout=10).json().get('data') or []
            coach_student_id = data[0]['id'] if data else None
        except Exception:
            coach_student_id = None

    checks, failures = run_checks(role_sessions, coach_student_id)
    print(f'TOTAL_CHECKS: {len(checks)}')
    print(f'FAIL_COUNT: {len(failures)}')
    for failure in failures:
        print(' | '.join(str(item) for item in failure))

    conn.close()

    if failures:
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
