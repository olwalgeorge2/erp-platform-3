package com.erp.identity.domain.model.tenant

/**
 * Enum representing the lifecycle status of a tenant.
 */
enum class TenantStatus {
    /**
     * Tenant is being provisioned - initial state
     */
    PROVISIONING,

    /**
     * Tenant is active and operational
     */
    ACTIVE,

    /**
     * Tenant is temporarily suspended (non-payment, policy violation)
     */
    SUSPENDED,

    /**
     * Tenant subscription has expired
     */
    EXPIRED,

    /**
     * Tenant is archived - no longer active but data retained
     */
    ARCHIVED,

    /**
     * Tenant is deleted - pending permanent removal
     */
    DELETED,
}
