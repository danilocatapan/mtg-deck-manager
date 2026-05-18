ALTER TABLE decks
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(16) NOT NULL DEFAULT 'private';

ALTER TABLE decks
    ADD COLUMN IF NOT EXISTS author_display_name VARCHAR(255);

ALTER TABLE decks
    DROP CONSTRAINT IF EXISTS ck_decks_visibility_value;

ALTER TABLE decks
    ADD CONSTRAINT ck_decks_visibility_value CHECK (visibility IN ('private', 'public'));

CREATE INDEX IF NOT EXISTS ix_decks_visibility_id
    ON decks (visibility, id DESC);

CREATE INDEX IF NOT EXISTS ix_decks_visibility_commander_ci
    ON decks (visibility, lower(commander));
