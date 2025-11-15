package com.erp.financial.ar.infrastructure.persistence.entity

import com.erp.financial.ar.domain.model.openitem.ArOpenItem
import com.erp.financial.ar.domain.model.openitem.ArOpenItemStatus
import com.erp.financial.shared.masterdata.PaymentTermType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "ar_open_items", schema = "financial_ar")
class ArOpenItemEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @OneToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    var invoice: CustomerInvoiceEntity,
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID,
    @Column(name = "customer_id", nullable = false)
    var customerId: UUID,
    @Column(name = "invoice_number", nullable = false, length = 64)
    var invoiceNumber: String,
    @Column(name = "invoice_date", nullable = false)
    var invoiceDate: LocalDate,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "original_amount_minor", nullable = false)
    var originalAmountMinor: Long,
    @Column(name = "cleared_amount_minor", nullable = false)
    var clearedAmountMinor: Long = 0,
    @Column(name = "amount_outstanding", nullable = false)
    var amountOutstanding: Long,
    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: ArOpenItemStatus = ArOpenItemStatus.OPEN,
    @Column(name = "journal_entry_id")
    var journalEntryId: UUID? = null,
    @Column(name = "payment_terms_code", nullable = false, length = 32)
    var paymentTermsCode: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms_type", nullable = false, length = 32)
    var paymentTermsType: PaymentTermType,
    @Column(name = "payment_terms_due_days", nullable = false)
    var paymentTermsDueDays: Int,
    @Column(name = "payment_terms_discount_percentage", precision = 9, scale = 4)
    var paymentTermsDiscountPercentage: BigDecimal? = null,
    @Column(name = "payment_terms_discount_days")
    var paymentTermsDiscountDays: Int? = null,
    @Column(name = "cash_discount_due_date")
    var cashDiscountDueDate: LocalDate? = null,
    @Column(name = "last_receipt_date")
    var lastReceiptDate: LocalDate? = null,
    @Column(name = "last_dunning_sent_at")
    var lastDunningSentAt: Instant? = null,
    @Column(name = "dunning_level", nullable = false)
    var dunningLevel: Int = 0,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): ArOpenItem =
        ArOpenItem(
            id = id,
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            customerId = customerId,
            invoiceId = invoice.id,
            invoiceNumber = invoiceNumber,
            invoiceDate = invoiceDate,
            dueDate = dueDate,
            currency = currency,
            originalAmountMinor = originalAmountMinor,
            clearedAmountMinor = clearedAmountMinor,
            status = status,
            paymentTermsCode = paymentTermsCode,
            paymentTermsType = paymentTermsType,
            paymentTermsDueDays = paymentTermsDueDays,
            paymentTermsDiscountPercentage = paymentTermsDiscountPercentage,
            paymentTermsDiscountDays = paymentTermsDiscountDays,
            cashDiscountDueDate = cashDiscountDueDate,
            journalEntryId = journalEntryId,
            lastReceiptDate = lastReceiptDate,
            lastDunningSentAt = lastDunningSentAt,
            dunningLevel = dunningLevel,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
