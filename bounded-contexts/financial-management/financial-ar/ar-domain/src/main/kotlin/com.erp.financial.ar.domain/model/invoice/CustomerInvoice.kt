package com.erp.financial.ar.domain.model.invoice

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.shared.Money
import com.erp.financial.shared.masterdata.PaymentTerms
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CustomerInvoice(
    val id: CustomerInvoiceId,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val netAmount: Money,
    val taxAmount: Money = Money.zero(currency),
    val receivedAmount: Money = Money.zero(currency),
    val status: CustomerInvoiceStatus,
    val dimensionAssignments: DimensionAssignments = DimensionAssignments(),
    val lines: List<CustomerInvoiceLine>,
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

    fun approve(clock: Clock = Clock.systemUTC()): CustomerInvoice =
        when (status) {
            CustomerInvoiceStatus.DRAFT -> copy(status = CustomerInvoiceStatus.APPROVED, updatedAt = Instant.now(clock))
            CustomerInvoiceStatus.CANCELED -> error("Canceled invoices cannot be approved")
            else -> this
        }

    fun post(clock: Clock = Clock.systemUTC()): CustomerInvoice =
        when (status) {
            CustomerInvoiceStatus.APPROVED, CustomerInvoiceStatus.DRAFT ->
                copy(
                    status = CustomerInvoiceStatus.POSTED,
                    updatedAt = Instant.now(clock),
                    postedAt = Instant.now(clock),
                )
            CustomerInvoiceStatus.CANCELED -> error("Cannot post canceled invoice")
            else -> this
        }

    fun assignJournalEntry(
        journalEntryId: UUID,
        clock: Clock = Clock.systemUTC(),
    ): CustomerInvoice {
        require(status == CustomerInvoiceStatus.POSTED) { "Journal entry can only be assigned for posted invoices" }
        return copy(journalEntryId = journalEntryId, updatedAt = Instant.now(clock))
    }

    fun applyReceipt(
        amount: Money,
        clock: Clock = Clock.systemUTC(),
    ): CustomerInvoice {
        require(amount.currency == currency) { "Receipt currency must match invoice currency" }
        require(amount.amount > 0) { "Receipt amount must be positive" }
        require(status in setOf(CustomerInvoiceStatus.POSTED, CustomerInvoiceStatus.PARTIALLY_PAID)) {
            "Receipts can only be applied to posted invoices"
        }
        val totalDue = totalDueAmount()
        val newReceived = (receivedAmount.amount + amount.amount).coerceAtMost(totalDue)
        val remaining = totalDue - newReceived
        val newStatus =
            when {
                remaining <= 0 -> CustomerInvoiceStatus.PAID
                newReceived > 0 -> CustomerInvoiceStatus.PARTIALLY_PAID
                else -> status
            }
        return copy(
            receivedAmount = Money(newReceived, currency),
            status = newStatus,
            updatedAt = Instant.now(clock),
        )
    }

    fun totalDueAmount(): Long = netAmount.amount + taxAmount.amount

    companion object {
        fun draft(
            tenantId: UUID,
            companyCodeId: UUID,
            customerId: UUID,
            invoiceNumber: String,
            invoiceDate: LocalDate,
            dueDate: LocalDate,
            currency: String,
            lines: List<CustomerInvoiceLine>,
            dimensionAssignments: DimensionAssignments = DimensionAssignments(),
            taxAmount: Money = Money.zero(currency.uppercase()),
            paymentTerms: PaymentTerms,
            clock: Clock = Clock.systemUTC(),
        ): CustomerInvoice {
            val now = Instant.now(clock)
            val normalizedCurrency = currency.uppercase()
            val net =
                lines.fold(Money.zero(normalizedCurrency)) { acc, line ->
                    Money(acc.amount + line.netAmount.amount, normalizedCurrency)
                }
            return CustomerInvoice(
                id = CustomerInvoiceId(),
                tenantId = tenantId,
                companyCodeId = companyCodeId,
                customerId = customerId,
                invoiceNumber = invoiceNumber,
                invoiceDate = invoiceDate,
                dueDate = dueDate,
                currency = normalizedCurrency,
                netAmount = net,
                taxAmount = taxAmount,
                receivedAmount = Money.zero(normalizedCurrency),
                status = CustomerInvoiceStatus.DRAFT,
                dimensionAssignments = dimensionAssignments,
                lines = lines,
                paymentTerms = paymentTerms,
                createdAt = now,
                updatedAt = now,
                postedAt = null,
                journalEntryId = null,
            )
        }
    }
}
