-- Add posted_at column to journal_entries
ALTER TABLE financial_accounting.journal_entries
    ADD COLUMN IF NOT EXISTS posted_at TIMESTAMPTZ;

-- Add index for querying posted entries
CREATE INDEX IF NOT EXISTS ix_journal_entries_posted_at
    ON financial_accounting.journal_entries (posted_at)
    WHERE posted_at IS NOT NULL;
