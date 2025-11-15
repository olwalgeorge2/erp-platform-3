package com.erp.financial.ar.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ar.application.port.input.command.CreateCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.PostCustomerInvoiceCommand
import com.erp.financial.ar.application.port.input.command.RecordCustomerReceiptCommand
import com.erp.financial.ar.application.port.input.query.ListCustomerInvoicesQuery
import com.erp.financial.ar.domain.model.invoice.CustomerInvoice
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import com.erp.financial.shared.masterdata.PaymentTermType
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.ValidationMessageResolver
import com.erp.financial.shared.validation.sanitizeCurrencyCode
import com.erp.financial.shared.validation.sanitizeReferenceNumber
import com.erp.financial.shared.validation.sanitizeText
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class CreateCustomerInvoiceRequest(
    @field:NotNull val tenantId: UUID,
    @field:NotNull val companyCodeId: UUID,
    @field:NotNull val customerId: UUID,
    @field:NotBlank val invoiceNumber: String,
    @field:NotNull val invoiceDate: LocalDate,
    @field:NotNull val dueDate: LocalDate,
    @field:NotBlank val currency: String,
    @field:Valid val dimensionDefaults: InvoiceDimensionRequest? = null,
    @field:NotEmpty @field:Valid val lines: List<CustomerInvoiceLineRequest>,
)

data class CustomerInvoiceLineRequest(
    @field:NotNull val glAccountId: UUID,
    @field:NotBlank val description: String,
    @field:Positive val netAmount: Long,
    @field:PositiveOrZero val taxAmount: Long = 0,
    @field:Valid val dimensionOverrides: InvoiceDimensionRequest? = null,
)

data class CustomerInvoiceResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val netAmount: Long,
    val taxAmount: Long,
    val receivedAmount: Long,
    val status: CustomerInvoiceStatus,
    val dimensionDefaults: InvoiceDimensionResponse,
    val lines: List<CustomerInvoiceLineResponse>,
    val paymentTerms: PaymentTermsDto,
    val createdAt: Instant,
    val updatedAt: Instant,
    val postedAt: Instant?,
    val journalEntryId: UUID?,
)

data class CustomerInvoiceLineResponse(
    val id: UUID,
    val glAccountId: UUID,
    val description: String,
    val netAmount: Long,
    val taxAmount: Long,
    val dimensionAssignments: InvoiceDimensionResponse,
)

data class PostCustomerInvoiceRequest(
    @field:NotNull val tenantId: UUID,
)

data class ReceiptRequest(
    @field:NotNull val tenantId: UUID,
    @field:Positive val amount: Long,
    val receiptDate: LocalDate? = null,
)

data class CustomerInvoiceSearchRequest(
    @field:NotNull val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val customerId: UUID? = null,
    val status: CustomerInvoiceStatus? = null,
    val dueBefore: LocalDate? = null,
)

data class PaymentTermsDto(
    val code: String,
    val type: PaymentTermType,
    val dueInDays: Int,
    val discountPercentage: BigDecimal? = null,
    val discountDays: Int? = null,
)

data class InvoiceDimensionRequest(
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
)

data class InvoiceDimensionResponse(
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
)

private fun CustomerInvoiceLineRequest.toCommandLine(
    index: Int,
    locale: Locale,
): CreateCustomerInvoiceCommand.Line =
    CreateCustomerInvoiceCommand.Line(
        glAccountId = glAccountId,
        description = requireNotBlank(description.sanitizeText(200), "lines[$index].description", locale),
        netAmount = requirePositive(netAmount, "lines[$index].netAmount", locale),
        taxAmount = requireNonNegative(taxAmount, "lines[$index].taxAmount", locale),
        dimensionAssignments = dimensionOverrides.toAssignments(),
    )

fun CreateCustomerInvoiceRequest.toCommand(locale: Locale): CreateCustomerInvoiceCommand =
    CreateCustomerInvoiceCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerId = customerId,
        invoiceNumber = requireNotBlank(invoiceNumber.sanitizeReferenceNumber(), "invoiceNumber", locale),
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = normalizeCurrency(currency.sanitizeCurrencyCode(), "currency", locale),
        lines = lines.mapIndexed { index, line -> line.toCommandLine(index, locale) },
        dimensionAssignments = dimensionDefaults.toAssignments(),
    )

fun PostCustomerInvoiceRequest.toCommand(invoiceId: UUID): PostCustomerInvoiceCommand =
    PostCustomerInvoiceCommand(
        tenantId = tenantId,
        invoiceId = invoiceId,
    )

