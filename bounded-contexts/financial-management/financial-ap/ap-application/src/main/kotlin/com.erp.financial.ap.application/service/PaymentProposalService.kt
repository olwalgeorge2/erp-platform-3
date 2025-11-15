package com.erp.financial.ap.application.service

import com.erp.financial.ap.application.port.input.PaymentProposalUseCase
import com.erp.financial.ap.application.port.input.command.GeneratePaymentProposalCommand
import com.erp.financial.ap.application.port.input.query.AgingBucket
import com.erp.financial.ap.application.port.output.ApOpenItemFilter
import com.erp.financial.ap.application.port.output.ApOpenItemRepository
import com.erp.financial.ap.application.port.output.ListPaymentProposalsQuery
import com.erp.financial.ap.application.port.output.PaymentProposalRepository
import com.erp.financial.ap.domain.model.openitem.ApOpenItemStatus
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalItem
import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@ApplicationScoped
class PaymentProposalService(
    private val openItemRepository: ApOpenItemRepository,
    private val proposalRepository: PaymentProposalRepository,
    meterRegistry: MeterRegistry,
) : PaymentProposalUseCase {
    private val generatedCounter = meterRegistry.counter("ap.payment_proposal.generated.total")

    override fun generateProposal(command: GeneratePaymentProposalCommand): PaymentProposal {
        val filter =
            ApOpenItemFilter(
                tenantId = command.tenantId,
                companyCodeId = command.companyCodeId,
                vendorId = null,
                statuses = setOf(ApOpenItemStatus.OPEN, ApOpenItemStatus.PARTIALLY_PAID),
                dueBefore = command.asOfDate,
            )
        val candidates =
            openItemRepository
                .list(filter)
                .filter { item ->
                    item.outstandingAmountMinor > 0 &&
                        (command.vendorIds == null || command.vendorIds.contains(item.vendorId)) &&
                        (
                            item.dueDate <= command.asOfDate ||
                                (
                                    command.includeDiscountEligible &&
                                        item.cashDiscountDueDate != null &&
                                        !command.paymentDate.isAfter(item.cashDiscountDueDate)
                                )
                        )
                }
        if (candidates.isEmpty()) {
            throw IllegalStateException("No eligible open items for payment proposal")
        }
        val currency = ensureSingleCurrency(candidates.map { it.currency })
        val proposalItems =
            candidates.map { item ->
                val discountPercentage = item.paymentTermsDiscountPercentage
                val discount =
                    if (
                        command.includeDiscountEligible &&
                        item.cashDiscountDueDate != null &&
                        !command.paymentDate.isAfter(item.cashDiscountDueDate) &&
                        discountPercentage != null
                    ) {
                        calculateDiscount(item.outstandingAmountMinor, discountPercentage)
                    } else {
                        0L
                    }
                PaymentProposalItem(
                    openItemId = item.id,
                    invoiceId = item.invoiceId,
                    vendorId = item.vendorId,
                    amountToPayMinor = item.outstandingAmountMinor,
                    discountMinor = discount,
                    bucket = determineBucket(item.dueDate, command.asOfDate).name,
                    currency = item.currency,
                    dueDate = item.dueDate,
                )
            }
        val totalDiscount = proposalItems.sumOf { it.discountMinor }
        val proposal =
            PaymentProposal.draft(
                tenantId = command.tenantId,
                companyCodeId = command.companyCodeId,
                currency = currency,
                proposalDate = command.asOfDate,
                paymentDate = command.paymentDate,
                items = proposalItems,
                discountAmountMinor = totalDiscount,
            )
        val saved = proposalRepository.save(proposal)
        openItemRepository.assignToProposal(
            tenantId = command.tenantId,
            openItemIds = proposalItems.map(PaymentProposalItem::openItemId),
            proposalId = saved.id,
        )
        generatedCounter.increment()
        return saved
    }

    override fun listProposals(query: ListPaymentProposalsQuery): List<PaymentProposal> = proposalRepository.list(query)

    override fun getProposal(
        tenantId: UUID,
        proposalId: UUID,
    ): PaymentProposal? = proposalRepository.findById(tenantId, proposalId)

    private fun ensureSingleCurrency(currencies: List<String>): String {
        val distinct = currencies.distinct()
        require(distinct.size == 1) { "Mixed currencies are not supported in a single payment proposal: $distinct" }
        return distinct.first()
    }

    private fun calculateDiscount(
        amountMinor: Long,
        percentage: BigDecimal,
    ): Long =
        BigDecimal(amountMinor)
            .multiply(percentage)
            .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP)
            .longValueExact()

    private fun determineBucket(
        dueDate: LocalDate,
        asOfDate: LocalDate,
    ): AgingBucket {
        val days = ChronoUnit.DAYS.between(dueDate, asOfDate)
        return when {
            days <= 0 -> AgingBucket.CURRENT
            days <= 30 -> AgingBucket.DAYS_1_30
            days <= 60 -> AgingBucket.DAYS_31_60
            days <= 90 -> AgingBucket.DAYS_61_90
            else -> AgingBucket.DAYS_91_PLUS
        }
    }
}
