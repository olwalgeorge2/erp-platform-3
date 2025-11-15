package com.erp.financial.ap.domain.model.bill

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.shared.Money
import com.erp.financial.shared.masterdata.PaymentTerms
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class VendorBill(
    val id: BillId,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val netAmount: Money,
    val taxAmount: Money = Money.zero(currency),
    val paidAmount: Money = Money.zero(currency),
    val status: BillStatus,
    val dimensionAssignments: DimensionAssignments = DimensionAssignments(),
    val lines: List<BillLine>,
    val paymentTerms: PaymentTerms,
    val createdAt: Instant,
    val updatedAt: Instant,
    val postedAt: Instant? = null,
    val journalEntryId: UUID? = null,
) {
    init {
        require(invoiceNumber.isNotBlank()) { "Invoice number must be provided" }
        require(dueDate >= invoiceDate) { "Due date must be on or after invoice date" }
        require(lines.isNotEmpty()) { "At least one line must be provided" }
        val computed =
            lines.fold(0L) { acc, line ->
                require(line.netAmount.currency == currency) { "Line currency mismatch" }
                acc + line.netAmount.amount
            }
        require(computed == netAmount.amount) { "Net amount must equal sum of lines" }
    }

    fun approve(clock: Clock = Clock.systemUTC()): VendorBill =
        when (status) {
            BillStatus.DRAFT -> copy(status = BillStatus.APPROVED, updatedAt = Instant.now(clock))
            BillStatus.CANCELED -> error("Canceled bills cannot be approved")
            else -> this
        }

    fun post(clock: Clock = Clock.systemUTC()): VendorBill =
        when (status) {
            BillStatus.APPROVED, BillStatus.DRAFT ->
                copy(
                    status = BillStatus.POSTED,
                    updatedAt = Instant.now(clock),
                    postedAt = Instant.now(clock),
                )
            BillStatus.CANCELED -> error("Cannot post canceled bill")
            else -> this
        }

    fun assignJournalEntry(
        journalEntryId: UUID,
        clock: Clock = Clock.systemUTC(),
    ): VendorBill {
        require(status == BillStatus.POSTED) { "Journal entry can only be assigned for posted bills" }
        return copy(journalEntryId = journalEntryId, updatedAt = Instant.now(clock))
    }

    fun markPaid(clock: Clock = Clock.systemUTC()): VendorBill =
        when (status) {
            BillStatus.POSTED, BillStatus.PARTIALLY_PAID ->
                copy(status = BillStatus.PAID, updatedAt = Instant.now(clock))
            BillStatus.CANCELED -> error("Cannot pay canceled bill")
            else -> this
        }

    fun cancel(clock: Clock = Clock.systemUTC()): VendorBill =
        if (status == BillStatus.PAID) {
            error("Cannot cancel paid bill")
        } else {
            copy(status = BillStatus.CANCELED, updatedAt = Instant.now(clock))
        }

    fun applyPayment(
        amount: Money,
        clock: Clock = Clock.systemUTC(),
    ): VendorBill {
        require(amount.currency == currency) { "Payment currency must match bill currency" }
        require(amount.amount > 0) { "Payment amount must be positive" }
        require(status in setOf(BillStatus.POSTED, BillStatus.PARTIALLY_PAID)) {
            "Payments can only be applied to posted bills"
        }
        val totalDue = totalDueAmount()
        val newPaid = (paidAmount.amount + amount.amount).coerceAtMost(totalDue)
        val remaining = totalDue - newPaid
        val newStatus =
            when {
                remaining <= 0 -> BillStatus.PAID
                newPaid > 0 -> BillStatus.PARTIALLY_PAID
                else -> status
            }
        return copy(
            paidAmount = Money(newPaid, currency),
            status = newStatus,
            updatedAt = Instant.now(clock),
        )
    }

    fun totalDueAmount(): Long = netAmount.amount + taxAmount.amount

    companion object {
        fun draft(
            tenantId: UUID,
            companyCodeId: UUID,
            vendorId: UUID,
            invoiceNumber: String,
            invoiceDate: LocalDate,
            dueDate: LocalDate,
            currency: String,
            lines: List<BillLine>,
            dimensionAssignments: DimensionAssignments = DimensionAssignments(),
            taxAmount: Money = Money.zero(currency.uppercase()),
            paymentTerms: PaymentTerms,
            clock: Clock = Clock.systemUTC(),
        ): VendorBill {
            val now = Instant.now(clock)
            val normalizedCurrency = currency.uppercase()
            val net =
                lines.fold(Money.zero(normalizedCurrency)) { acc, line ->
                    Money(acc.amount + line.netAmount.amount, normalizedCurrency)
                }
            return VendorBill(
                id = BillId.newId(),
                tenantId = tenantId,
                companyCodeId = companyCodeId,
                vendorId = vendorId,
                invoiceNumber = invoiceNumber,
                invoiceDate = invoiceDate,
                dueDate = dueDate,
                currency = normalizedCurrency,
                netAmount = net,
                taxAmount = taxAmount,
                paidAmount = Money.zero(normalizedCurrency),
                status = BillStatus.DRAFT,
                dimensionAssignments = dimensionAssignments,
                lines = lines,
                paymentTerms = paymentTerms,
                createdAt = now,
                updatedAt = now,
                journalEntryId = null,
            )
        }
    }
}
