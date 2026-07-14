-- Comment editing (stretch feature). edited_at is NULL until the author edits the
-- comment, then records the last edit time. Deleting a comment is a plain DELETE.
ALTER TABLE comments
    ADD COLUMN edited_at TIMESTAMPTZ;
