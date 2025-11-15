CREATE TABLE IF NOT EXISTS financial_accounting.control_account_config (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    subledger VARCHAR(8) NOT NULL,
    category VARCHAR(16) NOT NULL,
    dimension_key VARCHAR(128) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    gl_account_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_control_account_config_key
    ON financial_accounting.control_account_config (
        tenant_id,
        company_code_id,
        subledger,
        category,
        dimension_key,
        currency
    );
