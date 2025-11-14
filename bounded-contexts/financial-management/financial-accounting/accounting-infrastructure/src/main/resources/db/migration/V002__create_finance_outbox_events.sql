CREATE TABLE IF NOT EXISTS financial_accounting.finance_outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(256) NOT NULL,
    channel VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    occurred_at TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL,
    failure_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    last_attempt_at TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_finance_outbox_events_status_recorded
    ON financial_accounting.finance_outbox_events (status, recorded_at)
    WHERE status IN ('PENDING', 'FAILED');
