# Ticket Tracker — Kanban Ticketing System

A three-tier Kanban ticket tracker: an **Angular 18** SPA, a **Java 21 / Spring Boot 3.3**
HTTP API, and a **PostgreSQL 16** database. Everything runs from the repository root with a
single command — no host-installed Node, JDK, or Postgres required.

**Features:** email/password auth with email verification and password reset · teams · epics ·
tickets on a draggable five-column Kanban board (filters + search) · comments (add/edit/delete) ·
ticket activity history.

---

## Prerequisites

- **Docker Desktop** (or Docker Engine) with **Docker Compose v2**. That's the only requirement.
- Works on Windows, macOS, and Linux. A modern desktop Chrome / Edge / Firefox to use the app.

Check you have it:

```bash
docker compose version
```

---

## Run it locally

From the repository root:

```bash
cp .env.example .env       # optional — the built-in defaults already work for local use
docker compose up --build
```

First build takes a few minutes (it downloads Maven + npm dependencies inside the containers).
When it's ready, open **http://localhost:4200**.

### Services & URLs

| Service            | URL                                      | Notes                                        |
| ------------------ | ---------------------------------------- | -------------------------------------------- |
| **Frontend (app)** | http://localhost:4200                    | Angular SPA (nginx); proxies `/api` to backend |
| Backend API        | http://localhost:8080/api                | Spring Boot REST API                         |
| Health check       | http://localhost:8080/actuator/health    | readiness/liveness                           |
| **Mailpit inbox**  | http://localhost:8025                     | catches every outgoing email (dev SMTP sink) |
| pgAdmin            | http://localhost:5050                     | DB browser — login `admin@dataart.com` / `admin` |
| PostgreSQL         | `localhost:5432`                          | db/user/password default to `ticketing`      |

> The database starts **empty** (schema + migration metadata only). You create all data
> through the app.

---

## First-run walkthrough

Because there's no seed data, here's the full happy path for a brand-new user:

1. **Sign up** — open http://localhost:4200, click **Create an account →**, enter an email and a
   password (min 8 chars).
2. **Get the verification email** — the app sends a verification link over SMTP. Locally that
   goes to **Mailpit, not a real inbox**: open **http://localhost:8025**, open the newest message,
   and click the **verify link** (or copy it into the browser). Unverified accounts can't log in.
3. **Log in** — back on the app, sign in with the account you just verified.
4. **Create a team** → **Teams** tab → **+ Create team**.
5. **(Optional) create epics** → **Epics** tab → pick the team → **+ Create epic**.
6. **Create tickets** → **Board** tab → **+ New ticket** (choose type, optional epic, title, body).
7. **Use the board** — drag cards between the five columns (state is saved immediately), filter by
   type/epic, search by title, open a ticket to edit it, add/edit/delete comments, and see the
   activity history.
8. **Log out** via the menu in the top-right (your email).

> Forgot your password? Use **Forgot password?** on the login screen — the reset link also lands
> in Mailpit.

---

## Configuration

All configuration is via environment variables — see [`.env.example`](.env.example). The defaults
work out of the box for local use; copy it to `.env` only to change something. `.env` is
git-ignored and must never be committed.

