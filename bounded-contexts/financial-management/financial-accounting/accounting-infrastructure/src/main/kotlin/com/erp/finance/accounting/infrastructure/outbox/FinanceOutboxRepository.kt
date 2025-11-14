package com.erp.finance.accounting.infrastructure.outbox

import java.time.Instant

interface FinanceOutboxRepository {
    fun save(event: FinanceOutboxEventEntity)

    fun fetchPending(
        limit: Int,
        maxAttempts: Int,
    ): List<FinanceOutboxEventEntity>

    fun markPublished(event: FinanceOutboxEventEntity)

    fun markFailed(
        event: FinanceOutboxEventEntity,
        throwable: Throwable,
        maxAttempts: Int,
    )

    fun countPending(maxAttempts: Int): Long

    fun deleteOlderThan(
        statuses: Set<FinanceOutboxEventStatus>,
        cutoff: Instant,
    ): Int
}
