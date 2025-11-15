package com.erp.financial.ap.infrastructure.persistence.entity

import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalItem
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalStatus
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
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "payment_proposals", schema = "financial_ap")
class PaymentProposalEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "proposal_date", nullable = false)
    var proposalDate: LocalDate,
    @Column(name = "payment_date", nullable = false)
    var paymentDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: PaymentProposalStatus = PaymentProposalStatus.DRAFT,
    @Column(name = "total_amount_minor", nullable = false)
    var totalAmountMinor: Long,
    @Column(name = "discount_amount_minor", nullable = false)
    var discountAmountMinor: Long,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @OneToMany(mappedBy = "proposal", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var items: MutableList<PaymentProposalItemEntity> = mutableListOf()

    fun toDomain(): PaymentProposal =
        PaymentProposal(
            id = id,
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            currency = currency,
            proposalDate = proposalDate,
            paymentDate = paymentDate,
            status = status,
            totalAmountMinor = totalAmountMinor,
            discountAmountMinor = discountAmountMinor,
            items = items.map(PaymentProposalItemEntity::toDomain),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: PaymentProposal) {
        id = domain.id
        tenantId = domain.tenantId
        companyCodeId = domain.companyCodeId
        currency = domain.currency
        proposalDate = domain.proposalDate
        paymentDate = domain.paymentDate
        status = domain.status
        totalAmountMinor = domain.totalAmountMinor
        discountAmountMinor = domain.discountAmountMinor
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
        items.clear()
        items.addAll(domain.items.map { PaymentProposalItemEntity.from(it, this) })
    }

    companion object {
        fun from(domain: PaymentProposal): PaymentProposalEntity {
            val entity =
                PaymentProposalEntity(
                    id = domain.id,
                    tenantId = domain.tenantId,
                    companyCodeId = domain.companyCodeId,
                    currency = domain.currency,
                    proposalDate = domain.proposalDate,
                    paymentDate = domain.paymentDate,
                    status = domain.status,
                    totalAmountMinor = domain.totalAmountMinor,
                    discountAmountMinor = domain.discountAmountMinor,
                    createdAt = domain.createdAt,
                    updatedAt = domain.updatedAt,
                )
            entity.items = domain.items.map { PaymentProposalItemEntity.from(it, entity) }.toMutableList()
            return entity
        }
    }
}

@Entity
@Table(name = "payment_proposal_items", schema = "financial_ap")
class PaymentProposalItemEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    var proposal: PaymentProposalEntity? = null,
    @Column(name = "open_item_id", nullable = false)
    var openItemId: UUID,
    @Column(name = "invoice_id", nullable = false)
    var invoiceId: UUID,
    @Column(name = "vendor_id", nullable = false)
    var vendorId: UUID,
    @Column(name = "amount_to_pay_minor", nullable = false)
    var amountToPayMinor: Long,
    @Column(name = "discount_minor", nullable = false)
    var discountMinor: Long,
    @Column(name = "bucket", nullable = false, length = 32)
    var bucket: String,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "due_date", nullable = false)
    var dueDate: LocalDate,
) {
    fun toDomain(): PaymentProposalItem =
        PaymentProposalItem(
            id = id,
            openItemId = openItemId,
            invoiceId = invoiceId,
            vendorId = vendorId,
            amountToPayMinor = amountToPayMinor,
            discountMinor = discountMinor,
            bucket = bucket,
            currency = currency,
            dueDate = dueDate,
        )

    companion object {
        fun from(
            item: PaymentProposalItem,
            proposal: PaymentProposalEntity,
        ): PaymentProposalItemEntity =
            PaymentProposalItemEntity(
                id = item.id,
                proposal = proposal,
                openItemId = item.openItemId,
                invoiceId = item.invoiceId,
                vendorId = item.vendorId,
                amountToPayMinor = item.amountToPayMinor,
                discountMinor = item.discountMinor,
                bucket = item.bucket,
                currency = item.currency,
                dueDate = item.dueDate,
            )
    }
}