| Area | Variables |
| --- | --- |
| Database | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_PORT` |
| Auth / JWT | `JWT_SECRET` (see below), `JWT_TTL_SECONDS` |
| Email (SMTP) | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `MAIL_FROM` |
| Links | `APP_BASE_URL` — base URL used in verification / reset emails |
| Ports | `FRONTEND_PORT` (4200), `BACKEND_PORT` (8080), `MAILPIT_UI_PORT` (8025), `PGADMIN_PORT` (5050) |

- **Email** defaults to the bundled **Mailpit** container so the flow works with zero setup. To
  use a real relay (e.g. `relay1.dataart.com`), set `SMTP_HOST`/`SMTP_PORT` (and
  `SMTP_AUTH`/`SMTP_STARTTLS`/credentials as needed) in `.env`.
- **`JWT_SECRET`** — Compose supplies a dev-only value so local runs work immediately. **For any
  real deployment, set your own random secret of at least 32 bytes** — the backend refuses to
  start without one.

---

## Stopping & resetting

```bash
docker compose down        # stop and remove containers (database volume kept)
docker compose down -v      # also wipe the database → next start is empty again
```

---

## Project layout

```
.
├── docker-compose.yml        # db · backend · frontend · mailpit · pgadmin
├── .env.example
├── backend/                  # Java 21 · Spring Boot 3.3 · Spring Security · Flyway · PostgreSQL
│   ├── Dockerfile            # multi-stage Maven build → JRE image
│   └── src/main/java/com/dataart/ticketing/
│       ├── auth/  ticket/  team/  epic/  comment/  activity/   # feature packages
│       ├── domain/  repository/                                # JPA entities + repos
│       ├── config/  mail/  web/                                # security, SMTP, error handling
│       └── resources/db/migration/                             # Flyway V1–V4
└── frontend/                 # Angular 18 SPA
    ├── Dockerfile            # multi-stage: ng build → nginx
    ├── nginx.conf            # serves the SPA, proxies /api, sets security headers (CSP, etc.)
    └── src/app/
        ├── core/             # api/auth services, guard, interceptor, models, formatters
        ├── layouts/          # auth shell + app shell (header)
        └── pages/            # login, signup, verify, forgot/reset password, board, teams, epics, ticket editor
```

---

## Architecture & behavior notes

- **Three tiers, cleanly separated:** the nginx frontend serves the compiled SPA and reverse-proxies
  `/api` to the backend; the backend owns business logic; PostgreSQL owns persistence.
- **Auth:** bearer JWT in the `Authorization` header (never in a URL); passwords hashed with
  Argon2id; verification/reset tokens are single-use and expire (24h / 1h).
- **Data integrity is enforced in the DB:** case-insensitive unique email/team name, a ticket's epic
  must belong to the ticket's team (composite FK), `RESTRICT` on referenced team/epic deletes
  (→ HTTP 409), ticket delete cascades its comments, and `CHECK` constraints on ticket type/state.
- **Timestamps** are UTC ISO-8601; a ticket's `modified_at` advances only on a real field/state
  change (not on adding a comment or a no-op save).
- The schema is created by **Flyway migrations** (`ddl-auto: validate`); a fresh database contains
  no application data.

---

## Running the tests

- **Backend** — `cd backend && mvn test` runs JUnit + integration tests against a real PostgreSQL
  spun up by **Testcontainers** (needs a running Docker daemon and Maven). Covers the auth flow,
  teams/epics/tickets/comments, activity, and the business rules.
- **Frontend** — `cd frontend && npm install && npm test` runs the Karma/Jasmine specs (needs Node
  and a Chrome binary; set `CHROME_BIN` if it isn't auto-detected).

> On Docker Desktop for macOS, Testcontainers can't reach the daemon's API directly; run the suite
> in CI or a standard Docker/Linux host, where `mvn test` works as-is.

---

## Troubleshooting

- **A port is already in use** — set `FRONTEND_PORT` / `BACKEND_PORT` / `DB_PORT` / `MAILPIT_UI_PORT`
  / `PGADMIN_PORT` in `.env` and restart.
- **No verification email** — locally, emails don't go to a real inbox; check **Mailpit** at
  http://localhost:8025.
- **Backend won't start / "JWT signing secret is missing"** — set `JWT_SECRET` (≥ 32 bytes) in
  `.env`, or use the provided defaults via `docker compose up`.
- **Changes not showing after editing code** — rebuild: `docker compose up -d --build backend frontend`.

## Out of scope

Scrum/sprints, SSO/OAuth, fine-grained roles or team membership, file attachments, notifications,
and real-time multi-user updates are intentionally not implemented.
```
