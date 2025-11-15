ALTER TABLE financial_ap.ap_invoices
    ADD COLUMN IF NOT EXISTS paid_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_terms_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS payment_terms_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS payment_terms_description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_terms_due_days INTEGER,
    ADD COLUMN IF NOT EXISTS payment_terms_discount_percentage NUMERIC(9, 4),
    ADD COLUMN IF NOT EXISTS payment_terms_discount_days INTEGER;

UPDATE financial_ap.ap_invoices inv
SET payment_terms_code = ven.payment_term_code,
    payment_terms_type = ven.payment_term_type,
    payment_terms_due_days = ven.payment_term_due_days,
    payment_terms_discount_percentage = ven.payment_term_discount_percent,
    payment_terms_discount_days = ven.payment_term_discount_days
FROM financial_ap.vendors ven
WHERE inv.vendor_id = ven.id AND payment_terms_code IS NULL;

ALTER TABLE financial_ap.ap_invoices
    ALTER COLUMN payment_terms_code SET NOT NULL,
    ALTER COLUMN payment_terms_type SET NOT NULL,
    ALTER COLUMN payment_terms_due_days SET NOT NULL;

ALTER TABLE financial_ap.ap_open_items
    ADD COLUMN IF NOT EXISTS company_code_id UUID,
    ADD COLUMN IF NOT EXISTS vendor_id UUID,
    ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(64),
    ADD COLUMN IF NOT EXISTS invoice_date DATE,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3),
    ADD COLUMN IF NOT EXISTS original_amount_minor BIGINT,
    ADD COLUMN IF NOT EXISTS cleared_amount_minor BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_terms_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS payment_terms_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS payment_terms_due_days INTEGER,
    ADD COLUMN IF NOT EXISTS payment_terms_discount_percentage NUMERIC(9, 4),
    ADD COLUMN IF NOT EXISTS payment_terms_discount_days INTEGER,
    ADD COLUMN IF NOT EXISTS cash_discount_due_date DATE,
    ADD COLUMN IF NOT EXISTS last_payment_date DATE,
    ADD COLUMN IF NOT EXISTS last_statement_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_dunning_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS dunning_level INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS proposal_id UUID,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE financial_ap.ap_open_items oi
SET company_code_id = inv.company_code_id,
    vendor_id = inv.vendor_id,
    invoice_number = inv.invoice_number,
    invoice_date = inv.invoice_date,
    currency = inv.currency,
    original_amount_minor = inv.net_amount + inv.tax_amount,
    cleared_amount_minor = COALESCE(inv.paid_amount, 0),
    payment_terms_code = inv.payment_terms_code,
    payment_terms_type = inv.payment_terms_type,
    payment_terms_due_days = inv.payment_terms_due_days,
    payment_terms_discount_percentage = inv.payment_terms_discount_percentage,
    payment_terms_discount_days = inv.payment_terms_discount_days,
    cash_discount_due_date =
        CASE
            WHEN inv.payment_terms_discount_days IS NOT NULL THEN inv.invoice_date + inv.payment_terms_discount_days
            ELSE NULL
        END
FROM financial_ap.ap_invoices inv
WHERE oi.invoice_id = inv.id;

ALTER TABLE financial_ap.ap_open_items
    ALTER COLUMN company_code_id SET NOT NULL,
    ALTER COLUMN vendor_id SET NOT NULL,
    ALTER COLUMN invoice_number SET NOT NULL,
    ALTER COLUMN invoice_date SET NOT NULL,
    ALTER COLUMN currency SET NOT NULL,
    ALTER COLUMN original_amount_minor SET NOT NULL,
    ALTER COLUMN payment_terms_code SET NOT NULL,
    ALTER COLUMN payment_terms_type SET NOT NULL,
    ALTER COLUMN payment_terms_due_days SET NOT NULL;

CREATE TABLE IF NOT EXISTS financial_ap.payment_proposals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_code_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    proposal_date DATE NOT NULL,
    payment_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount_minor BIGINT NOT NULL,
    discount_amount_minor BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS financial_ap.payment_proposal_items (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES financial_ap.payment_proposals (id) ON DELETE CASCADE,
    open_item_id UUID NOT NULL REFERENCES financial_ap.ap_open_items (id) ON DELETE RESTRICT,
    invoice_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    amount_to_pay_minor BIGINT NOT NULL,
    discount_minor BIGINT NOT NULL,
    bucket VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    due_date DATE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_proposal_item_open_item
    ON financial_ap.payment_proposal_items (open_item_id);
