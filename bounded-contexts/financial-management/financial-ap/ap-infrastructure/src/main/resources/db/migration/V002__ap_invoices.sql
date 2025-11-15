CREATE TABLE IF NOT EXISTS financial_ap.ap_invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL REFERENCES financial_accounting.company_codes (id) ON DELETE RESTRICT,
    vendor_id UUID NOT NULL REFERENCES financial_ap.vendors (id) ON DELETE RESTRICT,
    invoice_number VARCHAR(64) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    net_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    cost_center_id UUID REFERENCES financial_accounting.cost_centers (id),
    profit_center_id UUID REFERENCES financial_accounting.profit_centers (id),
    department_id UUID REFERENCES financial_accounting.departments (id),
    project_id UUID REFERENCES financial_accounting.projects (id),
    business_area_id UUID REFERENCES financial_accounting.business_areas (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    posted_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_ap_invoices_tenant_vendor
    ON financial_ap.ap_invoices (tenant_id, vendor_id);
CREATE INDEX IF NOT EXISTS ix_ap_invoices_status
    ON financial_ap.ap_invoices (status);

CREATE TABLE IF NOT EXISTS financial_ap.ap_invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES financial_ap.ap_invoices (id) ON DELETE CASCADE,
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

CREATE TABLE IF NOT EXISTS financial_ap.ap_open_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES financial_ap.ap_invoices (id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    amount_outstanding BIGINT NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_ap_open_items_status
    ON financial_ap.ap_open_items (status);
