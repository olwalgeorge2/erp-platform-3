package com.erp.identity.infrastructure.outbox

enum class OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
