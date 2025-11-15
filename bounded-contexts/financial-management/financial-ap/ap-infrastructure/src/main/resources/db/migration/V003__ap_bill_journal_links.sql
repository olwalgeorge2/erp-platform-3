ALTER TABLE financial_ap.ap_invoices
    ADD COLUMN IF NOT EXISTS journal_entry_id UUID;

ALTER TABLE financial_ap.ap_open_items
    ADD COLUMN IF NOT EXISTS journal_entry_id UUID;
