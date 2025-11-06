package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.events.DomainEvent
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_outbox_events",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_outbox_event_id", columnNames = ["event_id"]),
    ],
)
class OutboxEventEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "event_id", nullable = false, updatable = false)
    var eventId: UUID = UUID.randomUUID(),

    @Column(name = "event_type", nullable = false, length = 256)
    var eventType: String = "",

    @Column(name = "aggregate_type", length = 256)
    var aggregateType: String? = null,

    @Column(name = "aggregate_id", length = 64)
    var aggregateId: String? = null,

    @Lob
    @Column(name = "payload", nullable = false)
    var payload: String = "",

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = Instant.now(),

    @Column(name = "recorded_at", nullable = false)
    var recordedAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: OutboxEventStatus = OutboxEventStatus.PENDING,

    @Column(name = "tenant_id")
    var tenantId: UUID? = null,

    @Column(name = "trace_id")
    var traceId: UUID? = null,
) {
    companion object {
        fun from(
            event: DomainEvent,
            objectMapper: ObjectMapper,
        ): OutboxEventEntity {
            val payload = objectMapper.writeValueAsString(event)
            return OutboxEventEntity(
                eventId = event.eventId,
                eventType = event.type(),
                aggregateType = event.javaClass.declaringClass?.name ?: event.javaClass.name,
                aggregateId = extractAggregateId(event),
                payload = payload,
                occurredAt = event.occurredAt,
                recordedAt = Instant.now(),
                status = OutboxEventStatus.PENDING,
                tenantId = extractTenantId(event),
                traceId = null,
            )
        }

        private fun extractAggregateId(event: DomainEvent): String? {
            val candidateNames = listOf("userId", "tenantId", "aggregateId", "id")
            candidateNames.forEach { property ->
                val value =
                    runCatching {
                        val method = event.javaClass.methods.firstOrNull { it.name.equals("get${property.replaceFirstChar { it.uppercase() }}", ignoreCase = false) }
                        method?.invoke(event)
                    }.getOrNull()

                val asString =
                    when (value) {
                        is com.erp.identity.domain.model.identity.UserId -> value.value.toString()
                        is com.erp.identity.domain.model.tenant.TenantId -> value.value.toString()
                        is UUID -> value.toString()
                        else -> value?.toString()
                    }
                if (!asString.isNullOrBlank()) {
                    return asString
                }
            }
            return null
        }

        private fun extractTenantId(event: DomainEvent): UUID? =
            runCatching {
                val method = event.javaClass.methods.firstOrNull { it.name == "getTenantId" && it.parameterCount == 0 }
                val value = method?.invoke(event)
                when (value) {
                    is com.erp.identity.domain.model.tenant.TenantId -> value.value
                    is UUID -> value
                    is String -> UUID.fromString(value)
                    else -> null
                }
            }.getOrNull()
    }
}
