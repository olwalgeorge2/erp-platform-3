package com.erp.identity.domain.services

import com.erp.identity.domain.events.TenantProvisionedEvent
import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.shared.types.results.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class TenantProvisioningServiceTest {
    private val subscription =
        Subscription(
            plan = SubscriptionPlan.STARTER,
            startDate = Instant.now().minusSeconds(3600),
            endDate = null,
            maxUsers = 25,
            maxStorage = 10_000,
            features = setOf("rbac"),
        )
    private val organization =
        Organization(
            legalName = "Acme Corp LLC",
            taxId = "123456789",
            industry = "Manufacturing",
            address = null,
            contactEmail = "ops@acme.test",
            contactPhone = null,
        )

    @Test
    fun `provisionTenant normalizes slug and merges metadata`() {
        val checker = RecordingSlugChecker(Result.success(true))
        val service = TenantProvisioningService(checker)
        val metadata = mapOf("region" to "us-east-1")
        val requestedBy = UUID.randomUUID()

        val result =
            service.provisionTenant(
                name = "  Acme Corp ",
                slug = "  Acme-Org ",
                subscription = subscription,
                organization = organization,
                metadata = metadata,
                requestedBy = requestedBy,
            )

        assertTrue(result is Result.Success<TenantProvisioningResult>)
        val success = result as Result.Success<TenantProvisioningResult>
        val tenant = success.value.tenant
        val event: TenantProvisionedEvent = success.value.event

        assertEquals("acme-org", checker.recordedSlug)
        assertEquals("Acme Corp", tenant.name)
        assertEquals("acme-org", tenant.slug)
        assertEquals(metadata, tenant.metadata)
        assertEquals(tenant.id, event.tenantId)
        assertEquals(tenant.slug, event.slug)
        assertEquals(tenant.subscription.plan, event.subscriptionPlan)
        assertEquals(requestedBy, event.occurredBy)
    }

    @Test
    fun `provisionTenant returns failure when slug already exists`() {
        val checker = RecordingSlugChecker(Result.success(false))
        val service = TenantProvisioningService(checker)

        val result =
            service.provisionTenant(
                name = "Acme Corp",
                slug = "acme-org",
                subscription = subscription,
                organization = organization,
            )

        assertTrue(result is Result.Failure)
        val failure = result as Result.Failure
        assertEquals("TENANT_SLUG_EXISTS", failure.error.code)
        assertEquals("acme-org", failure.error.details["slug"])
    }

    @Test
    fun `provisionTenant propagates uniqueness check failure`() {
        val uniquenessFailure: Result<Boolean> =
            Result.failure(
                code = "SLUG_CHECK_FAILED",
                message = "Unable to verify slug uniqueness",
            )
        val checker = RecordingSlugChecker(uniquenessFailure)
        val service = TenantProvisioningService(checker)

        val result =
            service.provisionTenant(
                name = "Acme Corp",
                slug = "acme-org",
                subscription = subscription,
                organization = organization,
            )

        assertTrue(result is Result.Failure)
        assertSame(uniquenessFailure, result)
    }

    private class RecordingSlugChecker(
        private val result: Result<Boolean>,
    ) : TenantSlugUniquenessChecker {
        var recordedSlug: String? = null

        override fun isUnique(slug: String): Result<Boolean> {
            recordedSlug = slug
            return result
        }
    }
}