fun ReceiptRequest.toCommand(
    invoiceId: UUID,
    locale: Locale,
): RecordCustomerReceiptCommand =
    RecordCustomerReceiptCommand(
        tenantId = tenantId,
        invoiceId = invoiceId,
        receiptAmount = requirePositive(amount, "amount", locale),
        receiptDate = receiptDate,
    )

fun CustomerInvoiceSearchRequest.toQuery(): ListCustomerInvoicesQuery =
    ListCustomerInvoicesQuery(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerId = customerId,
        status = status,
        dueBefore = dueBefore,
    )

fun CustomerInvoice.toResponse(): CustomerInvoiceResponse =
    CustomerInvoiceResponse(
        id = id.value,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerId = customerId,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = currency,
        netAmount = netAmount.amount,
        taxAmount = taxAmount.amount,
        receivedAmount = receivedAmount.amount,
        status = status,
        dimensionDefaults =
            InvoiceDimensionResponse(
                costCenterId = dimensionAssignments.costCenterId,
                profitCenterId = dimensionAssignments.profitCenterId,
                departmentId = dimensionAssignments.departmentId,
                projectId = dimensionAssignments.projectId,
                businessAreaId = dimensionAssignments.businessAreaId,
            ),
        lines =
            lines.map {
                CustomerInvoiceLineResponse(
                    id = it.id,
                    glAccountId = it.glAccountId,
                    description = it.description,
                    netAmount = it.netAmount.amount,
                    taxAmount = it.taxAmount.amount,
                    dimensionAssignments =
                        InvoiceDimensionResponse(
                            costCenterId = it.dimensionAssignments.costCenterId,
                            profitCenterId = it.dimensionAssignments.profitCenterId,
                            departmentId = it.dimensionAssignments.departmentId,
                            projectId = it.dimensionAssignments.projectId,
                            businessAreaId = it.dimensionAssignments.businessAreaId,
                        ),
                )
            },
        paymentTerms =
            PaymentTermsDto(
                code = paymentTerms.code,
                type = paymentTerms.type,
                dueInDays = paymentTerms.dueInDays,
                discountPercentage = paymentTerms.discountPercentage,
                discountDays = paymentTerms.discountDays,
            ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        postedAt = postedAt,
        journalEntryId = journalEntryId,
    )

private fun InvoiceDimensionRequest?.toAssignments(): DimensionAssignments =
    this?.let {
        DimensionAssignments(
            costCenterId = it.costCenterId,
            profitCenterId = it.profitCenterId,
            departmentId = it.departmentId,
            projectId = it.projectId,
            businessAreaId = it.businessAreaId,
        )
    } ?: DimensionAssignments()

private fun normalizeCurrency(
    value: String,
    field: String,
    locale: Locale,
): String {
    val normalized = value.trim().uppercase(Locale.getDefault())
    if (normalized.length != 3) {
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_CURRENCY_CODE,
            field = field,
            rejectedValue = value,
            locale = locale,
            message =
                ValidationMessageResolver.resolve(
                    FinanceValidationErrorCode.FINANCE_INVALID_CURRENCY_CODE,
                    locale,
                    value,
                ),
        )
    }
    return normalized
}

private fun requirePositive(
    value: Long,
    field: String,
    locale: Locale,
): Long {
    if (value <= 0) {
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_AMOUNT,
            field = field,
            rejectedValue = value.toString(),
            locale = locale,
            message = ValidationMessageResolver.resolve(FinanceValidationErrorCode.FINANCE_INVALID_AMOUNT, locale, field),
        )
    }
    return value
}

private fun requireNonNegative(
    value: Long,
    field: String,
    locale: Locale,
): Long {
    if (value < 0) {
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_AMOUNT,
            field = field,
            rejectedValue = value.toString(),
            locale = locale,
            message = ValidationMessageResolver.resolve(FinanceValidationErrorCode.FINANCE_INVALID_AMOUNT, locale, field),
        )
    }
    return value
}

private fun requireNotBlank(
    value: String?,
    field: String,
    locale: Locale,
): String {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty()) {
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_NAME,
            field = field,
            rejectedValue = value,
            locale = locale,
            message = ValidationMessageResolver.resolve(FinanceValidationErrorCode.FINANCE_INVALID_NAME, locale, field),
        )
    }
    return trimmed
}
