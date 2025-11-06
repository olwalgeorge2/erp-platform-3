-- Identity Service Index Optimizations
-- Aligns with Priority 1 (database performance) in Task 3.1 roadmap follow-up.

-- User lookups during authentication and tenant administration frequently filter by status.
CREATE INDEX IF NOT EXISTS idx_identity_users_tenant_status_created
    ON identity_users (tenant_id, status, created_at DESC);

-- Support credential reset / auditing by last update ordering.
CREATE INDEX IF NOT EXISTS idx_identity_users_tenant_updated
    ON identity_users (tenant_id, updated_at DESC);

-- Outbox polling scans pending events ordered by recorded_at.
CREATE INDEX IF NOT EXISTS idx_identity_outbox_pending
    ON identity_outbox_events (status, recorded_at)
    WHERE status = 'PENDING';

-- Tenant-specific replay/monitoring for outbox events.
CREATE INDEX IF NOT EXISTS idx_identity_outbox_tenant_status
    ON identity_outbox_events (tenant_id, status);
