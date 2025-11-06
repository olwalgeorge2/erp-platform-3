package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.success
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class LoggingOutboxMessagePublisher : OutboxMessagePublisher {
    override fun publish(
        eventType: String,
        aggregateId: String?,
        payload: String,
    ): Result<Unit> {
        LOGGER.infof(
            "Publishing outbox event type=%s aggregateId=%s payload=%s",
            eventType,
            aggregateId ?: "n/a",
            payload,
        )
        return success(Unit)
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(LoggingOutboxMessagePublisher::class.java)
    }
}
