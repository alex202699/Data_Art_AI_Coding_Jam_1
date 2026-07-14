-- Ticket activity history (stretch feature). One row per recorded event on a ticket:
-- creation, field/state changes, and comment add/edit/delete. Human labels are
-- denormalized into old_value/new_value at event time so history stays readable even
-- after a referenced team/epic is renamed or deleted.
CREATE TABLE ticket_activity (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id  UUID NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    actor_id   UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    kind       TEXT NOT NULL CHECK (
        kind IN (
            'created',
            'field_changed',
            'state_changed',
            'comment_added',
            'comment_edited',
            'comment_deleted'
        )
    ),
    -- For field_changed: which field. For state_changed: 'state'. Null otherwise.
    field      TEXT,
    old_value  TEXT,
    new_value  TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_ticket_activity_ticket_created ON ticket_activity (ticket_id, created_at);
