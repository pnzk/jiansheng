import random
import string
import sys
import os
from datetime import datetime

try:
    import pymysql
except Exception:
    pymysql = None

try:
    import requests
except Exception:
    requests = None


def rand_suffix():
    now = datetime.now().strftime('%Y%m%d%H%M%S')
    tail = ''.join(random.choices(string.digits, k=4))
    return f'{now}{tail}'


def fail(message):
    print(f'[X] {message}')
    return 1


def ok(message):
    print(f'[OK] {message}')


def main():
    if requests is None:
        return fail('缺少依赖 requests，请先执行: pip install requests')
    if pymysql is None:
        return fail('缺少依赖 pymysql，请先执行: pip install pymysql')

    base_url = os.getenv('API_BASE_URL', 'http://localhost:8080')
    db_host = os.getenv('MYSQL_HOST', 'localhost')
    db_user = os.getenv('MYSQL_USER', 'root')
    db_password = os.getenv('MYSQL_PASSWORD', '123456')
    db_name = os.getenv('MYSQL_DB', 'gym_fitness_analytics')
    password = os.getenv('SMOKE_PASSWORD', 'Abc12345')

    print('========================================')
    print('  Auth Smoke Test (Register/Login/DB)')
    print('========================================')
    print(f'BASE_URL : {base_url}')
    print(f'DB       : {db_user}@{db_host}/{db_name}')
    print()

    print('[1/6] 检查后端接口连通性...')
    try:
        response = requests.post(
            f'{base_url}/api/auth/login',
            json={'username': 'healthcheck', 'password': 'x'},
            timeout=5,
        )
        _ = response.json()
    except Exception:
        return fail(f'无法连接后端：{base_url}（请先启动后端）')
    ok('后端可访问')
    print()

    suffix = rand_suffix()
    username = f'smoke_user_{suffix}'
    email = f'{username}@example.com'
    phone = f'13{suffix[-9:]}'

    print('[2/6] 注册学员账号...')
    register_payload = {
        'username': username,
        'password': password,
        'realName': 'Auth Smoke Student',
        'email': email,
        'phone': phone,
        'age': 24,
        'gender': 'MALE',
        'role': 'STUDENT',
        'fitnessGoal': 'FAT_LOSS',
    }
    try:
        response = requests.post(
            f'{base_url}/api/auth/register',
            json=register_payload,
            timeout=10,
        )
        payload = response.json()
    except Exception as exc:
        return fail(f'注册请求失败：{exc}')

    if not payload.get('success'):
        return fail(f"注册失败：{payload.get('message')}")
    ok(f'注册成功：{username}')
    print()

    print('[3/6] 用用户名登录...')
    try:
        response = requests.post(
            f'{base_url}/api/auth/login',
            json={'username': username, 'password': password},
            timeout=10,
        )
        payload = response.json()
    except Exception as exc:
        return fail(f'用户名登录请求失败：{exc}')

    if not payload.get('success'):
        return fail(f"用户名登录失败：{payload.get('message')}")

    data = payload.get('data') or {}
    if not data.get('token'):
        return fail('用户名登录失败：未返回 token')
    ok(f"用户名登录通过，userId={data.get('userId')}")
    print()

    print('[4/6] 用邮箱登录...')
    try:
        response = requests.post(
            f'{base_url}/api/auth/login',
            json={'username': email, 'password': password},
            timeout=10,
        )
        payload = response.json()
    except Exception as exc:
        return fail(f'邮箱登录请求失败：{exc}')

    if not payload.get('success'):
        return fail(f"邮箱登录失败：{payload.get('message')}")

    data = payload.get('data') or {}
    if not data.get('token'):
        return fail('邮箱登录失败：未返回 token')
    ok(f"邮箱登录通过，userId={data.get('userId')}")
    print()

    print('[5/6] 校验数据库落库...')
    try:
        connection = pymysql.connect(
            host=db_host,
            user=db_user,
            password=db_password,
            database=db_name,
            charset='utf8mb4',
        )
    except Exception as exc:
        return fail(f'数据库连接失败：{exc}')

    try:
        with connection.cursor() as cursor:
            cursor.execute(
                'SELECT id, password FROM users WHERE username=%s',
                (username,),
            )
            row = cursor.fetchone()

        if not row:
            return fail(f'数据库校验失败：users 表未找到 {username}')

        user_id, password_hash = row
        ok(f'users 表已落库，userId={user_id}')

        if not password_hash or not str(password_hash).startswith(('$2a$', '$2b$', '$2y$')):
            return fail(f'密码哈希校验失败：{password_hash}')
        ok('密码哈希为 BCrypt')
    finally:
        connection.close()

    print()
    print('[6/6] 测试完成。')
    print('----------------------------------------')
    print('新注册账号（可用于前端登录验证）：')
    print(f'username : {username}')
    print(f'email    : {email}')
    print(f'password : {password}')
    print('----------------------------------------')
    print('[PASS] 认证链路正常：注册、登录、数据库落库均通过。')
    return 0


if __name__ == '__main__':
    sys.exit(main())
