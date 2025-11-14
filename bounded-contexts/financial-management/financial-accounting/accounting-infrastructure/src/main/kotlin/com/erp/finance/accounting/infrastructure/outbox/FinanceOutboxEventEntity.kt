package com.erp.finance.accounting.infrastructure.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "finance_outbox_events", schema = "financial_accounting")
class FinanceOutboxEventEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "event_type", nullable = false, length = 256)
    var eventType: String,
    @Column(name = "channel", nullable = false, length = 128)
    var channel: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String,
    @Column(name = "version", nullable = false)
    var version: Int = 1,
    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant,
    @Column(name = "recorded_at", nullable = false)
    var recordedAt: Instant = Instant.now(),
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: FinanceOutboxEventStatus = FinanceOutboxEventStatus.PENDING,
    @Column(name = "failure_count", nullable = false)
    var failureCount: Int = 0,
    @Column(name = "last_error", length = 2000)
    var lastError: String? = null,
    @Column(name = "last_attempt_at")
    var lastAttemptAt: Instant? = null,
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) {
    fun markPublished() {
        status = FinanceOutboxEventStatus.PUBLISHED
        publishedAt = Instant.now()
        lastAttemptAt = publishedAt
        lastError = null
    }

    fun markForRetry(
        failure: Throwable,
        maxAttemptsBeforeFailure: Int,
    ) {
        failureCount += 1
        lastAttemptAt = Instant.now()
        lastError = failure.message?.take(2000)
        status =
            if (failureCount >= maxAttemptsBeforeFailure) {
                FinanceOutboxEventStatus.FAILED
            } else {
                FinanceOutboxEventStatus.PENDING
            }
    }

    companion object {
        fun journal(
            payload: String,
            version: Int,
            occurredAt: Instant,
        ): FinanceOutboxEventEntity =
            FinanceOutboxEventEntity(
                eventType = "finance.journal.posted",
                channel = "finance-journal-events-out",
                payload = payload,
                version = version,
                occurredAt = occurredAt,
            )

        fun period(
            payload: String,
            version: Int,
            occurredAt: Instant,
        ): FinanceOutboxEventEntity =
            FinanceOutboxEventEntity(
                eventType = "finance.period.updated",
                channel = "finance-period-events-out",
                payload = payload,
                version = version,
                occurredAt = occurredAt,
            )
    }
}
