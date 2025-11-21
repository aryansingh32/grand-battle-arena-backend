-- ============================================================================
-- V0: Core domain schema
-- Creates all legacy tables required before RBAC, Ledger and other add-ons.
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id                  SERIAL PRIMARY KEY,
    firebase_useruid    VARCHAR(128) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    user_name           VARCHAR(255),
    role                VARCHAR(20) NOT NULL DEFAULT 'USER',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    device_token        VARCHAR(500),
    device_token_updated_at TIMESTAMPTZ,
    last_active_at      TIMESTAMPTZ,
    CHECK (role IN ('USER','ADMIN')),
    CHECK (status IN ('ACTIVE','INACTIVE','BANNED'))
);

CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users (firebase_useruid);

-- ----------------------------------------------------------------------------
-- Tournaments
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tournaments (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    map_type        VARCHAR(100),
    start_time      TIMESTAMPTZ,
    entry_fees      INTEGER NOT NULL DEFAULT 0,
    prize_pool      INTEGER NOT NULL DEFAULT 0,
    team_size       VARCHAR(20) NOT NULL DEFAULT 'SOLO',
    max_players     INTEGER NOT NULL DEFAULT 0,
    game            VARCHAR(100),
    image_link      VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    updated_at      TIMESTAMPTZ,
    game_id         VARCHAR(100),
    game_password   VARCHAR(100),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    CHECK (team_size IN ('SOLO','DUO','SQUAD','HEXA')),
    CHECK (status IN ('UPCOMING','ONGOING','COMPLETED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_tournaments_status_start
    ON tournaments (status, start_time DESC);

-- ----------------------------------------------------------------------------
-- Slots
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS slots (
    id                  SERIAL PRIMARY KEY,
    tournament_id       INTEGER NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    slot_number         INTEGER NOT NULL,
    firebase_useruid    VARCHAR(128) REFERENCES users(firebase_useruid) ON DELETE SET NULL,
    player_name         VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    booked_at           TIMESTAMPTZ,
    CHECK (status IN ('AVAILABLE','BOOKED')),
    UNIQUE (tournament_id, slot_number)
);

CREATE INDEX IF NOT EXISTS idx_slots_tournament_status
    ON slots (tournament_id, status);

-- ----------------------------------------------------------------------------
-- Wallet
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    coins           INTEGER NOT NULL DEFAULT 0,
    last_updated    TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Transaction history (deposits & withdrawals)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transaction_table (
    id                  SERIAL PRIMARY KEY,
    user_id             INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transaction_uid     VARCHAR(255),
    amount              INTEGER NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_by         VARCHAR(255),
    type                VARCHAR(20) NOT NULL DEFAULT 'WITHDRAWAL',
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    verified_at         TIMESTAMPTZ,
    CHECK (status IN ('PENDING','COMPLETED','REJECTED')),
    CHECK (type IN ('WITHDRAWAL','DEPOSIT'))
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_status
    ON transaction_table (user_id, status, created_at DESC);

-- ----------------------------------------------------------------------------
-- Payments metadata (QR / UPI mappings)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    id              SERIAL PRIMARY KEY,
    amount          INTEGER NOT NULL UNIQUE,
    coins           INTEGER,
    upi_id_qr_link  VARCHAR(500) NOT NULL,
    added_by        VARCHAR(255) NOT NULL,
    modified_by     VARCHAR(255),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- Notifications
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id                      SERIAL PRIMARY KEY,
    title                   VARCHAR(255),
    message                 VARCHAR(500),
    target_audience         VARCHAR(20) NOT NULL DEFAULT 'ALL',
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),
    created_by              VARCHAR(128),
    device_token            VARCHAR(500),
    device_token_updated_at TIMESTAMPTZ,
    CHECK (target_audience IN ('ALL','ADMIN','USER','REGISTERED'))
);

-- ----------------------------------------------------------------------------
-- Audit logs
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    category    VARCHAR(50) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    user_id     VARCHAR(128) NOT NULL,
    details     TEXT,
    timestamp   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_audit_category_timestamp
    ON audit_logs (category, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_audit_user_timestamp
    ON audit_logs (user_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_audit_action_timestamp
    ON audit_logs (action, timestamp DESC);

