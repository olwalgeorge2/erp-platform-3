package com.erp.finance.accounting.infrastructure.outbox

import com.erp.finance.accounting.application.port.output.FinanceEventPublisher
import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.DimensionEventAction
import com.erp.finance.accounting.domain.model.JournalEntry
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class FinanceOutboxPublisher(
    private val outboxRepository: FinanceOutboxRepository,
    private val objectMapper: ObjectMapper,
) : FinanceEventPublisher {
    override fun publishJournalPosted(entry: JournalEntry) {
        val payload = JournalPostedEventPayload.from(entry)
        val event =
            FinanceOutboxEventEntity(
                eventType = payload.eventType,
                channel = "finance-journal-events-out",
                payload = objectMapper.writeValueAsString(payload),
                version = payload.version,
                occurredAt = payload.occurredAt,
            )
        outboxRepository.save(event)
    }

    override fun publishPeriodUpdated(
        period: AccountingPeriod,
        previousStatus: AccountingPeriodStatus,
    ) {
        val payload = PeriodStatusEventPayload.from(period, previousStatus)
        val event =
            FinanceOutboxEventEntity(
                eventType = payload.eventType,
                channel = "finance-period-events-out",
                payload = objectMapper.writeValueAsString(payload),
                version = payload.version,
                occurredAt = payload.occurredAt,
            )
        outboxRepository.save(event)
    }

    override fun publishDimensionChanged(
        dimension: AccountingDimension,
        action: DimensionEventAction,
    ) {
        val payload = DimensionChangedEventPayload.from(dimension, action)
        val event =
            FinanceOutboxEventEntity.dimension(
                payload = objectMapper.writeValueAsString(payload),
                version = payload.version,
                occurredAt = payload.occurredAt,
            )
        outboxRepository.save(event)
    }
}
