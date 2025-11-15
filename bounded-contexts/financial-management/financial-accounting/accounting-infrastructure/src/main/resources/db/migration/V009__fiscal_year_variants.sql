-- Stage 1: fiscal year variant catalog + blackout enforcement hooks

CREATE TABLE IF NOT EXISTS financial_accounting.fiscal_year_variants (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_month SMALLINT NOT NULL CHECK (start_month BETWEEN 1 AND 12),
    calendar_pattern VARCHAR(32) NOT NULL DEFAULT 'CALENDAR', -- e.g., CALENDAR, 4-4-5, 4-5-4
    period_structure JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_fiscal_year_variants_tenant_code
    ON financial_accounting.fiscal_year_variants (tenant_id, code);

CREATE TABLE IF NOT EXISTS financial_accounting.fiscal_year_variant_periods (
    id BIGSERIAL PRIMARY KEY,
    variant_id UUID NOT NULL REFERENCES financial_accounting.fiscal_year_variants (id) ON DELETE CASCADE,
    period_number SMALLINT NOT NULL,
    label VARCHAR(64) NOT NULL,
    length_in_days SMALLINT NOT NULL,
    is_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (period_number > 0),
    CHECK (length_in_days > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_fiscal_variant_periods
    ON financial_accounting.fiscal_year_variant_periods (variant_id, period_number);

CREATE TABLE IF NOT EXISTS financial_accounting.company_code_fiscal_year_variants (
    company_code_id UUID NOT NULL REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE,
    fiscal_year_variant_id UUID NOT NULL REFERENCES financial_accounting.fiscal_year_variants (id) ON DELETE CASCADE,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_code_id, fiscal_year_variant_id, effective_from),
    CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

-- Blackout rules define when periods are locked or frozen for a company code
CREATE TABLE IF NOT EXISTS financial_accounting.period_blackouts (
    id UUID PRIMARY KEY,
    company_code_id UUID NOT NULL REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE,
    period_code VARCHAR(32) NOT NULL,
    blackout_start TIMESTAMPTZ NOT NULL,
    blackout_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (blackout_end >= blackout_start)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_period_blackouts
    ON financial_accounting.period_blackouts (company_code_id, period_code, blackout_start);
