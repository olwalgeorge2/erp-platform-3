package com.erp.identity.domain.model.tenant

import java.time.Instant

/**
 * Tenant aggregate root representing a multi-tenant organization.
 * Encapsulates all business rules related to tenant lifecycle and subscription management.
 */
data class Tenant(
    val id: TenantId,
    val name: String,
    val slug: String, // URL-friendly identifier (e.g., acme-corp)
    val status: TenantStatus,
    val subscription: Subscription,
    val organization: Organization?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(name.isNotBlank()) { "Tenant name cannot be blank" }
        require(slug.matches(Regex("^[a-z0-9-]+$"))) { "Slug must be lowercase alphanumeric with hyphens" }
        require(slug.length in 3..50) { "Slug must be between 3 and 50 characters" }
    }

    /**
     * Activate a provisioning tenant
     */
    fun activate(): Tenant {
        require(status == TenantStatus.PROVISIONING) { "Can only activate provisioning tenants" }
        return copy(
            status = TenantStatus.ACTIVE,
            updatedAt = Instant.now(),
        )
    }

    /**
     * Suspend an active tenant
     */
    fun suspend(reason: String): Tenant {
        require(status == TenantStatus.ACTIVE) { "Can only suspend active tenants" }
        return copy(
            status = TenantStatus.SUSPENDED,
            updatedAt = Instant.now(),
            metadata = metadata + ("suspension_reason" to reason),
        )
    }

    /**
     * Reactivate a suspended tenant
     */
    fun reactivate(): Tenant {
        require(status == TenantStatus.SUSPENDED) { "Can only reactivate suspended tenants" }
        return copy(
            status = TenantStatus.ACTIVE,
            updatedAt = Instant.now(),
            metadata = metadata - "suspension_reason",
        )
    }

    /**
     * Expire tenant subscription
     */
    fun expire(): Tenant {
        require(status == TenantStatus.ACTIVE || status == TenantStatus.SUSPENDED) {
            "Can only expire active or suspended tenants"
        }
        return copy(
            status = TenantStatus.EXPIRED,
            updatedAt = Instant.now(),
        )
    }

    /**
     * Archive a tenant
     */
    fun archive(): Tenant {
        require(status != TenantStatus.DELETED) { "Cannot archive deleted tenant" }
        return copy(
            status = TenantStatus.ARCHIVED,
            updatedAt = Instant.now(),
        )
    }

    /**
     * Mark tenant for deletion
     */
    fun delete(): Tenant =
        copy(
            status = TenantStatus.DELETED,
            updatedAt = Instant.now(),
        )

    /**
     * Update subscription
     */
    fun updateSubscription(newSubscription: Subscription): Tenant {
        require(status == TenantStatus.ACTIVE) { "Can only update subscription for active tenants" }
        return copy(
            subscription = newSubscription,
            updatedAt = Instant.now(),
        )
    }

    /**
     * Check if tenant is operational
     */
    fun isOperational(): Boolean = status == TenantStatus.ACTIVE && subscription.isActive()

    companion object {
        /**
         * Factory method to create a new tenant in provisioning state
         */
        fun provision(
            name: String,
            slug: String,
            subscription: Subscription,
            organization: Organization?,
        ): Tenant {
            val now = Instant.now()
            return Tenant(
                id = TenantId.generate(),
                name = name,
                slug = slug,
                status = TenantStatus.PROVISIONING,
                subscription = subscription,
                organization = organization,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
