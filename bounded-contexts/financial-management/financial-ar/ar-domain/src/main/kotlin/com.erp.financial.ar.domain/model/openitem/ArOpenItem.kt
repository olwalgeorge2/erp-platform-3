package com.erp.financial.ar.domain.model.openitem

import com.erp.financial.shared.masterdata.PaymentTermType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ArOpenItem(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerId: UUID,
    val invoiceId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val originalAmountMinor: Long,
    val clearedAmountMinor: Long,
    val status: ArOpenItemStatus,
    val paymentTermsCode: String,
    val paymentTermsType: PaymentTermType,
    val paymentTermsDueDays: Int,
    val paymentTermsDiscountPercentage: BigDecimal?,
    val paymentTermsDiscountDays: Int?,
    val cashDiscountDueDate: LocalDate?,
    val journalEntryId: UUID?,
    val lastReceiptDate: LocalDate?,
    val lastDunningSentAt: Instant?,
    val dunningLevel: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val outstandingAmountMinor: Long = (originalAmountMinor - clearedAmountMinor).coerceAtLeast(0)
}
