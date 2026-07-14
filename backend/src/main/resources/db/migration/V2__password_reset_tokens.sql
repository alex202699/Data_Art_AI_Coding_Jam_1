-- Password reset tokens. Mirrors email_verification_tokens; single-use, short-lived (1h).
-- Password reset is a stretch feature; the table is additive and unused until enabled.

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      TEXT        NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_prt_user ON password_reset_tokens (user_id);
