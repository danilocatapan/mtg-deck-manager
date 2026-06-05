DO $migration_assertions$
BEGIN
    IF EXISTS (SELECT 1 FROM decks WHERE name = 'Legacy Manual Projection') THEN
        RAISE EXCEPTION 'V13 did not remove the linked legacy manual projection';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM decks WHERE name = 'Curated External Deck') THEN
        RAISE EXCEPTION 'V13 removed a curated external deck';
    END IF;

    IF to_regclass('public.meta_top_decks') IS NOT NULL
        OR to_regclass('public.meta_top_deck_cards') IS NOT NULL
        OR to_regclass('public.meta_top_deck_import_batches') IS NOT NULL THEN
        RAISE EXCEPTION 'V13 did not remove every legacy manual-import table';
    END IF;

    IF to_regclass('public.meta_decks') IS NULL
        OR to_regclass('public.meta_deck_cards') IS NULL THEN
        RAISE EXCEPTION 'V13 did not create the canonical meta snapshot tables';
    END IF;

    IF to_regclass('public.recommendation_benchmark_runs') IS NULL
        OR to_regclass('public.recommendation_benchmark_case_results') IS NULL
        OR to_regclass('public.recommendation_benchmark_reviews') IS NULL THEN
        RAISE EXCEPTION 'V14 did not create every recommendation benchmark table';
    END IF;
END
$migration_assertions$;
