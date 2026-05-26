CREATE TABLE meta_combos (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(64) NOT NULL,
    external_id VARCHAR(120) NOT NULL,
    name VARCHAR(320) NOT NULL,
    result_text VARCHAR(2000),
    tags VARCHAR(1000),
    legalities VARCHAR(1000),
    brackets VARCHAR(1000),
    commander_required VARCHAR(160),
    popularity INTEGER,
    source_url VARCHAR(1000),
    source_updated_at TIMESTAMP WITH TIME ZONE,
    synced_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_meta_combos_source_external_id UNIQUE (source, external_id)
);

CREATE TABLE meta_combo_cards (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL REFERENCES meta_combos(id) ON DELETE CASCADE,
    card_name VARCHAR(180) NOT NULL,
    card_normalized VARCHAR(180) NOT NULL,
    card_role VARCHAR(64),
    commander_slot BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_meta_combo_cards_normalized ON meta_combo_cards(card_normalized);
CREATE INDEX idx_meta_combos_popularity ON meta_combos(popularity);
