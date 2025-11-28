-- ============================================================================
-- V7: Create tournament_results table
-- ============================================================================

CREATE TABLE IF NOT EXISTS tournament_results (
    id                  BIGSERIAL PRIMARY KEY,
    tournament_id       INTEGER NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    firebase_user_uid   VARCHAR(128) NOT NULL,
    player_name         VARCHAR(255),
    team_name           VARCHAR(255),
    kills               INTEGER NOT NULL DEFAULT 0,
    placement           INTEGER NOT NULL DEFAULT 0,
    coins_earned        INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tournament_results_tournament_id 
    ON tournament_results (tournament_id);

CREATE INDEX IF NOT EXISTS idx_tournament_results_user_uid 
    ON tournament_results (firebase_user_uid);
