-- Performance indexes for high-traffic read paths

CREATE INDEX IF NOT EXISTS idx_users_status_created
    ON users (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_users_role_status
    ON users (role, status);

CREATE INDEX IF NOT EXISTS idx_users_last_active
    ON users (last_active_at DESC);

CREATE INDEX IF NOT EXISTS idx_tournaments_game_status_start
    ON tournaments (game, status, start_time DESC);

CREATE INDEX IF NOT EXISTS idx_slots_user_status
    ON slots (firebase_useruid, status, booked_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_transaction_uid
    ON transaction_table (transaction_uid);

CREATE INDEX IF NOT EXISTS idx_transactions_created_at
    ON transaction_table (created_at DESC);
