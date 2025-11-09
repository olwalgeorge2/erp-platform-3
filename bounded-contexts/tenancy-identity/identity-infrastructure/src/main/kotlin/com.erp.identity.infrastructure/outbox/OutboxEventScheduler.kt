package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
    private val meterRegistry: MeterRegistry,
) {
    private val publishedCounter: Counter =
        meterRegistry.counter(
            "identity.outbox.events.published",
            "outcome",
            "success",
        )

    private val failedCounter: Counter =
        meterRegistry.counter(
            "identity.outbox.events.published",
            "outcome",
            "failure",
        )

    private val batchTimer: Timer =
        meterRegistry.timer(
            "identity.outbox.batch.duration",
        )

    @Scheduled(every = "5s", concurrentExecution = SKIP)
    @Transactional(TxType.REQUIRES_NEW)
    fun publishPendingEvents() {
        batchTimer.recordCallable {
            val pendingEvents = outboxRepository.fetchPending(BATCH_SIZE, MAX_ATTEMPTS)
            if (pendingEvents.isEmpty()) {
                return@recordCallable
            }

            LOGGER.debugf("Dispatching %d outbox events", pendingEvents.size)

            var successCount = 0
            var failureCount = 0

            pendingEvents.forEach { event ->
                try {
                    when (
                        val publishResult =
                            messagePublisher.publish(
                                event.eventType,
                                event.aggregateId,
                                event.payload,
                            )
                    ) {
                        is Result.Success -> {
                            outboxRepository.markPublished(event)
                            publishedCounter.increment()
                            successCount++
                            LOGGER.debugf("Published outbox event %s", event.eventId)
                        }
                        is Result.Failure -> {
                            val cause =
                                publishResult.error.cause
                                    ?: RuntimeException(publishResult.error.message)
                            outboxRepository.markFailed(event, cause, MAX_ATTEMPTS)
                            failedCounter.increment()
                            failureCount++
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
                    failedCounter.increment()
                    failureCount++
                    LOGGER.errorf(
                        ex,
                        "Unhandled exception while publishing outbox event %s",
                        event.eventId,
                    )
                }
            }

            if (successCount > 0 || failureCount > 0) {
                LOGGER.infof(
                    "Outbox batch complete: success=%d, failure=%d, total=%d",
                    successCount,
                    failureCount,
                    pendingEvents.size,
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
