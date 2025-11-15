-- Stage 1: company code abstraction layered above tenants

CREATE TABLE IF NOT EXISTS financial_accounting.company_codes (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    legal_entity_name VARCHAR(255) NOT NULL,
    country_code CHAR(2) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_company_codes_tenant_code
    ON financial_accounting.company_codes (tenant_id, code);

-- Mapping between company codes and ledgers for statutory reporting
CREATE TABLE IF NOT EXISTS financial_accounting.company_code_ledgers (
    company_code_id UUID NOT NULL REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE,
    ledger_id UUID NOT NULL REFERENCES financial_accounting.ledgers (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_code_id, ledger_id)
);
CREATE INDEX IF NOT EXISTS ix_company_code_ledgers_ledger
    ON financial_accounting.company_code_ledgers (ledger_id);

-- Backfill foreign key constraints for dimension catalogs
ALTER TABLE financial_accounting.cost_centers
    ADD CONSTRAINT fk_cost_centers_company_code FOREIGN KEY (company_code_id) REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE;
ALTER TABLE financial_accounting.profit_centers
    ADD CONSTRAINT fk_profit_centers_company_code FOREIGN KEY (company_code_id) REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE;
ALTER TABLE financial_accounting.departments
    ADD CONSTRAINT fk_departments_company_code FOREIGN KEY (company_code_id) REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE;
ALTER TABLE financial_accounting.projects
    ADD CONSTRAINT fk_projects_company_code FOREIGN KEY (company_code_id) REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE;
ALTER TABLE financial_accounting.business_areas
    ADD CONSTRAINT fk_business_areas_company_code FOREIGN KEY (company_code_id) REFERENCES financial_accounting.company_codes (id) ON DELETE CASCADE;
