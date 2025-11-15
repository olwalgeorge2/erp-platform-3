package com.erp.financial.ap.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ap.application.port.input.command.CreateVendorBillCommand
import com.erp.financial.ap.application.port.input.command.PostVendorBillCommand
import com.erp.financial.ap.application.port.input.command.RecordVendorPaymentCommand
import com.erp.financial.ap.application.port.input.query.ListVendorBillsQuery
import com.erp.financial.ap.domain.model.bill.BillStatus
import com.erp.financial.ap.domain.model.bill.VendorBill
import com.erp.financial.shared.masterdata.PaymentTermType
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.InputSanitizer.sanitizeCurrencyCode
import com.erp.financial.shared.validation.InputSanitizer.sanitizeReferenceNumber
import com.erp.financial.shared.validation.InputSanitizer.sanitizeText
import com.erp.financial.shared.validation.ValidationMessageResolver
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class CreateBillRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val companyCodeId: UUID,
    @field:NotNull
    val vendorId: UUID,
    @field:NotBlank
    val invoiceNumber: String,
    @field:NotNull
    val invoiceDate: LocalDate,
    @field:NotNull
    val dueDate: LocalDate,
    @field:NotBlank
    val currency: String,
    @field:Valid
    val dimensionDefaults: DimensionDefaultsRequest? = null,
    @field:NotEmpty
    @field:Valid
    val lines: List<BillLineRequest>,
)

data class BillLineRequest(
    @field:NotNull
    val glAccountId: UUID,
    @field:NotBlank
    val description: String,
    val netAmount: Long,
    val taxAmount: Long = 0,
    @field:Valid
    val dimensionOverrides: DimensionDefaultsRequest? = null,
)

data class BillResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorId: UUID,
    val invoiceNumber: String,
    val invoiceDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val netAmount: Long,
    val taxAmount: Long,
    val paidAmount: Long,
    val status: BillStatus,
    val dimensionDefaults: DimensionDefaultsRequest,
    val lines: List<BillLineResponse>,
    val paymentTerms: PaymentTermsDto,
    val createdAt: Instant,
    val updatedAt: Instant,
    val postedAt: Instant?,
    val journalEntryId: UUID?,
)

data class BillLineResponse(
    val id: UUID,
    val glAccountId: UUID,
    val description: String,
    val netAmount: Long,
    val taxAmount: Long,
    val dimensionAssignments: DimensionDefaultsRequest,
)

data class PostBillRequest(
    @field:NotNull
    val tenantId: UUID,
)

data class PaymentRequest(
    @field:NotNull
    val tenantId: UUID,
    val amount: Long,
    val paymentDate: LocalDate? = null,
)

data class BillSearchRequest(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val vendorId: UUID? = null,
    val status: BillStatus? = null,
    val dueBefore: LocalDate? = null,
)

data class BillListRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:QueryParam("vendorId")
    var vendorId: UUID? = null,
    @field:QueryParam("status")
    var status: String? = null,
    @field:QueryParam("dueBefore")
    var dueBefore: LocalDate? = null,
) {
    fun toQuery(locale: Locale): ListVendorBillsQuery =
        ListVendorBillsQuery(
            tenantId =
                tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale),
            companyCodeId = companyCodeId,
            vendorId = vendorId,
            status = status?.let { parseStatus(it, locale) },
            dueBefore = dueBefore,
        )

    private fun parseStatus(
        raw: String,
        locale: Locale,
    ): BillStatus =
        runCatching { BillStatus.valueOf(raw.uppercase(Locale.getDefault())) }
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
                            BillStatus.entries.joinToString(),
                        ),
                )
            }
}

data class BillScopedRequest(
    @field:NotBlank
    @field:PathParam("billId")
    var billId: String? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun tenantId(locale: Locale): UUID =
        tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale)

    fun billId(locale: Locale): UUID =
        billId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                        field = "billId",
                        rejectedValue = billId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                                locale,
                                billId,
                            ),
                    )
                }
        } ?: missingField("billId", FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID, locale)
}

