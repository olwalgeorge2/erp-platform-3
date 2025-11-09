-- EDA hardening for Tenancy-Identity
-- 1) Ensure outbox events carry an event version (matches JPA entity)
-- 2) Create idempotency store for consumed events

-- 1) Outbox: add version column if missing
ALTER TABLE identity_outbox_events
    ADD COLUMN IF NOT EXISTS version INT;

-- Backfill and enforce NOT NULL with a safe default
UPDATE identity_outbox_events SET version = 1 WHERE version IS NULL;

ALTER TABLE identity_outbox_events
    ALTER COLUMN version SET NOT NULL;

-- Optional: keep a default for future inserts created outside the app
ALTER TABLE identity_outbox_events
    ALTER COLUMN version SET DEFAULT 1;

-- 2) Idempotency store for consumed events
CREATE TABLE IF NOT EXISTS identity_processed_events (
    id UUID PRIMARY KEY,
    fingerprint VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_identity_processed_events_fingerprint UNIQUE (fingerprint)
);

-- Helpful index for operational housekeeping and observability
CREATE INDEX IF NOT EXISTS idx_identity_processed_events_processed_at
    ON identity_processed_events (processed_at DESC);

