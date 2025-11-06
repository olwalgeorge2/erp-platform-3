package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result

interface OutboxMessagePublisher {
    fun publish(
        eventType: String,
        aggregateId: String?,
        payload: String,
    ): Result<Unit>
}
