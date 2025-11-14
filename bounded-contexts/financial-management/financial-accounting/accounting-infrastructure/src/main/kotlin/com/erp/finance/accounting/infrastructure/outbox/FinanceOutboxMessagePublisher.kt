package com.erp.finance.accounting.infrastructure.outbox

interface FinanceOutboxMessagePublisher {
    fun publish(event: FinanceOutboxEventEntity)
}
