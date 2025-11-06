-- Baseline schema for identity service.

CREATE TABLE IF NOT EXISTS identity_tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    subscription_plan VARCHAR(32) NOT NULL,
    subscription_start_date TIMESTAMP NOT NULL,
    subscription_end_date TIMESTAMP,
    subscription_max_users INT NOT NULL,
    subscription_max_storage BIGINT NOT NULL,
    organization_legal_name VARCHAR(200),
    organization_tax_id VARCHAR(100),
    organization_industry VARCHAR(100),
    organization_contact_email VARCHAR(200),
    organization_contact_phone VARCHAR(50),
    address_street VARCHAR(200),
    address_city VARCHAR(100),
    address_state VARCHAR(100),
    address_postal_code VARCHAR(50),
    address_country VARCHAR(2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_identity_tenants_slug UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS identity_tenant_features (
    tenant_id UUID NOT NULL REFERENCES identity_tenants (id) ON DELETE CASCADE,
    feature VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS identity_tenant_metadata (
    tenant_id UUID NOT NULL REFERENCES identity_tenants (id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500),
    PRIMARY KEY (tenant_id, metadata_key)
);

CREATE TABLE IF NOT EXISTS identity_users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_tenants (id) ON DELETE CASCADE,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(200) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    password_hash VARCHAR(512) NOT NULL,
    password_salt VARCHAR(512) NOT NULL,
    hash_algorithm VARCHAR(16) NOT NULL,
    password_last_changed_at TIMESTAMP NOT NULL,
    password_expires_at TIMESTAMP,
    must_change_on_next_login BOOLEAN NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_login_at TIMESTAMP,
    failed_login_attempts INT NOT NULL,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_identity_users_username UNIQUE (tenant_id, username),
    CONSTRAINT uk_identity_users_email UNIQUE (tenant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_identity_users_tenant ON identity_users (tenant_id);

CREATE TABLE IF NOT EXISTS identity_roles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_tenants (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    is_system BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_identity_roles_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS identity_role_permissions (
    role_id UUID NOT NULL REFERENCES identity_roles (id) ON DELETE CASCADE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    PRIMARY KEY (role_id, resource, action, scope)
);

CREATE TABLE IF NOT EXISTS identity_role_metadata (
    role_id UUID NOT NULL REFERENCES identity_roles (id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500),
    PRIMARY KEY (role_id, metadata_key)
);

CREATE TABLE IF NOT EXISTS identity_user_roles (
    user_id UUID NOT NULL REFERENCES identity_users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS identity_user_metadata (
    user_id UUID NOT NULL REFERENCES identity_users (id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500),
    PRIMARY KEY (user_id, metadata_key)
);

CREATE TABLE IF NOT EXISTS identity_outbox_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(256) NOT NULL,
    aggregate_type VARCHAR(256),
    aggregate_id VARCHAR(64),
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    status VARCHAR(16) NOT NULL,
    tenant_id UUID,
    trace_id UUID,
    published_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    failure_count INT NOT NULL,
    last_error VARCHAR(2000),
    CONSTRAINT uk_identity_outbox_event_id UNIQUE (event_id)
);
