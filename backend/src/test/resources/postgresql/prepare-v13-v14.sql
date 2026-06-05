INSERT INTO decks (
    name,
    commander,
    owner_id,
    color_identity,
    visibility,
    source_type,
    external_source,
    imported_at
) VALUES
    (
        'Legacy Manual Projection',
        'Xenagos, God of Revels',
        'meta-import',
        'RG',
        'public',
        'meta_top_deck',
        'legacy-manual',
        CURRENT_TIMESTAMP
    ),
    (
        'Curated External Deck',
        'Atraxa, Praetors Voice',
        'external-curator',
        'WUBG',
        'public',
        'external_import',
        'curated-source',
        CURRENT_TIMESTAMP
    );

INSERT INTO meta_top_decks (
    source,
    name,
    format,
    commander,
    commander_normalized,
    deck_rank,
    ranking_period,
    ranking_date,
    archetype,
    bracket,
    color_identity,
    public_deck_id,
    created_at,
    updated_at
) SELECT
    'legacy-manual',
    'Legacy Manual Projection',
    'COMMANDER',
    'Xenagos, God of Revels',
    'xenagos god of revels',
    1,
    'monthly',
    CURRENT_DATE,
    'stompy',
    'high-power',
    'RG',
    id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM decks
WHERE name = 'Legacy Manual Projection';
