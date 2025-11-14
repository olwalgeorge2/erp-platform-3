package com.erp.finance.accounting.infrastructure.outbox

import io.quarkus.scheduler.Scheduled
import io.quarkus.scheduler.Scheduled.ConcurrentExecution
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger

@ApplicationScoped
class FinanceOutboxEventScheduler(
    private val outboxRepository: FinanceOutboxRepository,
    private val messagePublisher: FinanceOutboxMessagePublisher,
) {
    @Scheduled(every = "5s", concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional(TxType.REQUIRES_NEW)
    fun publishPendingEvents() {
        val events = outboxRepository.fetchPending(BATCH_SIZE, MAX_ATTEMPTS)
        if (events.isEmpty()) {
            return
        }

        LOGGER.debugf("Dispatching %d finance outbox events", events.size)

        events.forEach { event ->
            try {
                messagePublisher.publish(event)
                outboxRepository.markPublished(event)
            } catch (ex: Exception) {
                outboxRepository.markFailed(event, ex, MAX_ATTEMPTS)
                LOGGER.warnf(ex, "Failed to publish finance outbox event %s", event.id)
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_ATTEMPTS = 5
        private val LOGGER: Logger = Logger.getLogger(FinanceOutboxEventScheduler::class.java)
    }
}
