package com.erp.financial.ar.application.port.input.query

import java.time.LocalDate
import java.util.UUID

data class ArAgingQuery(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val customerId: UUID? = null,
    val asOfDate: LocalDate,
)

data class ArAgingDetailResult(
    val asOfDate: LocalDate,
    val lines: List<ArAgingDetailLine>,
)

data class ArAgingDetailLine(
    val openItemId: UUID,
    val invoiceId: UUID,
    val customerId: UUID,
    val invoiceNumber: String,
    val dueDate: LocalDate,
    val bucket: ArAgingBucket,
    val amountMinor: Long,
    val currency: String,
)

data class ArAgingSummaryResult(
    val asOfDate: LocalDate,
    val buckets: List<ArAgingSummaryLine>,
)

data class ArAgingSummaryLine(
    val bucket: ArAgingBucket,
    val totalAmountMinor: Long,
    val count: Long,
)

enum class ArAgingBucket { CURRENT, DAYS_1_30, DAYS_31_60, DAYS_61_90, DAYS_91_PLUS }

interface ArOpenItemQueryUseCase {
    fun getAgingDetail(query: ArAgingQuery): ArAgingDetailResult

    fun getAgingSummary(query: ArAgingQuery): ArAgingSummaryResult
}
