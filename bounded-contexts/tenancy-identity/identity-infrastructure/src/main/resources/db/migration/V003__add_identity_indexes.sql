-- Optimize user lookups by status and creation time
CREATE INDEX IF NOT EXISTS idx_identity_users_tenant_status
    ON identity_users (tenant_id, status)
    WHERE status <> 'DELETED';

CREATE INDEX IF NOT EXISTS idx_identity_users_tenant_created_at
    ON identity_users (tenant_id, created_at DESC);

-- Accelerate role resolution inside command handlers
CREATE INDEX IF NOT EXISTS idx_identity_roles_tenant
    ON identity_roles (tenant_id);

-- Improve outbox polling (pending + retry flows)
CREATE INDEX IF NOT EXISTS idx_identity_outbox_pending_events
    ON identity_outbox_events (recorded_at)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_identity_outbox_failed_events
    ON identity_outbox_events (status, failure_count, last_attempt_at);

-- Enforce case-insensitive uniqueness for tenant slugs
CREATE UNIQUE INDEX IF NOT EXISTS idx_identity_tenants_slug_lower
    ON identity_tenants (LOWER(slug));

-- Refresh planner statistics for affected tables
ANALYZE identity_users;
ANALYZE identity_roles;
ANALYZE identity_outbox_events;
ANALYZE identity_tenants;
