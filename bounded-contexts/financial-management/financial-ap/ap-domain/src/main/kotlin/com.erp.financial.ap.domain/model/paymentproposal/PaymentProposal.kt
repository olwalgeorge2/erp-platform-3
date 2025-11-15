package com.erp.financial.ap.domain.model.paymentproposal

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PaymentProposal(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val currency: String,
    val proposalDate: LocalDate,
    val paymentDate: LocalDate,
    val status: PaymentProposalStatus,
    val totalAmountMinor: Long,
    val discountAmountMinor: Long,
    val items: List<PaymentProposalItem>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(currency.length == 3) { "Currency must be ISO-4217" }
        require(items.isNotEmpty()) { "Payment proposal must contain at least one item" }
        require(totalAmountMinor > 0) { "Payment proposal total must be positive" }
    }

    fun approve(clock: Clock = Clock.systemUTC()): PaymentProposal =
        when (status) {
            PaymentProposalStatus.DRAFT ->
                copy(status = PaymentProposalStatus.APPROVED, updatedAt = Instant.now(clock))
            else -> this
        }

    fun cancel(clock: Clock = Clock.systemUTC()): PaymentProposal =
        when (status) {
            PaymentProposalStatus.SENT -> error("Sent proposals cannot be canceled")
            else -> copy(status = PaymentProposalStatus.CANCELED, updatedAt = Instant.now(clock))
        }

    companion object {
        fun draft(
            tenantId: UUID,
            companyCodeId: UUID,
            currency: String,
            proposalDate: LocalDate,
            paymentDate: LocalDate,
            items: List<PaymentProposalItem>,
            discountAmountMinor: Long,
            clock: Clock = Clock.systemUTC(),
        ): PaymentProposal {
            val now = Instant.now(clock)
            val totalAmount = items.sumOf { it.amountToPayMinor }
            require(totalAmount > 0) { "Proposal amount must be positive" }
            return PaymentProposal(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                companyCodeId = companyCodeId,
                currency = currency,
                proposalDate = proposalDate,
                paymentDate = paymentDate,
                status = PaymentProposalStatus.DRAFT,
                totalAmountMinor = totalAmount,
                discountAmountMinor = discountAmountMinor,
                items = items,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

data class PaymentProposalItem(
    val id: UUID = UUID.randomUUID(),
    val openItemId: UUID,
    val invoiceId: UUID,
    val vendorId: UUID,
    val amountToPayMinor: Long,
    val discountMinor: Long,
    val bucket: String,
    val currency: String,
    val dueDate: LocalDate,
)
