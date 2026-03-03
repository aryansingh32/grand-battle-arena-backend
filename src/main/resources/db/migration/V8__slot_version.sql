-- V8: Add version column for JPA optimistic locking on slots
-- This is defense-in-depth against concurrent modifications
ALTER TABLE slots ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
