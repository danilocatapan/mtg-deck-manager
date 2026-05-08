ALTER TABLE deck_cards
    ADD COLUMN IF NOT EXISTS zone VARCHAR(40) DEFAULT 'main';

UPDATE deck_cards
SET zone = 'main'
WHERE zone IS NULL OR zone = '';
