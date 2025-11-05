package com.erp.shared.types.aggregate

import com.erp.shared.types.events.DomainEvent

/**
 * Base class for aggregate roots that tracks emitted domain events.
 * Aggregates extend this so the application layer can publish events after persistence succeeds.
 */
abstract class AggregateRoot<ID>(
    open val id: ID,
) {
    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    val domainEvents: List<DomainEvent>
        get() = _domainEvents.toList()

    protected fun registerEvent(event: DomainEvent) {
        _domainEvents += event
    }

    fun clearDomainEvents() {
        _domainEvents.clear()
    }
}
