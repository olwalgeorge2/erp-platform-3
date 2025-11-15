package com.erp.financial.ar.infrastructure.adapter.input.rest.dto

import com.erp.financial.ar.application.port.input.query.ListCustomerInvoicesQuery
import com.erp.financial.ar.application.port.input.query.ListCustomersQuery
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
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

data class CustomerListRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:QueryParam("status")
    var status: String? = null,
) {
    fun toQuery(locale: Locale): ListCustomersQuery =
        ListCustomersQuery(
            tenantId =
                tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale),
            companyCodeId = companyCodeId,
            status = status?.let { parseStatus(it, locale) },
        )

    private fun parseStatus(
        raw: String,
        locale: Locale,
    ): com.erp.financial.shared.masterdata.MasterDataStatus =
        runCatching {
            com.erp.financial.shared.masterdata.MasterDataStatus
                .valueOf(raw.uppercase(Locale.getDefault()))
        }.getOrElse {
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
                        com.erp.financial.shared.masterdata.MasterDataStatus.entries
                            .joinToString(),
                    ),
            )
        }
}

data class CustomerScopedRequest(
    @field:NotBlank
    @field:PathParam("customerId")
    var customerId: String? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun tenantId(locale: Locale): UUID =
        tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale)

    fun customerId(locale: Locale): UUID =
        customerId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_CUSTOMER_ID,
                        field = "customerId",
                        rejectedValue = customerId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_CUSTOMER_ID,
                                locale,
                                customerId,
                            ),
                    )
                }
        } ?: missingField("customerId", FinanceValidationErrorCode.FINANCE_INVALID_CUSTOMER_ID, locale)
}

data class CustomerIdPathParams(
    @field:NotBlank
    @field:PathParam("customerId")
    var customerId: String? = null,
) {
    fun customerId(locale: Locale): UUID =
        customerId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_CUSTOMER_ID,
                        field = "customerId",
                        rejectedValue = customerId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_CUSTOMER_ID,
                                locale,
                                customerId,
                            ),
                    )
                }
        } ?: missingField("customerId", FinanceValidationErrorCode.FINANCE_INVALID_CUSTOMER_ID, locale)
}

data class CustomerInvoiceListRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:QueryParam("customerId")
    var customerId: UUID? = null,
    @field:QueryParam("status")
    var status: String? = null,
    @field:QueryParam("dueBefore")
    var dueBefore: LocalDate? = null,
) {
    fun toQuery(locale: Locale): ListCustomerInvoicesQuery =
        ListCustomerInvoicesQuery(
            tenantId =
                tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale),
            companyCodeId = companyCodeId,
            customerId = customerId,
            status = status?.let { parseStatus(it, locale) },
            dueBefore = dueBefore,
        )

    private fun parseStatus(
        raw: String,
        locale: Locale,
    ): CustomerInvoiceStatus =
        runCatching { CustomerInvoiceStatus.valueOf(raw.uppercase(Locale.getDefault())) }
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
                            CustomerInvoiceStatus.entries.joinToString(),
                        ),
                )
            }
}

data class CustomerInvoiceScopedRequest(
    @field:NotBlank
    @field:PathParam("invoiceId")
    var invoiceId: String? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun tenantId(locale: Locale): UUID =
        tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale)

    fun invoiceId(locale: Locale): UUID =
        invoiceId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                        field = "invoiceId",
                        rejectedValue = invoiceId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                                locale,
                                invoiceId,
                            ),
                    )
                }
        } ?: missingField("invoiceId", FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID, locale)
}

data class CustomerInvoicePathParams(
    @field:NotBlank
    @field:PathParam("invoiceId")
    var invoiceId: String? = null,
) {
    fun invoiceId(locale: Locale): UUID =
        invoiceId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                        field = "invoiceId",
                        rejectedValue = invoiceId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID,
                                locale,
                                invoiceId,
                            ),
                    )
                }
        } ?: missingField("invoiceId", FinanceValidationErrorCode.FINANCE_INVALID_INVOICE_ID, locale)
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
