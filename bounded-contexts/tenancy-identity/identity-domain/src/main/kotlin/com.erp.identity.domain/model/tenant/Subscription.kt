package com.erp.identity.domain.model.tenant

import java.time.Instant

/**
 * Value object representing a tenant's subscription details.
 */
data class Subscription(
    val plan: SubscriptionPlan,
    val startDate: Instant,
    val endDate: Instant?,
    val maxUsers: Int,
    val maxStorage: Long, // in bytes
    val features: Set<String>,
) {
    init {
        require(maxUsers > 0) { "Max users must be positive" }
        require(maxStorage > 0) { "Max storage must be positive" }
        require(startDate.isBefore(endDate ?: Instant.MAX)) { "Start date must be before end date" }
    }

    fun isExpired(at: Instant = Instant.now()): Boolean = endDate?.isBefore(at) ?: false

    fun isActive(at: Instant = Instant.now()): Boolean = !isExpired(at) && !startDate.isAfter(at)

    fun hasFeature(feature: String): Boolean = features.contains(feature)
}

/**
 * Available subscription plans
 */
enum class SubscriptionPlan {
    FREE,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE,
    CUSTOM,
}
