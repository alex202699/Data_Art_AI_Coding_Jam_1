# CLAUDE.md — Kanban Ticketing System

Guidance for working in this repo. Keep this file high-signal; it loads every session.

## What this is
A hackathon three-tier Kanban ticket tracker. Full spec: `.claude/requirements/requirements.pdf`
(treat it as the source of truth). Stack:
- **Frontend** — Angular 18 SPA (standalone components, signals), served by nginx.
- **Backend** — Java 21 / Spring Boot 3.3 HTTP API.
- **DB** — PostgreSQL 16, schema owned by Flyway migrations.

## Running & tooling
- Start everything from the repo root: `docker compose up --build`.
- **Toolchain on the host:** Node + npm are installed (Angular 18-compatible); **Maven is NOT** — build the backend through Docker, don't assume `mvn`. The app is designed to build/run entirely via Docker regardless.
  - Backend build: happens in `backend/Dockerfile` (Maven image).
  - Frontend build: `frontend/Dockerfile` (Node image → nginx).
  - Frontend deps are installed locally in `frontend/node_modules` (gitignored) with `package-lock.json` committed — for IDE IntelliSense and local `ng serve` / `npm test`. Run `npm install` in `frontend/` if the folder is missing.
  - Rebuild one service: `docker compose up -d --build <backend|frontend>`.
- Ports: frontend http://localhost:4200 · backend http://localhost:8080/api · Mailpit inbox http://localhost:8025 · pgAdmin http://localhost:5050 · Postgres 5432.
- **Email flow (dev):** SMTP defaults to the `mailpit` container; verification/reset links land in the Mailpit inbox. Config is env-driven and overridable to `relay1.dataart.com`.
- Copy `.env.example` → `.env` before running (defaults work for local).

## Verifying changes
- The app is the source of truth — drive real flows, don't just typecheck.
- Backend API: `curl` against `http://localhost:8080/api/...`; auth emails via the Mailpit API (`http://localhost:8025/api/v1/...`).
- Frontend renders client-side, so `curl` only proves nginx served HTML. To confirm a screen actually renders, screenshot with headless Chrome:
  `docker run --rm -v /tmp/shots:/shots zenika/alpine-chrome --no-sandbox --headless --screenshot=/shots/x.png --window-size=1200,900 --virtual-time-budget=10000 http://host.docker.internal:4200/<route>` then Read the PNG. (`--dump-dom` snapshots before lazy route chunks load — prefer screenshots.)
- Automated tests exist but need tooling not on the host: backend `mvn test` (Testcontainers), frontend `npm test` (Karma). Run them inside a container if needed.

## Architecture & where things live
### Backend (`backend/src/main/java/com/dataart/ticketing/`)
- `domain/` JPA entities · `repository/` Spring Data repos · `auth/` (AuthController/Service, JwtService, JwtAuthFilter, TokenService, dtos) · `mail/` · `web/` (GlobalExceptionHandler) · `config/` (SecurityConfig, AppProperties).
- Schema is **owned by Flyway** (`src/main/resources/db/migration/V*.sql`); Hibernate is `ddl-auto: validate`. To change the schema, add a new `V<n>__*.sql` migration — never edit an applied one, never rely on Hibernate to create tables.
- Config binds under `app.*` via `AppProperties` (base-url, mail.from, jwt.secret/ttl, verification/reset TTLs).

### Frontend (`frontend/src/app/`)
- `core/` — `models.ts` (enums + interfaces), `api.service.ts`, `auth-api.service.ts`, `auth.service.ts` (token+user signals), `auth.guard.ts`, `auth.interceptor.ts`.
- `layouts/` — `auth-shell` (centered card, public) and `app-shell` (header + user menu, guarded).
- `pages/` — auth screens (login/signup/verify/forgot-password/reset-password) and app screens (board/teams/epics; board/teams/epics are still stubs).
- Global styles: `frontend/src/styles.css`.

## Conventions & non-obvious rules
- **Auth:** bearer JWT (HS256, `sub`=userId). Token in the `Authorization` header (never a URL), stored in `sessionStorage`. Endpoints under `/api/auth/*`. Passwords hashed with Argon2id. The interceptor skips its 401/403 auto-redirect for `/api/auth/*` so pages handle those inline.
- **Domain rules (enforced server-side — client validation is never enough):** ticket `type` ∈ `bug|feature|fix`; `state` ∈ `new|ready_for_implementation|in_progress|ready_for_acceptance|done`. Deleting a team with tickets/epics, or an epic referenced by tickets → **409**. A ticket's epic must belong to the ticket's team. `modified_at` advances only on a real ticket field/state change (not on comment add or a no-op save). Timestamps are UTC ISO-8601. Verification/reset tokens are single-use; a new one invalidates prior unused tokens.
- **Frontend design:** the look is deliberately cloned from `~/Documents/kanban-board` (a Next.js reference app); `styles.css` is its `globals.css` adopted verbatim. Preserve visual parity. Gotcha: auth page component hosts use `display: contents` so `.auth-card` (a flex child of `.auth-shell`) reaches its 380px width — don't remove it.
- **Never** commit secrets — `.env` is gitignored; share config via `.env.example`. A `.claude/` hook blocks this.
- **Never** hand-edit generated output (`backend/target/`, `frontend/dist/`, `frontend/node_modules/`, `frontend/.angular/`) — a `.claude/` hook blocks it. Edit the source.
- A fresh DB must start empty (schema + migration metadata only); no seed data.

## Custom agents (`.claude/agents/`)
- `technical-solution-architect` (Opus) — design/architecture decisions.
- `backend-engineer` (Sonnet) — Java/Spring specialist.
- `frontend-engineer` (Sonnet) — Angular specialist.
