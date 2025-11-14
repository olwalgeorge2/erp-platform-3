package com.erp.finance.accounting.infrastructure.outbox

enum class FinanceOutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
