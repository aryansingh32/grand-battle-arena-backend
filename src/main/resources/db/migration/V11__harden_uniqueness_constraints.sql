-- Harden data integrity for production workloads

-- Ensure transaction idempotency key is unique when present
CREATE UNIQUE INDEX IF NOT EXISTS uq_transaction_uid_not_null
    ON transaction_table (transaction_uid)
    WHERE transaction_uid IS NOT NULL;

-- Enforce username uniqueness (case-insensitive) when provided
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower_not_null
    ON users (LOWER(user_name))
    WHERE user_name IS NOT NULL;
