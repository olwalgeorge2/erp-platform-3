package com.erp.financial.ar.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceId
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceLine
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import com.erp.financial.shared.Money
import com.erp.financial.shared.masterdata.PaymentTermType
import com.erp.financial.shared.masterdata.PaymentTerms
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "ar_invoices", schema = "financial_ar")
class CustomerInvoiceEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
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
    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "net_amount", nullable = false)
    var netAmount: Long,
    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Long,
    @Column(name = "received_amount", nullable = false)
    var receivedAmount: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: CustomerInvoiceStatus = CustomerInvoiceStatus.DRAFT,
    @Column(name = "cost_center_id")
    var costCenterId: UUID? = null,
    @Column(name = "profit_center_id")
    var profitCenterId: UUID? = null,
    @Column(name = "department_id")
    var departmentId: UUID? = null,
    @Column(name = "project_id")
    var projectId: UUID? = null,
    @Column(name = "business_area_id")
    var businessAreaId: UUID? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(name = "posted_at")
    var postedAt: Instant? = null,
    @Column(name = "journal_entry_id")
    var journalEntryId: UUID? = null,
    @Column(name = "payment_terms_code", nullable = false, length = 32)
    var paymentTermsCode: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms_type", nullable = false, length = 32)
    var paymentTermsType: PaymentTermType,
    @Column(name = "payment_terms_description")
    var paymentTermsDescription: String? = null,
    @Column(name = "payment_terms_due_days", nullable = false)
    var paymentTermsDueDays: Int,
    @Column(name = "payment_terms_discount_percentage", precision = 9, scale = 4)
    var paymentTermsDiscountPercentage: java.math.BigDecimal? = null,
    @Column(name = "payment_terms_discount_days")
    var paymentTermsDiscountDays: Int? = null,
    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0,
) {
    @OneToMany(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var lines: MutableList<CustomerInvoiceLineEntity> = mutableListOf()

    fun toDomain(): CustomerInvoice =
        CustomerInvoice(
            id = CustomerInvoiceId(id),
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            customerId = customerId,
            invoiceNumber = invoiceNumber,
            invoiceDate = invoiceDate,
            dueDate = dueDate,
            currency = currency,
            netAmount = Money(netAmount, currency),
            taxAmount = Money(taxAmount, currency),
            receivedAmount = Money(receivedAmount, currency),
            status = status,
            dimensionAssignments =
                DimensionAssignments(
                    costCenterId = costCenterId,
                    profitCenterId = profitCenterId,
                    departmentId = departmentId,
                    projectId = projectId,
                    businessAreaId = businessAreaId,
                ),
            lines = lines.map { it.toDomain(currency) },
            paymentTerms =
                PaymentTerms(
                    code = paymentTermsCode,
                    description = paymentTermsDescription,
                    type = paymentTermsType,
                    dueInDays = paymentTermsDueDays,
                    discountPercentage = paymentTermsDiscountPercentage,
                    discountDays = paymentTermsDiscountDays,
                ),
            createdAt = createdAt,
            updatedAt = updatedAt,
            postedAt = postedAt,
            journalEntryId = journalEntryId,
        )

    fun updateFrom(domain: CustomerInvoice) {
        id = domain.id.value
        tenantId = domain.tenantId
        companyCodeId = domain.companyCodeId
        customerId = domain.customerId
        invoiceNumber = domain.invoiceNumber
        invoiceDate = domain.invoiceDate
        dueDate = domain.dueDate
        currency = domain.currency
        netAmount = domain.netAmount.amount
        taxAmount = domain.taxAmount.amount
        receivedAmount = domain.receivedAmount.amount
        status = domain.status
        costCenterId = domain.dimensionAssignments.costCenterId
        profitCenterId = domain.dimensionAssignments.profitCenterId
        departmentId = domain.dimensionAssignments.departmentId
        projectId = domain.dimensionAssignments.projectId
        businessAreaId = domain.dimensionAssignments.businessAreaId
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
        postedAt = domain.postedAt
        journalEntryId = domain.journalEntryId
        paymentTermsCode = domain.paymentTerms.code
        paymentTermsType = domain.paymentTerms.type
        paymentTermsDescription = domain.paymentTerms.description
        paymentTermsDueDays = domain.paymentTerms.dueInDays
        paymentTermsDiscountPercentage = domain.paymentTerms.discountPercentage
        paymentTermsDiscountDays = domain.paymentTerms.discountDays
        lines.clear()
        lines.addAll(domain.lines.map { CustomerInvoiceLineEntity.from(it, this) })
    }

    companion object {
        fun from(domain: CustomerInvoice): CustomerInvoiceEntity {
            val entity =
                CustomerInvoiceEntity(
                    id = domain.id.value,
                    tenantId = domain.tenantId,
                    companyCodeId = domain.companyCodeId,
                    customerId = domain.customerId,
                    invoiceNumber = domain.invoiceNumber,
                    invoiceDate = domain.invoiceDate,
                    dueDate = domain.dueDate,
                    currency = domain.currency,
                    netAmount = domain.netAmount.amount,
                    taxAmount = domain.taxAmount.amount,
                    receivedAmount = domain.receivedAmount.amount,
                    status = domain.status,
                    costCenterId = domain.dimensionAssignments.costCenterId,
                    profitCenterId = domain.dimensionAssignments.profitCenterId,
                    departmentId = domain.dimensionAssignments.departmentId,
                    projectId = domain.dimensionAssignments.projectId,
                    businessAreaId = domain.dimensionAssignments.businessAreaId,
                    createdAt = domain.createdAt,
                    updatedAt = domain.updatedAt,
                    postedAt = domain.postedAt,
                    journalEntryId = domain.journalEntryId,
                    paymentTermsCode = domain.paymentTerms.code,
                    paymentTermsType = domain.paymentTerms.type,
                    paymentTermsDescription = domain.paymentTerms.description,
                    paymentTermsDueDays = domain.paymentTerms.dueInDays,
                    paymentTermsDiscountPercentage = domain.paymentTerms.discountPercentage,
                    paymentTermsDiscountDays = domain.paymentTerms.discountDays,
                )
            entity.lines = domain.lines.map { CustomerInvoiceLineEntity.from(it, entity) }.toMutableList()
            return entity
        }
    }
}

@Entity
@Table(name = "ar_invoice_lines", schema = "financial_ar")
class CustomerInvoiceLineEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    var invoice: CustomerInvoiceEntity? = null,
    @Column(name = "gl_account_id", nullable = false)
    var glAccountId: UUID,
    @Column(name = "description", nullable = false, length = 255)
    var description: String,
    @Column(name = "net_amount", nullable = false)
    var netAmount: Long,
    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Long,
    @Column(name = "cost_center_id")
    var costCenterId: UUID? = null,
    @Column(name = "profit_center_id")
    var profitCenterId: UUID? = null,
    @Column(name = "department_id")
    var departmentId: UUID? = null,
    @Column(name = "project_id")
    var projectId: UUID? = null,
    @Column(name = "business_area_id")
    var businessAreaId: UUID? = null,
) {
    fun toDomain(currency: String): CustomerInvoiceLine =
        CustomerInvoiceLine(
            id = id,
            glAccountId = glAccountId,
            description = description,
            netAmount = Money(netAmount, currency),
            taxAmount = Money(taxAmount, currency),
            dimensionAssignments =
                DimensionAssignments(
                    costCenterId = costCenterId,
                    profitCenterId = profitCenterId,
                    departmentId = departmentId,
                    projectId = projectId,
                    businessAreaId = businessAreaId,
                ),
        )

    companion object {
        fun from(
            line: CustomerInvoiceLine,
            parent: CustomerInvoiceEntity,
        ): CustomerInvoiceLineEntity =
            CustomerInvoiceLineEntity(
                id = line.id,
                invoice = parent,
                glAccountId = line.glAccountId,
                description = line.description,
                netAmount = line.netAmount.amount,
                taxAmount = line.taxAmount.amount,
                costCenterId = line.dimensionAssignments.costCenterId,
                profitCenterId = line.dimensionAssignments.profitCenterId,
                departmentId = line.dimensionAssignments.departmentId,
                projectId = line.dimensionAssignments.projectId,
                businessAreaId = line.dimensionAssignments.businessAreaId,
            )
    }
}
