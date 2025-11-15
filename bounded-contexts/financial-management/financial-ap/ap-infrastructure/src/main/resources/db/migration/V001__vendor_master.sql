CREATE SCHEMA IF NOT EXISTS financial_ap;

CREATE TABLE IF NOT EXISTS financial_ap.vendors (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL REFERENCES financial_accounting.company_codes (id) ON DELETE RESTRICT,
    vendor_number VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    currency VARCHAR(3) NOT NULL,
    payment_term_code VARCHAR(32) NOT NULL,
    payment_term_type VARCHAR(32) NOT NULL,
    payment_term_due_days INTEGER NOT NULL CHECK (payment_term_due_days >= 0),
    payment_term_discount_percent NUMERIC(5, 2),
    payment_term_discount_days INTEGER CHECK (payment_term_discount_days IS NULL OR payment_term_discount_days >= 0),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(128) NOT NULL,
    state VARCHAR(128),
    postal_code VARCHAR(32),
    country_code CHAR(2) NOT NULL,
    contact_name VARCHAR(255),
    contact_email VARCHAR(320),
    contact_phone VARCHAR(64),
    bank_name VARCHAR(255),
    bank_account VARCHAR(64),
    bank_routing VARCHAR(64),
    bank_iban VARCHAR(64),
    bank_swift VARCHAR(32),
    default_cost_center_id UUID REFERENCES financial_accounting.cost_centers (id),
    default_profit_center_id UUID REFERENCES financial_accounting.profit_centers (id),
    default_department_id UUID REFERENCES financial_accounting.departments (id),
    default_project_id UUID REFERENCES financial_accounting.projects (id),
    default_business_area_id UUID REFERENCES financial_accounting.business_areas (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, vendor_number)
);

CREATE INDEX IF NOT EXISTS ix_vendors_company_code
    ON financial_ap.vendors (company_code_id);
CREATE INDEX IF NOT EXISTS ix_vendors_status
    ON financial_ap.vendors (status);
