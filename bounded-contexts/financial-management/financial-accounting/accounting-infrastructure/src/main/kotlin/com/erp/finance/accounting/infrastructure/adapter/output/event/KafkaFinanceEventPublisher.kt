package com.erp.finance.accounting.infrastructure.adapter.output.event

import com.erp.finance.accounting.infrastructure.outbox.FinanceOutboxEventEntity
import com.erp.finance.accounting.infrastructure.outbox.FinanceOutboxMessagePublisher
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.jboss.logging.Logger

@ApplicationScoped
class KafkaFinanceEventPublisher(
    @Channel("finance-journal-events-out") private val journalEmitter: Emitter<String>,
    @Channel("finance-period-events-out") private val periodEmitter: Emitter<String>,
    @Channel("finance-dimension-events-out") private val dimensionEmitter: Emitter<String>,
) : FinanceOutboxMessagePublisher {
    override fun publish(event: FinanceOutboxEventEntity) {
        val emitter =
            when (event.channel) {
                "finance-journal-events-out" -> journalEmitter
                "finance-period-events-out" -> periodEmitter
                "finance-dimension-events-out" -> dimensionEmitter
                else -> {
                    LOGGER.errorf("Unknown finance outbox channel %s", event.channel)
                    throw IllegalArgumentException("Unsupported channel ${event.channel}")
                }
            }

        emitter.send(event.payload)
        LOGGER.debugf(
            "Published finance outbox event %s to %s",
            event.id,
            event.channel,
        )
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(KafkaFinanceEventPublisher::class.java)
    }
}
