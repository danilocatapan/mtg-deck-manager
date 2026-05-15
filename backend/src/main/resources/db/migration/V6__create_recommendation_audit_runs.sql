CREATE TABLE IF NOT EXISTS recommendation_audit_runs (
    id BIGSERIAL PRIMARY KEY,
    deck_id BIGINT REFERENCES decks(id) ON DELETE SET NULL,
    owner_id VARCHAR(255),
    commander VARCHAR(255),
    color_identity VARCHAR(32),
    bracket VARCHAR(64),
    archetype VARCHAR(128),
    algorithm_version VARCHAR(64) NOT NULL DEFAULT 'strategic-v1',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    gaps_json TEXT,
    issues_json TEXT,
    weak_cards_json TEXT,
    params_json TEXT,
    recommendations_json TEXT,
    blocked_pairs_json TEXT,
    protected_cuts_json TEXT,
    feedback_status VARCHAR(32),
    feedback_reason VARCHAR(255),
    feedback_notes TEXT,
    feedback_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_recommendation_audit_runs_deck_created
    ON recommendation_audit_runs(deck_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_recommendation_audit_runs_owner_created
    ON recommendation_audit_runs(owner_id, created_at DESC);
