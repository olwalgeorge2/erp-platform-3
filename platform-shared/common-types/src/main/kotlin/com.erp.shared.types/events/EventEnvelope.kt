package com.erp.shared.types.events

import java.time.Instant

/**
 * Wraps a domain event with metadata to be dispatched by infrastructure layers.
 */
data class EventEnvelope<T : DomainEvent>(
    val event: T,
    val metadata: EventMetadata = EventMetadata(),
    val publishedAt: Instant = Instant.now(),
)
