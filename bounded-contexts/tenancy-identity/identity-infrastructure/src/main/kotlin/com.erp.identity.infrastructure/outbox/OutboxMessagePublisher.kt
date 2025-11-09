package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result

interface OutboxMessagePublisher {
    fun publish(
        eventType: String,
        aggregateId: String?,
        payload: String,
        version: Int = 1,
    ): Result<Unit>
}
