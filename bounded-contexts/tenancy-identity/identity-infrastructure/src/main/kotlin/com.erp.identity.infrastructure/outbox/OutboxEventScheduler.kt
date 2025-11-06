package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result
import io.quarkus.scheduler.Scheduled
import io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger

@ApplicationScoped
class OutboxEventScheduler(
    private val outboxRepository: OutboxRepository,
    private val messagePublisher: OutboxMessagePublisher,
) {
    @Scheduled(every = "5s", concurrentExecution = SKIP)
    @Transactional(TxType.REQUIRES_NEW)
    fun publishPendingEvents() {
        val pendingEvents = outboxRepository.fetchPending(BATCH_SIZE, MAX_ATTEMPTS)
        if (pendingEvents.isEmpty()) {
            return
        }

        LOGGER.debugf("Dispatching %d outbox events", pendingEvents.size)

        pendingEvents.forEach { event ->
            try {
                when (val publishResult = messagePublisher.publish(event.eventType, event.aggregateId, event.payload)) {
                    is Result.Success -> {
                        outboxRepository.markPublished(event)
                        LOGGER.debugf("Published outbox event %s", event.eventId)
                    }
                    is Result.Failure -> {
                        val cause =
                            publishResult.error.cause
                                ?: RuntimeException(publishResult.error.message)
                        outboxRepository.markFailed(event, cause, MAX_ATTEMPTS)
                        LOGGER.warnf(
                            cause,
                            "Failed to publish outbox event %s: %s",
                            event.eventId,
                            publishResult.error.message,
                        )
                    }
                }
            } catch (ex: Exception) {
                outboxRepository.markFailed(event, ex, MAX_ATTEMPTS)
                LOGGER.errorf(
                    ex,
                    "Unhandled exception while publishing outbox event %s",
                    event.eventId,
                )
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_ATTEMPTS = 5
        private val LOGGER: Logger = Logger.getLogger(OutboxEventScheduler::class.java)
    }
}
