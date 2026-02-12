# Run Guide (IDEA One-Click)

## Required Software
- Java JDK 17+ (JDK 21 is OK)
- Maven 3.6+
- Node.js 16+ (includes npm)
- MySQL 8.0+
- Navicat (optional, for DB management)

## One-Click Start in IDEA
1) Open the project root (the folder containing `code/`) in IDEA.
2) Select the run configuration `StartAll` and click Run.
   - This now runs `code\\start-all.bat`, which will:
     - run Scheme-B DB migration (schema + real data import + page artifacts rebuild)
     - start backend (`8080`) and frontend (`3000`)
     - open browser automatically
3) Open the browser if it is not auto-opened: `http://localhost:3000`.

Notes:
- All paths are relative to the project root; you can move the whole folder to another PC and run directly.

## Auto Setup in IDEA
If you want to run the installer from IDEA, use the run configuration `AutoSetup`.
It runs the auto-setup script only and then exits.

## Database Initialization (first time)
Run once:
```
code\init-database.bat
```
This creates schema and base tables.

## One-Click Migration + Start (recommended for new PC)
Run:
```
code\migrate-and-start.bat
```

Common options:
- `--large` generate and import 10,000-user large dataset
- `--clean-first` clean dataset folder then import
- `--with-api-smoke` run API smoke check after migration

## Command Reference (IDEA + CLI)
- IDEA `StartAll` = `code\start-all.bat`
- One-click migrate + start = `code\migrate-and-start.bat`
- Migrate only (no service start) = `code\migrate-and-start.bat --no-start --no-pause`
- Scheme-B migrate only = `code\migrate-scheme-b.bat --no-pause`
- Large dataset migrate = `code\migrate-and-start.bat --large`
- Re-clean dataset then migrate = `code\migrate-and-start.bat --clean-first`
- Check environment (CI/no block) = `code\check-env.bat --no-pause`
- Install dependencies (CI/no block) = `code\install-deps.bat --no-pause`
- Import cleaned data (CI/no block) = `code\import-data.bat --no-pause`
- Import large data (CI/no block) = `code\import-large-data.bat --no-pause`
- Stop services (no block) = `code\stop.bat --no-pause`

Typical new-PC flow:
```
code\start-all.bat
```

Optional DB env overrides before running commands:
```
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASSWORD=123456
set MYSQL_DB=gym_fitness_analytics
```

## Rebuild Real Data + Validate APIs (recommended)
Run:
```
code\seed-all-db-artifacts.bat
```
What it does:
- Seed coaches/admin and assign students
- Ensure recent exercise/body-metric coverage
- Rebuild achievements and analytics artifacts
- Backfill `exercise_records.created_at`
- Run API smoke checks for admin/coach/student endpoints

If any step fails, the script exits with non-zero code.

## Auth Smoke Test（注册/登录落库）

Run:
```
code\auth-smoke-test.bat
```

It validates:
- register new STUDENT account via `/api/auth/register`
- login by username via `/api/auth/login`
- login by email via `/api/auth/login`
- DB row exists in `users`
- password stored as BCrypt hash

Optional env overrides:
- `API_BASE_URL` (default `http://localhost:8080`)
- `MYSQL_HOST` / `MYSQL_USER` / `MYSQL_PASSWORD` / `MYSQL_DB`

## Navicat Connection
- host: localhost
- port: 3306
- user: root
- password: 123456
- database: gym_fitness_analytics

## Default Login Accounts

`init-database.bat` in clean mode does **not** create test users.

After running `seed-all-db-artifacts.bat`, default generated accounts are:
- admin: `admin_auto_001` / `123456`
- coach: `coach_auto_001` / `123456`
- student: auto-generated real student accounts (password `123456`)

You can query accounts directly from DB (`users` table), or use:
```
python code\scripts\api_smoke_check.py
```
to verify end-to-end login/API availability.

## Auto Setup
Run `code\auto-setup.bat` (or `auto-setup.ps1`) to install missing tools automatically. It checks and installs:
- JDK (Temurin 17)
- Maven
- Node.js (npm)
- MySQL

Notes:
- Uses `winget` if available, otherwise Chocolatey.
- Run PowerShell/IDEA as Administrator for installs.
- If installs new software, restart IDEA so PATH is refreshed.
- You can force the installer with `INSTALLER=winget` or `INSTALLER=choco`.
