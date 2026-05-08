ALTER TABLE decks
    ADD COLUMN IF NOT EXISTS history_json TEXT;

DROP INDEX IF EXISTS ux_deck_cards_deck_id_name_ci;

CREATE UNIQUE INDEX IF NOT EXISTS ux_deck_cards_deck_id_name_zone_ci
    ON deck_cards (deck_id, lower(name), zone)
    WHERE deck_id IS NOT NULL;

CREATE OR REPLACE FUNCTION fn_validate_deck_card_limits()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    affected_deck_id BIGINT;
    entry_count INTEGER;
    total_quantity INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        affected_deck_id := OLD.deck_id;
    ELSE
        affected_deck_id := NEW.deck_id;
    END IF;

    IF affected_deck_id IS NULL THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    SELECT count(*), COALESCE(sum(CASE WHEN zone = 'main' THEN quantity ELSE 0 END), 0)
      INTO entry_count, total_quantity
      FROM deck_cards
     WHERE deck_id = affected_deck_id;

    IF entry_count > 120 THEN
        RAISE EXCEPTION 'Deck % has % card entries; maximum is 120.', affected_deck_id, entry_count
            USING ERRCODE = '23514',
                  CONSTRAINT = 'ck_deck_cards_entries_per_deck';
    END IF;

    IF total_quantity > 99 THEN
        RAISE EXCEPTION 'Deck % has % main deck cards; maximum main-deck size is 99.', affected_deck_id, total_quantity
            USING ERRCODE = '23514',
                  CONSTRAINT = 'ck_deck_cards_total_quantity_per_deck';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
