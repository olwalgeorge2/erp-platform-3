-- Case-insensitive lookup helpers for identity users.
-- Supports exists/find queries that normalize username/email casing via LOWER().

CREATE INDEX IF NOT EXISTS idx_identity_users_tenant_lower_username
    ON identity_users (tenant_id, LOWER(username));

CREATE INDEX IF NOT EXISTS idx_identity_users_tenant_lower_email
    ON identity_users (tenant_id, LOWER(email));
