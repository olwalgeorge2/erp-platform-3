CREATE TABLE IF NOT EXISTS financial_ar.ar_invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL REFERENCES financial_accounting.company_codes (id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES financial_ar.customers (id) ON DELETE RESTRICT,
    invoice_number VARCHAR(64) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    net_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    received_amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    cost_center_id UUID REFERENCES financial_accounting.cost_centers (id),
    profit_center_id UUID REFERENCES financial_accounting.profit_centers (id),
    department_id UUID REFERENCES financial_accounting.departments (id),
    project_id UUID REFERENCES financial_accounting.projects (id),
    business_area_id UUID REFERENCES financial_accounting.business_areas (id),
    payment_terms_code VARCHAR(32) NOT NULL,
    payment_terms_type VARCHAR(32) NOT NULL,
    payment_terms_description VARCHAR(255),
    payment_terms_due_days INTEGER NOT NULL,
    payment_terms_discount_percentage NUMERIC(9, 4),
    payment_terms_discount_days INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    posted_at TIMESTAMPTZ,
    journal_entry_id UUID,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS financial_ar.ar_invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES financial_ar.ar_invoices (id) ON DELETE CASCADE,
    gl_account_id UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    net_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    cost_center_id UUID REFERENCES financial_accounting.cost_centers (id),
    profit_center_id UUID REFERENCES financial_accounting.profit_centers (id),
    department_id UUID REFERENCES financial_accounting.departments (id),
    project_id UUID REFERENCES financial_accounting.projects (id),
    business_area_id UUID REFERENCES financial_accounting.business_areas (id)
);

CREATE TABLE IF NOT EXISTS financial_ar.ar_open_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES financial_ar.ar_invoices (id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    invoice_number VARCHAR(64) NOT NULL,
    invoice_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    original_amount_minor BIGINT NOT NULL,
    cleared_amount_minor BIGINT NOT NULL DEFAULT 0,
    amount_outstanding BIGINT NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    journal_entry_id UUID,
    payment_terms_code VARCHAR(32) NOT NULL,
    payment_terms_type VARCHAR(32) NOT NULL,
    payment_terms_due_days INTEGER NOT NULL,
    payment_terms_discount_percentage NUMERIC(9, 4),
    payment_terms_discount_days INTEGER,
    cash_discount_due_date DATE,
    last_receipt_date DATE,
    last_dunning_sent_at TIMESTAMPTZ,
    dunning_level INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_ar_invoices_status
    ON financial_ar.ar_invoices (status);

CREATE INDEX IF NOT EXISTS ix_ar_open_items_status
    ON financial_ar.ar_open_items (status);
