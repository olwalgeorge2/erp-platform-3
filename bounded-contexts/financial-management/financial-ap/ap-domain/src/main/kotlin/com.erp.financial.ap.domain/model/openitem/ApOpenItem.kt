package com.erp.financial.ap.domain.model.openitem

import com.erp.financial.shared.masterdata.PaymentTermType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ApOpenItem(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorId: UUID,
    val invoiceId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val originalAmountMinor: Long,
    val clearedAmountMinor: Long,
    val status: ApOpenItemStatus,
    val paymentTermsCode: String,
    val paymentTermsType: PaymentTermType,
    val paymentTermsDueDays: Int,
    val paymentTermsDiscountPercentage: BigDecimal?,
    val paymentTermsDiscountDays: Int?,
    val cashDiscountDueDate: LocalDate?,
    val journalEntryId: UUID?,
    val lastPaymentDate: LocalDate?,
    val lastStatementSentAt: Instant?,
    val lastDunningSentAt: Instant?,
    val dunningLevel: Int,
    val proposalId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val outstandingAmountMinor: Long = (originalAmountMinor - clearedAmountMinor).coerceAtLeast(0)

    fun isEligibleForDiscount(asOfDate: LocalDate): Boolean =
        cashDiscountDueDate?.let { !asOfDate.isAfter(it) } ?: false

    fun agingDays(asOfDate: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS
            .between(dueDate, asOfDate)
}
