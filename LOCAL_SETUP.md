# Running Mahberix locally with Docker Compose

This starts PostgreSQL already filled with demo data (organizations, managers, employees, jobs,
applicants with AI overviews, blogs, events and more), and starts the API built from this repo.

> **Use the `serdesiyon` branch.** `main` is behind and doesn't match the seeded database.
> If you got this as a git checkout rather than the zip, run `git checkout serdesiyon` (then `git pull`) before anything else.
> The zip is already on `serdesiyon`; check with `git branch --show-current`.

## Prerequisites

- Docker with the Compose plugin (`docker compose version` should work)
- Node.js 20+ and pnpm, only if you also want the web frontend

## 1. Configure

The zip already includes a working `.env`, so you can skip this step. `docker-compose.yml`
overrides the `DB_*` values in it to point at the bundled Postgres.

Without a `.env` (a fresh git checkout), create one: `cp .env.example .env`, then set
`APP_JWT_SECRET` to any long random string (`openssl rand -base64 48`). Everything else is
optional; see [What works without keys](#what-works-without-keys).

## 2. Start the database and the API

```bash
docker compose up -d --build
```

The first build takes a few minutes because Maven downloads its dependencies.

On the very first start, Postgres loads `db-seed/cms3.sql` automatically. The dump already
includes the Flyway migration history, so you don't need to run any migration.

Check that it's up:

```bash
docker compose ps                         # db should be "healthy"
docker compose logs -f api                # wait for "Started AfrodebabCmsApiApplication"
curl http://localhost:8080/actuator/health
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Postgres: `localhost:5433`, database `cms3`, user `postgres`, password `postgres`

## 3. Log in

Every manager, vice manager and employee in the seed has the same password:

**Password:** `<ask the person who sent you this repo>`

Example accounts:

| Role | Email | Organization |
|---|---|---|
| Manager | `serdesiyon@gmail.com` | AfroDebab |
| Vice manager | `mekdes.alemu@example.com` | AfroDebab |
| Employee | `abebe.kebede@example.com` | |

Log in through Swagger (`POST /manager/auth/login` or `POST /employee/auth/login`). Copy the
returned token into **Authorize** as `Bearer <token>`, or log in through the frontend below.

## 4. Frontend (optional)

```bash
cd frontend
echo "NEXT_PUBLIC_CMS_BASE_URL=http://localhost:8080" > .env.local
pnpm install
pnpm dev
```

Open http://localhost:3000. The API already allows `http://localhost:3000` for CORS.

## What works without keys

| Feature | Needs in `.env` |
|---|---|
| Login, dashboards, HR, payroll, attendance, jobs, blogs, events, reviews | nothing extra |
| Existing applicants' resumes and AI overviews (read-only) | nothing, they're in the seed |
| New AI overviews for new applicants | `GEMINI_API_KEY` and the R2 keys |
| Uploading photos, logos, resumes | `S3_API`, `PUBLIC_DEVELOPMENT_URL`, `ACCESS_KEY_ID`, `SECRET_ACCESS_KEY` |
| Sending emails (OTP, invites, notifications) | `SENDGRID_*`; without it, sends fail, so signup/OTP flows won't complete (existing seeded accounts still log in) |
| Trello / GitHub / Google connect buttons | the matching client IDs/secrets plus `TOKEN_ENCRYPTION_KEY` |

Integration connections are cleared in the seed, so managers show as "not connected".

## Resetting

Start over from a clean seed. This deletes all data in the local DB:

```bash
docker compose down -v
docker compose up -d
```

Stop without losing data:

```bash
docker compose down
```

## Troubleshooting

- **Port already in use:** change the left side of `"8080:8080"` or `"5433:5432"` in `docker-compose.yml`.
- **The seed didn't load (empty tables):** the volume already existed. Run `docker compose down -v`, then start again.
- **The API exits right after starting:** run `docker compose logs api`. It's usually a missing `APP_JWT_SECRET`.
