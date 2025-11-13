-- Additional index coverage for tenant and role queries.
-- Aligns with roadmap Task 3.1 "Database indexes" work item.

-- Tenant listings often filter on status and always sort by created_at desc.
CREATE INDEX IF NOT EXISTS idx_identity_tenants_status_created
    ON identity_tenants (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_identity_tenants_created
    ON identity_tenants (created_at DESC);

-- Role queries filter by tenant and name plus ordered listings.
CREATE INDEX IF NOT EXISTS idx_identity_roles_tenant_created
    ON identity_roles (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_identity_roles_tenant_lower_name
    ON identity_roles (tenant_id, LOWER(name));

ANALYZE identity_tenants;
ANALYZE identity_roles;
