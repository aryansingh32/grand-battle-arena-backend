CREATE TABLE IF NOT EXISTS notification_reads (
    id SERIAL PRIMARY KEY,
    notification_id INTEGER NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    firebase_useruid VARCHAR(128) NOT NULL REFERENCES users(firebase_useruid) ON DELETE CASCADE,
    read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_read_user UNIQUE (notification_id, firebase_useruid)
);

CREATE INDEX IF NOT EXISTS idx_notification_reads_user_readat
    ON notification_reads (firebase_useruid, read_at DESC);
