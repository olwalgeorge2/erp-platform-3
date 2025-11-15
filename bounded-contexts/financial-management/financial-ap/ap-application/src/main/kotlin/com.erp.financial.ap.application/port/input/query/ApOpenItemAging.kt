package com.erp.financial.ap.application.port.input.query

import java.time.LocalDate
import java.util.UUID

data class ApAgingQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val vendorId: UUID? = null,
    val asOfDate: LocalDate,
)

data class AgingDetailResult(
    val asOfDate: LocalDate,
    val lines: List<AgingDetailLine>,
)

data class AgingDetailLine(
    val openItemId: UUID,
    val invoiceId: UUID,
    val vendorId: UUID,
    val invoiceNumber: String,
    val dueDate: LocalDate,
    val bucket: AgingBucket,
    val amountMinor: Long,
    val currency: String,
)

data class AgingSummaryResult(
    val asOfDate: LocalDate,
    val buckets: List<AgingSummaryLine>,
)

data class AgingSummaryLine(
    val bucket: AgingBucket,
    val totalAmountMinor: Long,
    val count: Long,
)

enum class AgingBucket { CURRENT, DAYS_1_30, DAYS_31_60, DAYS_61_90, DAYS_91_PLUS }

interface ApOpenItemQueryUseCase {
    fun getAgingDetail(query: ApAgingQuery): AgingDetailResult

    fun getAgingSummary(query: ApAgingQuery): AgingSummaryResult
}
