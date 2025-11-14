-- Baseline schema for financial accounting context
CREATE SCHEMA IF NOT EXISTS financial_accounting;

CREATE TABLE IF NOT EXISTS financial_accounting.ledgers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    chart_of_accounts_id UUID NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_ledgers_tenant_chart
    ON financial_accounting.ledgers (tenant_id, chart_of_accounts_id);

CREATE TABLE IF NOT EXISTS financial_accounting.chart_of_accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_coa_tenant_code
    ON financial_accounting.chart_of_accounts (tenant_id, code);

CREATE TABLE IF NOT EXISTS financial_accounting.accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    chart_of_accounts_id UUID NOT NULL REFERENCES financial_accounting.chart_of_accounts (id),
    parent_account_id UUID NULL REFERENCES financial_accounting.accounts (id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    is_posting BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_tenant_code
    ON financial_accounting.accounts (tenant_id, code);
CREATE INDEX IF NOT EXISTS ix_accounts_chart
    ON financial_accounting.accounts (chart_of_accounts_id);

CREATE TABLE IF NOT EXISTS financial_accounting.accounting_periods (
    id UUID PRIMARY KEY,
    ledger_id UUID NOT NULL REFERENCES financial_accounting.ledgers (id),
    tenant_id UUID NOT NULL,
    period_code VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (end_date >= start_date)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_periods_ledger_code
    ON financial_accounting.accounting_periods (ledger_id, period_code);
CREATE INDEX IF NOT EXISTS ix_periods_tenant_status
    ON financial_accounting.accounting_periods (tenant_id, status);

CREATE TABLE IF NOT EXISTS financial_accounting.journal_entries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    ledger_id UUID NOT NULL REFERENCES financial_accounting.ledgers (id),
    accounting_period_id UUID NOT NULL REFERENCES financial_accounting.accounting_periods (id),
    posting_batch_id UUID NULL,
    reference VARCHAR(128),
    description TEXT,
    booked_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    total_debits BIGINT NOT NULL DEFAULT 0,
    total_credits BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_journal_entries_ledger_period
    ON financial_accounting.journal_entries (ledger_id, accounting_period_id);
CREATE INDEX IF NOT EXISTS ix_journal_entries_tenant_status
    ON financial_accounting.journal_entries (tenant_id, status);

CREATE TABLE IF NOT EXISTS financial_accounting.journal_entry_lines (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES financial_accounting.journal_entries (id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES financial_accounting.accounts (id),
    tenant_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    debit_amount BIGINT NOT NULL DEFAULT 0,
    credit_amount BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK ((debit_amount = 0 AND credit_amount > 0) OR (credit_amount = 0 AND debit_amount > 0))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_journal_entry_lines_entry_line
    ON financial_accounting.journal_entry_lines (journal_entry_id, line_number);
CREATE INDEX IF NOT EXISTS ix_journal_entry_lines_account
    ON financial_accounting.journal_entry_lines (account_id);

-- Utility table for posting batches (optional; placeholder for future batching logic)
CREATE TABLE IF NOT EXISTS financial_accounting.posting_batches (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    ledger_id UUID NOT NULL REFERENCES financial_accounting.ledgers (id),
    reference VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ NULL,
    posted_at TIMESTAMPTZ NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_posting_batches_tenant_status
    ON financial_accounting.posting_batches (tenant_id, status);
