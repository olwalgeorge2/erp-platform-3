package com.erp.identity.infrastructure.outbox

import com.erp.identity.application.port.output.EventPublisherPort
import com.erp.shared.types.events.DomainEvent
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class OutboxEventPublisher(
    private val objectMapper: ObjectMapper,
    private val outboxRepository: OutboxRepository,
) : EventPublisherPort {
    override fun publish(event: DomainEvent) {
        val entity = OutboxEventEntity.from(event, objectMapper)
        outboxRepository.save(entity).onFailure { failure ->
            LOGGER.errorf(
                failure.error.cause,
                "Failed to persist domain event %s into outbox: %s",
                event.type(),
                failure.error.message,
            )
        }
    }

    override fun publish(events: Collection<DomainEvent>) {
        events.forEach(::publish)
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(OutboxEventPublisher::class.java)
    }
}
