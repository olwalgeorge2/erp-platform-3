package com.erp.identity.domain.events

import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.shared.types.events.EventVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class TenantProvisionedEventTest {
    @Test
    fun `defaults populate identifiers and initial version`() {
        val tenantId = TenantId.generate()
        val event =
            TenantProvisionedEvent(
                tenantId = tenantId,
                slug = "acme",
                status = TenantStatus.PROVISIONING,
                subscriptionPlan = SubscriptionPlan.STARTER,
                occurredBy = null,
            )

        assertEquals(tenantId, event.tenantId)
        assertEquals(EventVersion.initial(), event.version)
        assertNotNull(event.eventId)
        assertTrue(!event.occurredAt.isAfter(Instant.now()))
    }

    @Test
    fun `type returns fully qualified event name`() {
        val event =
            TenantProvisionedEvent(
                tenantId = TenantId.generate(),
                slug = "acme",
                status = TenantStatus.PROVISIONING,
                subscriptionPlan = SubscriptionPlan.STARTER,
                occurredBy = null,
            )

        assertTrue(event.type().contains("TenantProvisionedEvent"))
    }
}
