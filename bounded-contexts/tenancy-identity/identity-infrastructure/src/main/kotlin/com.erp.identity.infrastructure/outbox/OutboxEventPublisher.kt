package com.erp.identity.infrastructure.outbox

import com.erp.identity.application.port.output.EventPublisherPort
import com.erp.shared.types.events.DomainEvent
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class OutboxEventPublisher(
    private val entityManager: EntityManager,
    private val objectMapper: ObjectMapper,
) : EventPublisherPort {
    override fun publish(event: DomainEvent) {
        val entity = OutboxEventEntity.from(event, objectMapper)
        entityManager.persist(entity)
    }

    override fun publish(events: Collection<DomainEvent>) {
        events.forEach(::publish)
    }
}
