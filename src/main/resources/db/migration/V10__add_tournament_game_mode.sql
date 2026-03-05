-- V10: Add game mode support for tournaments
ALTER TABLE tournaments
    ADD COLUMN IF NOT EXISTS game_mode VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_tournaments_game_mode
    ON tournaments (game_mode);
