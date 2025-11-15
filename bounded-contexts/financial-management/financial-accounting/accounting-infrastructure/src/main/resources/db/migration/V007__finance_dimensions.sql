-- Stage 1: core finance dimension catalogs

CREATE TABLE IF NOT EXISTS financial_accounting.cost_centers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (valid_to IS NULL OR valid_to >= valid_from)
);
ALTER TABLE financial_accounting.cost_centers
    ADD CONSTRAINT fk_cost_centers_parent FOREIGN KEY (parent_id) REFERENCES financial_accounting.cost_centers (id) ON DELETE SET NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_cost_centers_tenant_code
    ON financial_accounting.cost_centers (tenant_id, code);
CREATE INDEX IF NOT EXISTS ix_cost_centers_company_code
    ON financial_accounting.cost_centers (company_code_id);

CREATE TABLE IF NOT EXISTS financial_accounting.profit_centers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (valid_to IS NULL OR valid_to >= valid_from)
);
ALTER TABLE financial_accounting.profit_centers
    ADD CONSTRAINT fk_profit_centers_parent FOREIGN KEY (parent_id) REFERENCES financial_accounting.profit_centers (id) ON DELETE SET NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_profit_centers_tenant_code
    ON financial_accounting.profit_centers (tenant_id, code);
CREATE INDEX IF NOT EXISTS ix_profit_centers_company_code
    ON financial_accounting.profit_centers (company_code_id);

CREATE TABLE IF NOT EXISTS financial_accounting.departments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (valid_to IS NULL OR valid_to >= valid_from)
);
ALTER TABLE financial_accounting.departments
    ADD CONSTRAINT fk_departments_parent FOREIGN KEY (parent_id) REFERENCES financial_accounting.departments (id) ON DELETE SET NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_departments_tenant_code
    ON financial_accounting.departments (tenant_id, code);
CREATE INDEX IF NOT EXISTS ix_departments_company_code
    ON financial_accounting.departments (company_code_id);

CREATE TABLE IF NOT EXISTS financial_accounting.projects (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (valid_to IS NULL OR valid_to >= valid_from)
);
ALTER TABLE financial_accounting.projects
    ADD CONSTRAINT fk_projects_parent FOREIGN KEY (parent_id) REFERENCES financial_accounting.projects (id) ON DELETE SET NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_projects_tenant_code
    ON financial_accounting.projects (tenant_id, code);
CREATE INDEX IF NOT EXISTS ix_projects_company_code
    ON financial_accounting.projects (company_code_id);

CREATE TABLE IF NOT EXISTS financial_accounting.business_areas (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id UUID NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (valid_to IS NULL OR valid_to >= valid_from)
);
ALTER TABLE financial_accounting.business_areas
    ADD CONSTRAINT fk_business_areas_parent FOREIGN KEY (parent_id) REFERENCES financial_accounting.business_areas (id) ON DELETE SET NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_business_areas_tenant_code
    ON financial_accounting.business_areas (tenant_id, code);
CREATE INDEX IF NOT EXISTS ix_business_areas_company_code
    ON financial_accounting.business_areas (company_code_id);

-- Account dimension enforcement matrix
CREATE TABLE IF NOT EXISTS financial_accounting.account_dimension_policies (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    dimension_type VARCHAR(32) NOT NULL,
    requirement_level VARCHAR(16) NOT NULL DEFAULT 'OPTIONAL',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CHECK (requirement_level IN ('OPTIONAL', 'MANDATORY')),
    CHECK (dimension_type IN ('COST_CENTER', 'PROFIT_CENTER', 'DEPARTMENT', 'PROJECT', 'BUSINESS_AREA'))
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_dimension_policies_rule
    ON financial_accounting.account_dimension_policies (tenant_id, account_type, dimension_type);

-- Dimension references on journal entry lines
ALTER TABLE financial_accounting.journal_entry_lines
    ADD COLUMN cost_center_id UUID NULL,
    ADD COLUMN profit_center_id UUID NULL,
    ADD COLUMN department_id UUID NULL,
    ADD COLUMN project_id UUID NULL,
    ADD COLUMN business_area_id UUID NULL;

ALTER TABLE financial_accounting.journal_entry_lines
    ADD CONSTRAINT fk_jel_cost_center FOREIGN KEY (cost_center_id) REFERENCES financial_accounting.cost_centers (id),
    ADD CONSTRAINT fk_jel_profit_center FOREIGN KEY (profit_center_id) REFERENCES financial_accounting.profit_centers (id),
    ADD CONSTRAINT fk_jel_department FOREIGN KEY (department_id) REFERENCES financial_accounting.departments (id),
    ADD CONSTRAINT fk_jel_project FOREIGN KEY (project_id) REFERENCES financial_accounting.projects (id),
    ADD CONSTRAINT fk_jel_business_area FOREIGN KEY (business_area_id) REFERENCES financial_accounting.business_areas (id);

CREATE INDEX IF NOT EXISTS ix_jel_cost_center
    ON financial_accounting.journal_entry_lines (cost_center_id);
CREATE INDEX IF NOT EXISTS ix_jel_profit_center
    ON financial_accounting.journal_entry_lines (profit_center_id);
CREATE INDEX IF NOT EXISTS ix_jel_department
    ON financial_accounting.journal_entry_lines (department_id);
CREATE INDEX IF NOT EXISTS ix_jel_project
    ON financial_accounting.journal_entry_lines (project_id);
CREATE INDEX IF NOT EXISTS ix_jel_business_area
    ON financial_accounting.journal_entry_lines (business_area_id);
