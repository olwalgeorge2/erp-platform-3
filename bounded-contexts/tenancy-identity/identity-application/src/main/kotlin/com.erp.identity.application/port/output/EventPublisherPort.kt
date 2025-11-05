package com.erp.identity.application.port.output

import com.erp.shared.types.events.DomainEvent

interface EventPublisherPort {
    fun publish(event: DomainEvent)

    fun publish(events: Collection<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
