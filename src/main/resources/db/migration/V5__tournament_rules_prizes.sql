-- ============================================================================
-- V5: Tournament rules, scoreboard, and prize fields
-- ============================================================================

-- Add rules column to tournaments (stored as JSON array)
ALTER TABLE tournaments 
ADD COLUMN IF NOT EXISTS rules TEXT;

-- Add prize fields to tournaments
ALTER TABLE tournaments 
ADD COLUMN IF NOT EXISTS per_kill_reward INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS first_prize INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS second_prize INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS third_prize INTEGER DEFAULT 0;

-- Create global_rules table for admin-managed global rules
CREATE TABLE IF NOT EXISTS global_rules (
    id              SERIAL PRIMARY KEY,
    rule_text       TEXT NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_global_rules_active_order ON global_rules (is_active, display_order);

-- Create app_config table for app version, filters, etc.
CREATE TABLE IF NOT EXISTS app_config (
    id              SERIAL PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    TEXT,
    config_type     VARCHAR(50) NOT NULL DEFAULT 'STRING',
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_by      VARCHAR(128)
);

-- Insert default app version config
INSERT INTO app_config (config_key, config_value, config_type) 
VALUES ('app_version_min_supported', '1.1.0', 'STRING'),
       ('app_version_latest', '1.3.2', 'STRING'),
       ('app_version_play_store_url', 'https://play.google.com/store/apps/details?id=com.esport.tournament', 'STRING')
ON CONFLICT (config_key) DO NOTHING;

-- Insert default filters config (stored as JSON)
INSERT INTO app_config (config_key, config_value, config_type) 
VALUES ('filters', '{"games":["Free Fire","PUBG","COD Mobile","BGMI","Clash Royale"],"teamSizes":["Solo","Duo","Squad","Hexa"],"maps":["Bermuda","Purgatory","Kalahari","Alpine","NeXTerra"],"timeSlots":["6:00-6:30 PM","7:00-8:00 PM","8:00-9:00 PM","9:00-10:00 PM"]}', 'JSON')
ON CONFLICT (config_key) DO NOTHING;

