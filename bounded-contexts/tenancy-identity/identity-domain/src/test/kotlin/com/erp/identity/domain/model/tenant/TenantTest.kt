package com.erp.identity.domain.model.tenant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class TenantTest {
    private val baseSubscription =
        Subscription(
            plan = SubscriptionPlan.PROFESSIONAL,
            startDate = Instant.now().minusSeconds(3600),
            endDate = null,
            maxUsers = 50,
            maxStorage = 50_000,
            features = setOf("rbac"),
        )

    @Test
    fun `activate transitions provisioning tenant to active`() {
        val tenant = Tenant.provision("Acme", "acme", baseSubscription, null)

        val activated = tenant.activate()

        assertEquals(TenantStatus.ACTIVE, activated.status)
    }

    @Test
    fun `activate throws when tenant is not provisioning`() {
        val tenant = Tenant.provision("Acme", "acme", baseSubscription, null).activate()

        assertThrows(IllegalArgumentException::class.java) {
            tenant.activate()
        }
    }

    @Test
    fun `suspend adds suspension reason and updates status`() {
        val tenant = Tenant.provision("Acme", "acme", baseSubscription, null).activate()

        val suspended = tenant.suspend("payment overdue")

        assertEquals(TenantStatus.SUSPENDED, suspended.status)
        assertEquals("payment overdue", suspended.metadata["suspension_reason"])
    }

    @Test
    fun `suspend requires active tenant`() {
        val provisioning = Tenant.provision("Acme", "acme", baseSubscription, null)

        assertThrows(IllegalArgumentException::class.java) {
            provisioning.suspend("ops")
        }
    }

    @Test
    fun `reactivate clears suspension reason`() {
        val tenant = Tenant.provision("Acme", "acme", baseSubscription, null).activate()
        val suspended = tenant.suspend("payment overdue")

        val reactivated = suspended.reactivate()

        assertEquals(TenantStatus.ACTIVE, reactivated.status)
        assertFalse(reactivated.metadata.containsKey("suspension_reason"))
    }

    @Test
    fun `updateSubscription requires active tenant`() {
        val provisioningTenant = Tenant.provision("Acme", "acme", baseSubscription, null)
        val newSubscription =
            baseSubscription.copy(
                plan = SubscriptionPlan.ENTERPRISE,
            )

        assertThrows(IllegalArgumentException::class.java) {
            provisioningTenant.updateSubscription(newSubscription)
        }

        val activeTenant = provisioningTenant.activate()
        val updated = activeTenant.updateSubscription(newSubscription)

        assertEquals(SubscriptionPlan.ENTERPRISE, updated.subscription.plan)
    }

    @Test
    fun `isOperational returns true only for active tenants with active subscriptions`() {
        val activeTenant = Tenant.provision("Acme", "acme", baseSubscription, null).activate()
        val expiredTenant =
            activeTenant.updateSubscription(
                baseSubscription.copy(
                    endDate = Instant.now().minusSeconds(60),
                ),
            )

        assertTrue(activeTenant.isOperational())
        assertFalse(expiredTenant.isOperational())
    }

    @Test
    fun `expire transitions active tenant to expired`() {
        val tenant = Tenant.provision("Acme", "acme", baseSubscription, null).activate()

        val expired = tenant.expire()

        assertEquals(TenantStatus.EXPIRED, expired.status)
    }

    @Test
    fun `archive moves tenant to archived from any non deleted state`() {
        val tenant = Tenant.provision("Acme", "acme", baseSubscription, null).activate()

        val archived = tenant.archive()

        assertEquals(TenantStatus.ARCHIVED, archived.status)
    }

    @Test
    fun `delete marks tenant as deleted`() {
        val tenant = Tenant.provision("Acme", "acme", baseSubscription, null).activate()

        val deleted = tenant.delete()

        assertEquals(TenantStatus.DELETED, deleted.status)
    }

    @Test
    fun `archive rejects deleted tenant`() {
        val deleted = Tenant.provision("Acme", "acme", baseSubscription, null).activate().delete()

        assertThrows(IllegalArgumentException::class.java) {
            deleted.archive()
        }
    }

    @Test
    fun `provision merges provided metadata`() {
        val subscription = baseSubscription
        val reactivated =
            Tenant.provision(
                name = "Meta",
                slug = "meta",
                subscription = subscription,
                organization = null,
            ).copy(metadata = mapOf("existing" to "value"))
                .activate()
                .suspend("maintenance")
                .reactivate()

        assertEquals("value", reactivated.metadata["existing"])
        assertFalse(reactivated.metadata.containsKey("suspension_reason"))
    }

    @Test
    fun `provision validates slug format`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tenant.provision(
                name = "Acme",
                slug = "INVALID!",
                subscription = baseSubscription,
                organization = null,
            )
        }
    }
}
