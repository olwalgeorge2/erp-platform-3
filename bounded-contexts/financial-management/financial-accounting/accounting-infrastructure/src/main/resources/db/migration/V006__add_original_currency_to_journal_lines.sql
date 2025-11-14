ALTER TABLE financial_accounting.journal_entry_lines
    ADD COLUMN original_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD COLUMN original_amount BIGINT NOT NULL DEFAULT 0;
