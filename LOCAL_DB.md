# Local Database Setup

## What changed

- Local backend runs with the `local` Spring profile by default.
- `local` uses PostgreSQL and Flyway migrations.
- Tests use disposable PostgreSQL containers through Testcontainers.
- Seed data is loaded automatically on an empty database.

## 1. Start PostgreSQL

From the repository root:

```powershell
docker compose -f docker-compose.local.yml up -d
```

This starts PostgreSQL on `localhost:5432` with:

- database: `gonguham`
- username: `gonguham`
- password: `gonguham`

## 2. Optional environment overrides

If you want different credentials, copy `backend/.env.example` and export your own values:

```powershell
$env:GONGUHAM_DB_URL="jdbc:postgresql://localhost:5432/gonguham"
$env:GONGUHAM_DB_USERNAME="gonguham"
$env:GONGUHAM_DB_PASSWORD="gonguham"
```

## 3. Run the backend

```powershell
cd backend
.\gradlew.bat bootRun
```

Flyway will create the schema, then the application will seed the database with realistic demo users, studies, sessions, posts, and avatar/shop data.

## 4. Run tests

```powershell
cd backend
.\gradlew.bat test
```

Tests now boot against PostgreSQL as well, so schema and constraint behavior stays aligned with local and production.

## Useful seed accounts

- `leader@gonguham.app / gonguham123!`
- `member@gonguham.app / gonguham123!`
- `guest@gonguham.app / gonguham123!`
- `hana@gonguham.app / gonguham123!`
- `minho@gonguham.app / gonguham123!`