data class BillPathParams(
    @field:NotBlank
    @field:PathParam("billId")
    var billId: String? = null,
) {
    fun billId(locale: Locale): UUID =
        billId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                        field = "billId",
                        rejectedValue = billId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                                locale,
                                billId,
                            ),
                    )
                }
        } ?: missingField("billId", FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID, locale)
}

fun CreateBillRequest.toCommand(locale: Locale): CreateVendorBillCommand =
    CreateVendorBillCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorId = vendorId,
        invoiceNumber = requireNotBlank(invoiceNumber.sanitizeReferenceNumber(), "invoiceNumber", locale),
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = normalizeCurrency(currency.sanitizeCurrencyCode(), "currency", locale),
        lines = lines.mapIndexed { index, line -> line.toCommandLine(index, locale) },
        dimensionAssignments = dimensionDefaults.toAssignments(),
    )

fun PostBillRequest.toCommand(billId: UUID): PostVendorBillCommand =
    PostVendorBillCommand(
        tenantId = tenantId,
        billId = billId,
    )

fun PaymentRequest.toCommand(
    billId: UUID,
    locale: Locale,
): RecordVendorPaymentCommand =
    RecordVendorPaymentCommand(
        tenantId = tenantId,
        billId = billId,
        paymentAmount = requirePositive(amount, "amount", locale),
        paymentDate = paymentDate,
    )

fun BillSearchRequest.toQuery(): ListVendorBillsQuery =
    ListVendorBillsQuery(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorId = vendorId,
        status = status,
        dueBefore = dueBefore,
    )

fun VendorBill.toResponse(): BillResponse =
    BillResponse(
        id = id.value,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorId = vendorId,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        dueDate = dueDate,
        currency = currency,
        netAmount = netAmount.amount,
        taxAmount = taxAmount.amount,
        paidAmount = paidAmount.amount,
        status = status,
        dimensionDefaults =
            DimensionDefaultsRequest(
                costCenterId = dimensionAssignments.costCenterId,
                profitCenterId = dimensionAssignments.profitCenterId,
                departmentId = dimensionAssignments.departmentId,
                projectId = dimensionAssignments.projectId,
                businessAreaId = dimensionAssignments.businessAreaId,
            ),
        lines =
            lines.map {
                BillLineResponse(
                    id = it.id,
                    glAccountId = it.glAccountId,
                    description = it.description,
                    netAmount = it.netAmount.amount,
                    taxAmount = it.taxAmount.amount,
                    dimensionAssignments =
                        DimensionDefaultsRequest(
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

private fun BillLineRequest.toCommandLine(
    index: Int,
    locale: Locale,
): CreateVendorBillCommand.Line =
    CreateVendorBillCommand.Line(
        glAccountId = glAccountId,
        description = requireNotBlank(description.sanitizeText(200), "lines[$index].description", locale),
        netAmount = requirePositive(netAmount, "lines[$index].netAmount", locale),
        taxAmount = requireNonNegative(taxAmount, "lines[$index].taxAmount", locale),
        dimensionAssignments = dimensionOverrides.toAssignments(),
    )

private fun DimensionDefaultsRequest?.toAssignments(): DimensionAssignments =
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
            message =
                ValidationMessageResolver.resolve(
                    FinanceValidationErrorCode.FINANCE_INVALID_AMOUNT,
                    locale,
                    field,
                ),
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
            message =
                ValidationMessageResolver.resolve(
                    FinanceValidationErrorCode.FINANCE_INVALID_AMOUNT,
                    locale,
                    field,
                ),
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

data class PaymentTermsDto(
    val code: String,
    val type: PaymentTermType,
    val dueInDays: Int,
    val discountPercentage: BigDecimal? = null,
    val discountDays: Int? = null,
)

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
