package com.erp.finance.accounting.infrastructure.outbox

import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.DimensionEventAction
import com.erp.finance.accounting.domain.model.DimensionType
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.JournalEntry
import com.erp.finance.accounting.domain.model.JournalEntryLine
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

data class JournalPostedEventPayload(
    val eventId: UUID,
    val eventType: String,
    val version: Int,
    val occurredAt: Instant,
    val tenantId: UUID,
    val ledgerId: UUID,
    val journalEntryId: UUID,
    val postingBatchId: UUID?,
    val periodId: UUID,
    val reference: String?,
    val description: String?,
    val totalDebitsMinor: Long,
    val totalCreditsMinor: Long,
    val currency: String,
    val lines: List<JournalEntryLinePayload>,
    val metadata: Map<String, String> = emptyMap(),
) {
    companion object {
        fun from(entry: JournalEntry): JournalPostedEventPayload {
            val totals =
                entry.lines.fold(0L to 0L) { acc, line ->
                    if (line.direction == EntryDirection.DEBIT) {
                        (acc.first + line.amount.amount) to acc.second
                    } else {
                        acc.first to (acc.second + line.amount.amount)
                    }
                }
            val currency = entry.lines.firstOrNull()?.currency ?: "USD"
            val occurredAt = entry.postedAt ?: entry.bookedAt
            return JournalPostedEventPayload(
                eventId = UUID.randomUUID(),
                eventType = "finance.journal.posted",
                version = 1,
                occurredAt = occurredAt,
                tenantId = entry.tenantId,
                ledgerId = entry.ledgerId.value,
                journalEntryId = entry.id,
                postingBatchId = null,
                periodId = entry.accountingPeriodId.value,
                reference = entry.reference,
                description = entry.description,
                totalDebitsMinor = totals.first,
                totalCreditsMinor = totals.second,
                currency = currency,
                lines = entry.lines.map(JournalEntryLinePayload::from),
            )
        }
    }
}

data class JournalEntryLinePayload(
    val accountId: UUID,
    val direction: EntryDirection,
    val amountMinor: Long,
    val currency: String,
    val description: String?,
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
) {
    companion object {
        fun from(line: JournalEntryLine): JournalEntryLinePayload =
            JournalEntryLinePayload(
                accountId = line.accountId.value,
                direction = line.direction,
                amountMinor = line.amount.amount,
                currency = line.currency,
                description = line.description,
                costCenterId = line.dimensions.costCenterId,
                profitCenterId = line.dimensions.profitCenterId,
                departmentId = line.dimensions.departmentId,
                projectId = line.dimensions.projectId,
                businessAreaId = line.dimensions.businessAreaId,
            )
    }
}

data class DimensionChangedEventPayload(
    val eventId: UUID = UUID.randomUUID(),
    val eventType: String = "finance.dimension.changed",
    val version: Int = 1,
    val occurredAt: Instant = Instant.now(),
    val tenantId: UUID,
    val dimensionId: UUID,
    val companyCodeId: UUID,
    val dimensionType: DimensionType,
    val action: DimensionEventAction,
    val code: String,
    val name: String,
    val status: String,
    val validFrom: Instant,
    val validTo: Instant?,
) {
    companion object {
        fun from(
            dimension: AccountingDimension,
            action: DimensionEventAction,
        ): DimensionChangedEventPayload =
            DimensionChangedEventPayload(
                tenantId = dimension.tenantId,
                dimensionId = dimension.id,
                companyCodeId = dimension.companyCodeId,
                dimensionType = dimension.type,
                action = action,
                code = dimension.code,
                name = dimension.name,
                status = dimension.status.name,
                validFrom = dimension.validFrom.atStartOfDay(ZoneOffset.UTC).toInstant(),
                validTo = dimension.validTo?.atStartOfDay(ZoneOffset.UTC)?.toInstant(),
            )
    }
}

data class PeriodStatusEventPayload(
    val eventId: UUID,
    val eventType: String,
    val version: Int,
    val occurredAt: Instant,
    val tenantId: UUID,
    val ledgerId: UUID,
    val periodId: UUID,
    val periodCode: String,
    val previousStatus: String,
    val currentStatus: String,
    val freezeOnly: Boolean,
    val performedBy: String? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    companion object {
        fun from(
            period: AccountingPeriod,
            previousStatus: AccountingPeriodStatus,
        ): PeriodStatusEventPayload =
            PeriodStatusEventPayload(
                eventId = UUID.randomUUID(),
                eventType = resolveEventType(period.status, previousStatus),
                version = 1,
                occurredAt = Instant.now(),
                tenantId = period.tenantId,
                ledgerId = period.ledgerId.value,
                periodId = period.id.value,
                periodCode = period.code,
                previousStatus = previousStatus.name,
                currentStatus = period.status.name,
                freezeOnly = period.status == AccountingPeriodStatus.FROZEN,
            )

        private fun resolveEventType(
            current: AccountingPeriodStatus,
            previous: AccountingPeriodStatus,
        ): String =
            when (current) {
                AccountingPeriodStatus.CLOSED -> "finance.period.closed"
                AccountingPeriodStatus.FROZEN -> "finance.period.frozen"
                AccountingPeriodStatus.OPEN ->
                    if (previous == AccountingPeriodStatus.OPEN) {
                        "finance.period.open"
                    } else {
                        "finance.period.reopened"
                    }
            }
    }
}
