CREATE TABLE IF NOT EXISTS financial_accounting.exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(18,8) NOT NULL,
    as_of TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_exchange_rates_lookup
    ON financial_accounting.exchange_rates(base_currency, quote_currency, as_of DESC);
