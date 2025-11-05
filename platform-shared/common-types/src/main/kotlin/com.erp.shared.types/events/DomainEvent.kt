package com.erp.shared.types.events

import java.time.Instant
import java.util.UUID

/**
 * Domain events capture state transitions within aggregates.
 */
interface DomainEvent {
    val eventId: UUID
    val occurredAt: Instant
    val version: EventVersion
        get() = EventVersion.initial()

    fun type(): String = this::class.qualifiedName ?: this::class.simpleName ?: "unknown-event"
}
