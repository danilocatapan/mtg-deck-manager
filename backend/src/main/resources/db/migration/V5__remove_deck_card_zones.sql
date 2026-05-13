DROP INDEX IF EXISTS ux_deck_cards_deck_id_name_zone_ci;

WITH merged AS (
    SELECT MIN(id) AS keep_id, deck_id, lower(name) AS normalized_name, SUM(quantity) AS total_quantity
      FROM deck_cards
     WHERE deck_id IS NOT NULL
     GROUP BY deck_id, lower(name)
    HAVING COUNT(*) > 1
),
updated AS (
    UPDATE deck_cards card
       SET quantity = LEAST(99, merged.total_quantity)
      FROM merged
     WHERE card.id = merged.keep_id
 RETURNING card.id
)
DELETE FROM deck_cards card
 USING merged
 WHERE card.deck_id = merged.deck_id
   AND lower(card.name) = merged.normalized_name
   AND card.id <> merged.keep_id;

CREATE UNIQUE INDEX IF NOT EXISTS ux_deck_cards_deck_id_name_ci
    ON deck_cards (deck_id, lower(name))
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

    SELECT count(*), COALESCE(sum(quantity), 0)
      INTO entry_count, total_quantity
      FROM deck_cards
     WHERE deck_id = affected_deck_id;

    IF entry_count > 120 THEN
        RAISE EXCEPTION 'Deck % has % card entries; maximum is 120.', affected_deck_id, entry_count
            USING ERRCODE = '23514',
                  CONSTRAINT = 'ck_deck_cards_entries_per_deck';
    END IF;

    IF total_quantity > 99 THEN
        RAISE EXCEPTION 'Deck % has % cards; maximum is 99.', affected_deck_id, total_quantity
            USING ERRCODE = '23514',
                  CONSTRAINT = 'ck_deck_cards_total_quantity_per_deck';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

ALTER TABLE deck_cards
    DROP COLUMN IF EXISTS zone;
