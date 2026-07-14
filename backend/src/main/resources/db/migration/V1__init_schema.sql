-- Kanban ticketing system — initial schema.
-- A fresh database contains schema + Flyway metadata only. No seed/application data.

-- gen_random_uuid() is available in PostgreSQL 13+ core.

-- ---------------------------------------------------------------------------
-- Users & email verification
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          TEXT        NOT NULL,
    password_hash  TEXT        NOT NULL,
    email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Email uniqueness is case-insensitive (emails are trimmed before insert by the app).
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE email_verification_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      TEXT        NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_evt_user ON email_verification_tokens (user_id);

-- ---------------------------------------------------------------------------
-- Teams
-- ---------------------------------------------------------------------------
CREATE TABLE teams (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Team names non-empty (enforced in app after trim) and unique case-insensitively.
CREATE UNIQUE INDEX ux_teams_name_lower ON teams (lower(name));

-- ---------------------------------------------------------------------------
-- Epics
-- ---------------------------------------------------------------------------
CREATE TABLE epics (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID        NOT NULL REFERENCES teams (id) ON DELETE RESTRICT,
    title       TEXT        NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Composite target so a ticket's (epic_id, team_id) can be FK-checked below,
    -- enforcing "epic belongs to the same team as the ticket" at the DB level.
    UNIQUE (id, team_id)
);
CREATE INDEX ix_epics_team ON epics (team_id);

-- ---------------------------------------------------------------------------
-- Tickets
-- ---------------------------------------------------------------------------
CREATE TABLE tickets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID        NOT NULL REFERENCES teams (id) ON DELETE RESTRICT,
    epic_id     UUID,
    type        TEXT        NOT NULL CHECK (type IN ('bug', 'feature', 'fix')),
    state       TEXT        NOT NULL DEFAULT 'new'
                    CHECK (state IN ('new', 'ready_for_implementation', 'in_progress',
                                     'ready_for_acceptance', 'done')),
    title       TEXT        NOT NULL,
    body        TEXT        NOT NULL,
    created_by  UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Epic must belong to the ticket's team. With MATCH SIMPLE, a NULL epic_id
    -- skips this check, so "no epic" is allowed. Deleting a referenced epic is
    -- blocked (RESTRICT) -> mapped to HTTP 409 by the app.
    CONSTRAINT fk_ticket_epic_same_team
        FOREIGN KEY (epic_id, team_id) REFERENCES epics (id, team_id) ON DELETE RESTRICT
);
CREATE INDEX ix_tickets_team ON tickets (team_id);
CREATE INDEX ix_tickets_epic ON tickets (epic_id);
-- Board reads a team's cards most-recently-modified first.
CREATE INDEX ix_tickets_team_modified ON tickets (team_id, modified_at DESC);

-- ---------------------------------------------------------------------------
-- Comments
-- ---------------------------------------------------------------------------
CREATE TABLE comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id  UUID        NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Comments listed chronologically, oldest first.
CREATE INDEX ix_comments_ticket_created ON comments (ticket_id, created_at ASC);
