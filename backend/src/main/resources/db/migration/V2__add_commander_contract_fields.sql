ALTER TABLE decks
    ADD COLUMN IF NOT EXISTS commanders_json TEXT;
