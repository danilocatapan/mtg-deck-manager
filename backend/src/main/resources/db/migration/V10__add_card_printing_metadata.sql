ALTER TABLE deck_cards
    ADD COLUMN IF NOT EXISTS scryfall_id VARCHAR(80);

ALTER TABLE deck_cards
    ADD COLUMN IF NOT EXISTS set_code VARCHAR(16);

ALTER TABLE deck_cards
    ADD COLUMN IF NOT EXISTS set_name VARCHAR(160);

ALTER TABLE deck_cards
    ADD COLUMN IF NOT EXISTS collector_number VARCHAR(32);

ALTER TABLE deck_cards
    ADD COLUMN IF NOT EXISTS finish VARCHAR(16);

ALTER TABLE deck_cards
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000);

ALTER TABLE meta_top_deck_cards
    ADD COLUMN IF NOT EXISTS set_code VARCHAR(16);

ALTER TABLE meta_top_deck_cards
    ADD COLUMN IF NOT EXISTS set_name VARCHAR(160);

ALTER TABLE meta_top_deck_cards
    ADD COLUMN IF NOT EXISTS collector_number VARCHAR(32);

ALTER TABLE meta_top_deck_cards
    ADD COLUMN IF NOT EXISTS finish VARCHAR(16);

ALTER TABLE meta_top_deck_cards
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000);
