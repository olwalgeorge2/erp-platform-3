package com.erp.finance.accounting.infrastructure.outbox

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.quarkus.scheduler.Scheduled
import io.quarkus.scheduler.Scheduled.ConcurrentExecution
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@ApplicationScoped
class FinanceOutboxEventScheduler(
    private val outboxRepository: FinanceOutboxRepository,
    private val messagePublisher: FinanceOutboxMessagePublisher,
    private val meterRegistry: MeterRegistry,
) {
    private val publishedCounter = meterRegistry.counter("finance.outbox.events.published")
    private val failedCounter = meterRegistry.counter("finance.outbox.events.failed")
    private val batchSizeGauge =
        meterRegistry.gauge("finance.outbox.events.batch.size", AtomicInteger(0))!!
    private val pendingGauge =
        meterRegistry.gauge("finance.outbox.events.pending", AtomicLong(0))!!
    private val drainTimer: Timer = meterRegistry.timer("finance.outbox.events.drain")

    @Scheduled(every = "5s", concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional(TxType.REQUIRES_NEW)
    fun publishPendingEvents() {
        val sample = Timer.start(meterRegistry)
        try {
            val pending = outboxRepository.countPending(MAX_ATTEMPTS)
            pendingGauge.set(pending)

            val events = outboxRepository.fetchPending(BATCH_SIZE, MAX_ATTEMPTS)
            batchSizeGauge.set(events.size)

            if (events.isEmpty()) {
                return
            }

            LOGGER.debugf("Dispatching %d finance outbox events", events.size)

            events.forEach { event ->
                try {
                    messagePublisher.publish(event)
                    outboxRepository.markPublished(event)
                    publishedCounter.increment()
                } catch (ex: Exception) {
                    outboxRepository.markFailed(event, ex, MAX_ATTEMPTS)
                    failedCounter.increment()
                    LOGGER.warnf(ex, "Failed to publish finance outbox event %s", event.id)
                }
            }
        } finally {
            sample.stop(drainTimer)
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_ATTEMPTS = 5
        private val LOGGER: Logger = Logger.getLogger(FinanceOutboxEventScheduler::class.java)
    }
}
