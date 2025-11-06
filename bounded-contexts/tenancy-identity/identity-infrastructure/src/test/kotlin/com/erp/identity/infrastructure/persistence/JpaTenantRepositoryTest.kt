package com.erp.identity.infrastructure.persistence

import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.identity.domain.model.tenant.Tenant
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.time.Instant
import com.erp.identity.infrastructure.persistence.entity.TenantEntity

class JpaTenantRepositoryTest {
    private val entityManager = mockk<EntityManager>(relaxed = true)
    private val repository = JpaTenantRepository(entityManager)

    @Test
    fun `save returns tenant slug exists when unique constraint violated`() {
        every { entityManager.merge(any<TenantEntity>()) } throws PersistenceException(
            "constraint violated",
            ConstraintViolationException(
                "duplicate slug",
                SQLException("duplicate key uk_identity_tenants_slug"),
                "INSERT ...",
                "uk_identity_tenants_slug",
            ),
        )

        val result = repository.save(sampleTenant())

        assertTrue(result.isFailure())
        val failure = result as com.erp.shared.types.results.Result.Failure
        assertEquals("TENANT_SLUG_EXISTS", failure.error.code)
    }

    private fun sampleTenant(): Tenant {
        val subscription =
            Subscription(
                plan = SubscriptionPlan.PROFESSIONAL,
                startDate = Instant.now(),
                endDate = null,
                maxUsers = 100,
                maxStorage = 1024 * 1024,
                features = setOf("rbac"),
            )

        return Tenant.provision(
            name = "Acme Corp",
            slug = "acme-corp",
            subscription = subscription,
            organization =
                Organization(
                    legalName = "Acme Corp LLC",
                    taxId = "123456789",
                    industry = "Manufacturing",
                    address = null,
                    contactEmail = "admin@acme.test",
                    contactPhone = null,
                ),
        )
    }
}
