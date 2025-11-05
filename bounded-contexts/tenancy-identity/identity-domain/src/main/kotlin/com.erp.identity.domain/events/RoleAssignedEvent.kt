package com.erp.identity.domain.events

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.events.DomainEvent
import com.erp.shared.types.events.EventVersion
import java.time.Instant
import java.util.UUID

data class RoleAssignedEvent(
    val tenantId: TenantId,
    val userId: UserId,
    val roleId: RoleId,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    override val version: EventVersion = EventVersion.initial(),
) : DomainEvent
