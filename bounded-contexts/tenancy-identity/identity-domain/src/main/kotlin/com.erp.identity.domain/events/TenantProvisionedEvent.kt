package com.erp.identity.domain.events

import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.shared.types.events.DomainEvent
import com.erp.shared.types.events.EventVersion
import java.time.Instant
import java.util.UUID

data class TenantProvisionedEvent(
    val tenantId: TenantId,
    val slug: String,
    val status: TenantStatus,
    val subscriptionPlan: SubscriptionPlan,
    val occurredBy: UUID?,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    override val version: EventVersion = EventVersion.initial(),
) : DomainEvent
