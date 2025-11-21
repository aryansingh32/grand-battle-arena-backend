-- ============================================================================
-- V4: Banners table for dynamic banner management
-- ============================================================================

CREATE TABLE IF NOT EXISTS banners (
    id              SERIAL PRIMARY KEY,
    image_url       VARCHAR(500),
    title           VARCHAR(255),
    description     VARCHAR(1000),
    action_url      VARCHAR(500),
    type            VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    display_order   INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    start_date      TIMESTAMPTZ,
    end_date        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    CHECK (type IN ('IMAGE','VIDEO','AD'))
);

CREATE INDEX IF NOT EXISTS idx_banners_active_dates ON banners (is_active, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_banners_order ON banners (display_order, created_at DESC);

