package com.erp.financial.ap.infrastructure.adapter.input.rest.dto

import com.erp.financial.ap.application.port.input.command.GeneratePaymentProposalCommand
import com.erp.financial.ap.application.port.output.ListPaymentProposalsQuery
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalItem
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalStatus
import java.time.LocalDate
import java.util.UUID

data class GeneratePaymentProposalRequest(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val asOfDate: LocalDate,
    val paymentDate: LocalDate,
    val vendorIds: Set<UUID>? = null,
    val includeDiscountEligible: Boolean = true,
)

data class PaymentProposalResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val currency: String,
    val proposalDate: LocalDate,
    val paymentDate: LocalDate,
    val status: PaymentProposalStatus,
    val totalAmountMinor: Long,
    val discountAmountMinor: Long,
    val items: List<PaymentProposalItemResponse>,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant,
)

data class PaymentProposalItemResponse(
    val id: UUID,
    val openItemId: UUID,
    val invoiceId: UUID,
    val vendorId: UUID,
    val amountToPayMinor: Long,
    val discountMinor: Long,
    val bucket: String,
    val currency: String,
    val dueDate: LocalDate,
)

data class PaymentProposalSearchRequest(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: PaymentProposalStatus? = null,
)

fun GeneratePaymentProposalRequest.toCommand(): GeneratePaymentProposalCommand =
    GeneratePaymentProposalCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        asOfDate = asOfDate,
        paymentDate = paymentDate,
        vendorIds = vendorIds,
        includeDiscountEligible = includeDiscountEligible,
    )

fun PaymentProposalItem.toResponse(): PaymentProposalItemResponse =
    PaymentProposalItemResponse(
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

fun PaymentProposal.toResponse(): PaymentProposalResponse =
    PaymentProposalResponse(
        id = id,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        currency = currency,
        proposalDate = proposalDate,
        paymentDate = paymentDate,
        status = status,
        totalAmountMinor = totalAmountMinor,
        discountAmountMinor = discountAmountMinor,
        items = items.map(PaymentProposalItem::toResponse),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PaymentProposalSearchRequest.toQuery(): ListPaymentProposalsQuery =
    ListPaymentProposalsQuery(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        status = status,
    )
