package com.erp.financial.ap.infrastructure.adapter.input.rest.dto

import com.erp.financial.ap.application.port.input.command.GeneratePaymentProposalCommand
import com.erp.financial.ap.application.port.output.ListPaymentProposalsQuery
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposal
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalItem
import com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalStatus
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.ValidationMessageResolver
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class GeneratePaymentProposalRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val companyCodeId: UUID,
    @field:NotNull
    val asOfDate: LocalDate,
    @field:NotNull
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

fun GeneratePaymentProposalRequest.toCommand(locale: Locale): GeneratePaymentProposalCommand {
    if (paymentDate.isBefore(asOfDate)) {
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_DATE,
            field = "paymentDate",
            rejectedValue = paymentDate.toString(),
            locale = locale,
            message =
                ValidationMessageResolver.resolve(
                    FinanceValidationErrorCode.FINANCE_INVALID_DATE,
                    locale,
                    paymentDate,
                    "must be on or after asOfDate",
                ),
        )
    }
    return GeneratePaymentProposalCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        asOfDate = asOfDate,
        paymentDate = paymentDate,
        vendorIds = vendorIds,
        includeDiscountEligible = includeDiscountEligible,
    )
}

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

data class PaymentProposalListRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:QueryParam("status")
    var status: String? = null,
) {
    fun toQuery(locale: Locale): ListPaymentProposalsQuery =
        PaymentProposalSearchRequest(
            tenantId =
                tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale),
            companyCodeId = companyCodeId,
            status = status?.let { parseStatus(it, locale) },
        ).toQuery()

    private fun parseStatus(
        raw: String,
        locale: Locale,
    ): PaymentProposalStatus =
        runCatching { PaymentProposalStatus.valueOf(raw) }
            .getOrElse {
                throw FinanceValidationException(
                    errorCode = FinanceValidationErrorCode.FINANCE_INVALID_STATUS,
                    field = "status",
                    rejectedValue = raw,
                    locale = locale,
                    message =
                        ValidationMessageResolver.resolve(
                            FinanceValidationErrorCode.FINANCE_INVALID_STATUS,
                            locale,
                            raw,
                            PaymentProposalStatus.entries.joinToString(),
                        ),
                )
            }
}

data class PaymentProposalDetailRequest(
    @field:NotBlank
    @field:PathParam("proposalId")
    var proposalId: String? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun tenantId(locale: Locale): UUID =
        tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale)

    fun proposalId(locale: Locale): UUID =
        proposalId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_PAYMENT_PROPOSAL_ID,
                        field = "proposalId",
                        rejectedValue = proposalId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_PAYMENT_PROPOSAL_ID,
                                locale,
                                proposalId,
                            ),
                    )
                }
        } ?: missingField("proposalId", FinanceValidationErrorCode.FINANCE_INVALID_PAYMENT_PROPOSAL_ID, locale)
}

private fun missingField(
    field: String,
    code: FinanceValidationErrorCode,
    locale: Locale,
): Nothing =
    throw FinanceValidationException(
        errorCode = code,
        field = field,
        rejectedValue = null,
        locale = locale,
        message = ValidationMessageResolver.resolve(code, locale, "<missing>"),
    )
