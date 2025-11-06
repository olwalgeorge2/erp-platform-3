package com.erp.identity.domain.services

import com.erp.identity.domain.events.TenantProvisionedEvent
import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.failure
import com.erp.shared.types.results.Result.Companion.success
import java.util.UUID

class TenantProvisioningService(
    private val slugUniquenessChecker: TenantSlugUniquenessChecker,
) {
    fun provisionTenant(
        name: String,
        slug: String,
        subscription: Subscription,
        organization: Organization?,
        metadata: Map<String, String> = emptyMap(),
        requestedBy: UUID? = null,
    ): Result<TenantProvisioningResult> {
        val normalizedSlug = slug.trim().lowercase()
        val uniquenessResult = slugUniquenessChecker.isUnique(normalizedSlug)
        val isUnique =
            when (uniquenessResult) {
                is Result.Success -> uniquenessResult.value
                is Result.Failure -> return uniquenessResult
            }
        if (!isUnique) {
            return failure(
                code = "TENANT_SLUG_EXISTS",
                message = "Tenant slug already exists",
                details = mapOf("slug" to normalizedSlug),
            )
        }

        val tenant = Tenant.provision(name.trim(), normalizedSlug, subscription, organization)
        val tenantWithMetadata =
            if (metadata.isEmpty()) tenant else tenant.copy(metadata = tenant.metadata + metadata)

        val event =
            TenantProvisionedEvent(
                tenantId = tenantWithMetadata.id,
                slug = tenantWithMetadata.slug,
                status = tenantWithMetadata.status,
                subscriptionPlan = tenantWithMetadata.subscription.plan,
                occurredBy = requestedBy,
            )

        return success(
            TenantProvisioningResult(
                tenant = tenantWithMetadata,
                event = event,
            ),
        )
    }
}

data class TenantProvisioningResult(
    val tenant: Tenant,
    val event: TenantProvisionedEvent,
)

fun interface TenantSlugUniquenessChecker {
    fun isUnique(slug: String): Result<Boolean>
}
