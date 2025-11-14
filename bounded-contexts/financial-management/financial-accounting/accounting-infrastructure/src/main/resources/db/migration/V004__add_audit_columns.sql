ALTER TABLE financial_accounting.ledgers
    ADD COLUMN created_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN source_system VARCHAR(64) NOT NULL DEFAULT 'erp-platform';

ALTER TABLE financial_accounting.chart_of_accounts
    ADD COLUMN created_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN source_system VARCHAR(64) NOT NULL DEFAULT 'erp-platform';

ALTER TABLE financial_accounting.accounting_periods
    ADD COLUMN created_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN source_system VARCHAR(64) NOT NULL DEFAULT 'erp-platform';

ALTER TABLE financial_accounting.journal_entries
    ADD COLUMN created_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by VARCHAR(128) NOT NULL DEFAULT 'system',
    ADD COLUMN source_system VARCHAR(64) NOT NULL DEFAULT 'erp-platform';
