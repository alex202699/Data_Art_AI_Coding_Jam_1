# Ticket Tracker — Kanban Ticketing System

A three-tier Kanban-style ticket tracker: an **Angular** SPA, a **Java / Spring Boot**
HTTP API, and a **PostgreSQL** database. The whole solution starts from the repository
root with a single command.

## Prerequisites

- Docker + Docker Compose (nothing else — no host-installed Node, JDK, or Postgres).
- Works on Windows, macOS, and Linux.

## Quick start

```bash
cp .env.example .env      # adjust secrets/SMTP as needed
docker compose up --build
```

Then open:

| Tier      | URL                                  |
| --------- | ------------------------------------ |
| Frontend  | http://localhost:4200                |
| Backend   | http://localhost:8080/api            |
| Health    | http://localhost:8080/actuator/health |

The frontend container (nginx) serves the compiled SPA and proxies `/api` to the backend
container, keeping the presentation, application, and persistence tiers separate.

A fresh database starts with **schema + migration metadata only** — no seed or application
data. Create all test data through the UI or API.

## Configuration

All configuration is via environment variables (see [`.env.example`](.env.example)).
Secrets and SMTP credentials must **not** be committed — `.env` is git-ignored.

- **Database**: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- **Email verification (SMTP)**: `SMTP_HOST` (supports `relay1.dataart.com`), `SMTP_PORT`,
  `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`
- **Links**: `APP_BASE_URL` — base URL used in verification emails

## Project layout

```
.
├── docker-compose.yml        # db + backend + frontend
├── .env.example
├── backend/                  # Java 21 · Spring Boot · Flyway · PostgreSQL
│   ├── Dockerfile            # multi-stage Maven build -> JRE image
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/dataart/ticketing/
│       │   ├── TicketingApplication.java
│       │   └── config/SecurityConfig.java   # Argon2id, public/auth routes
│       └── resources/
│           ├── application.yml
│           └── db/migration/V1__init_schema.sql   # full data model
└── frontend/                 # Angular 18 SPA
    ├── Dockerfile            # multi-stage: ng build -> nginx
    ├── nginx.conf            # serves SPA, proxies /api -> backend
    └── src/app/
        ├── core/             # api.service, auth (service/guard/interceptor), models
        └── pages/            # login, verify, board, teams, epics
```

### Where the engineers pick up

The scaffold boots end-to-end (DB migrates, backend serves `/actuator/health`, SPA loads
and routes). The domain features are stubbed and marked with `TODO` comments:

- **Backend** (`com.dataart.ticketing`): add `domain` (JPA entities), `repository`,
  `service`, `web` (controllers + DTOs), and `auth` packages implementing teams, epics,
  tickets, comments, and the auth/verification flow against the schema in `V1__init_schema.sql`.
- **Frontend** (`src/app`): flesh out the page components using the typed
  [`ApiService`](frontend/src/app/core/api.service.ts) and the enums in
  [`models.ts`](frontend/src/app/core/models.ts).

## Data model

Defined in [`V1__init_schema.sql`](backend/src/main/resources/db/migration/V1__init_schema.sql):
`users`, `email_verification_tokens`, `teams`, `epics`, `tickets`, `comments`. Key
integrity is enforced in the database:

- Case-insensitive unique email and team name (`lower(...)` unique indexes).
- A ticket's epic must belong to the ticket's team (composite FK `(epic_id, team_id)`).
- Team / epic deletes are `RESTRICT` (referenced -> HTTP 409); ticket delete cascades to
  its comments.
- Ticket `type` and `state` constrained to the canonical enum values via `CHECK`.

## Testing

- **Backend**: `cd backend && mvn test` — boots the app against a real PostgreSQL via
  Testcontainers (requires a local Docker daemon) and verifies migrations + Argon2 hashing.
- **Frontend**: `cd frontend && npm test` — Karma/Jasmine specs for the app shell.

## Notes

- Timestamps are UTC ISO-8601; `modified_at` advances only on a real ticket field/state
  change (not on comment add or a no-op save).
- Session tokens/identifiers are never placed in URLs (only the single-use email
  verification token may appear in a verification link).
- Out of scope: Scrum/sprints, SSO/OAuth, roles/membership, attachments, notifications,
  real-time updates, custom workflows.
```
