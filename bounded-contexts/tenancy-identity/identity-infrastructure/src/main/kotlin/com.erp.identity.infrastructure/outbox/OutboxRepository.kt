package com.erp.identity.infrastructure.outbox

import com.erp.shared.types.results.Result

interface OutboxRepository {
    fun save(event: OutboxEventEntity): Result<Unit>

    fun fetchPending(
        limit: Int,
        maxAttemptsBeforeFailure: Int,
    ): List<OutboxEventEntity>

    fun markPublished(event: OutboxEventEntity): Result<Unit>

    fun markFailed(
        event: OutboxEventEntity,
        failure: Throwable,
        maxAttemptsBeforeFailure: Int,
    ): Result<Unit>
}
