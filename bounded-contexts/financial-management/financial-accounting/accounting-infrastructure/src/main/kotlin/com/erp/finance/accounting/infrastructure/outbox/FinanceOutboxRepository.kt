package com.erp.finance.accounting.infrastructure.outbox

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
}
