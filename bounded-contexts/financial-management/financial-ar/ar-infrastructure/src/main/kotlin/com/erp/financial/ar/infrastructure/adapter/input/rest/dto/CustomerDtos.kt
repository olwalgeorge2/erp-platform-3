package com.erp.financial.ar.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ar.application.port.input.command.RegisterCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerCommand
import com.erp.financial.ar.application.port.input.command.UpdateCustomerStatusCommand
import com.erp.financial.ar.application.port.input.query.ListCustomersQuery
import com.erp.financial.ar.domain.model.customer.Customer
import com.erp.financial.ar.domain.model.customer.CustomerNumber
import com.erp.financial.ar.domain.model.customer.CustomerProfile
import com.erp.financial.shared.Money
import com.erp.financial.shared.masterdata.Address
import com.erp.financial.shared.masterdata.ContactPerson
import com.erp.financial.shared.masterdata.MasterDataStatus
import com.erp.financial.shared.masterdata.PaymentTermType
import com.erp.financial.shared.masterdata.PaymentTerms
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.ValidationMessageResolver
import com.erp.financial.shared.validation.sanitizeAccountCode
import com.erp.financial.shared.validation.sanitizeCurrencyCode
import com.erp.financial.shared.validation.sanitizeEmail
import com.erp.financial.shared.validation.sanitizeName
import com.erp.financial.shared.validation.sanitizePhoneNumber
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

data class CustomerRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val companyCodeId: UUID,
    @field:NotBlank
    val customerNumber: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val currency: String,
    @field:Valid
    val paymentTerms: PaymentTermsRequest,
    @field:Valid
    val billingAddress: AddressRequest,
    @field:Valid
    val shippingAddress: AddressRequest? = null,
    @field:Valid
    val contact: ContactRequest? = null,
    @field:Valid
    val creditLimit: CreditLimitRequest? = null,
    @field:Valid
    val dimensionDefaults: DimensionDefaultsRequest? = null,
)

data class PaymentTermsRequest(
    @field:NotBlank
    val code: String,
    @field:NotNull
    val type: PaymentTermType,
    val dueInDays: Int,
    val discountPercentage: BigDecimal? = null,
    val discountDays: Int? = null,
)

data class AddressRequest(
    @field:NotBlank
    val line1: String,
    val line2: String? = null,
    @field:NotBlank
    val city: String,
    val stateOrProvince: String? = null,
    val postalCode: String? = null,
    @field:NotBlank
    val countryCode: String,
)

data class ContactRequest(
    @field:NotBlank
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
)

data class CreditLimitRequest(
    @field:NotNull
    val amount: BigDecimal,
    @field:NotBlank
    val currency: String,
)

data class DimensionDefaultsRequest(
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
)

data class CustomerResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerNumber: String,
    val name: String,
    val status: MasterDataStatus,
    val currency: String,
    val paymentTerms: PaymentTermsRequest,
    val billingAddress: AddressRequest,
    val shippingAddress: AddressRequest?,
    val contact: ContactRequest?,
    val creditLimit: CreditLimitRequest?,
    val dimensionDefaults: DimensionDefaultsRequest,
)

data class CustomerStatusRequest(
    @field:NotNull val tenantId: UUID,
    @field:NotNull val status: MasterDataStatus,
)

data class CustomerSearchRequest(
    @field:NotNull val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: MasterDataStatus? = null,
)

fun CustomerRequest.toRegisterCommand(locale: Locale): RegisterCustomerCommand =
    RegisterCustomerCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerNumber = CustomerNumber(requireNotBlank(customerNumber.sanitizeAccountCode(), "customerNumber", FinanceValidationErrorCode.FINANCE_INVALID_CUSTOMER_NUMBER, locale)),
        profile = toProfile(locale),
    )

fun CustomerRequest.toUpdateCommand(
    customerId: UUID,
    locale: Locale,
): UpdateCustomerCommand =
    UpdateCustomerCommand(
        tenantId = tenantId,
        customerId = customerId,
        profile = toProfile(locale),
    )

fun CustomerStatusRequest.toStatusCommand(customerId: UUID): UpdateCustomerStatusCommand =
    UpdateCustomerStatusCommand(
        tenantId = tenantId,
        customerId = customerId,
        targetStatus = status,
    )

fun CustomerSearchRequest.toQuery(): ListCustomersQuery =
    ListCustomersQuery(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        status = status,
    )

private fun CustomerRequest.toProfile(locale: Locale): CustomerProfile =
    CustomerProfile(
        name = requireNotBlank(name.sanitizeName(), "name", FinanceValidationErrorCode.FINANCE_INVALID_NAME, locale),
        billingAddress =
            billingAddress.toDomainAddress("billingAddress", locale),
        shippingAddress = shippingAddress?.toDomainAddress("shippingAddress", locale),
        preferredCurrency = normalizeCurrency(currency.sanitizeCurrencyCode(), "currency", locale),
        paymentTerms = paymentTerms.toDomain(locale),
        primaryContact =
            contact?.let { ContactPerson(name = it.name.sanitizeName(), email = it.email?.sanitizeEmail(), phoneNumber = it.phoneNumber?.sanitizePhoneNumber()) },
        creditLimit =
            creditLimit?.let {
                Money.fromMajor(
                    major = it.amount,
                    currency = normalizeCurrency(it.currency.sanitizeCurrencyCode(), "creditLimit.currency", locale),
                )
            },
        dimensionDefaults =
            dimensionDefaults?.let {
                DimensionAssignments(
                    costCenterId = it.costCenterId,
                    profitCenterId = it.profitCenterId,
                    departmentId = it.departmentId,
                    projectId = it.projectId,
                    businessAreaId = it.businessAreaId,
                )
            } ?: DimensionAssignments(),
    )

private fun PaymentTermsRequest.toDomain(locale: Locale): PaymentTerms {
    if (dueInDays <= 0) {
        throw FinanceValidationException(
            errorCode = FinanceValidationErrorCode.FINANCE_INVALID_PAYMENT_TERMS,
            field = "paymentTerms.dueInDays",
            rejectedValue = dueInDays.toString(),
            locale = locale,
            message =
                ValidationMessageResolver.resolve(
                    FinanceValidationErrorCode.FINANCE_INVALID_PAYMENT_TERMS,
                    locale,
                    "dueInDays must be greater than zero",
                ),
        )
    }
    return PaymentTerms(
        code = requireNotBlank(code.sanitizeAccountCode(), "paymentTerms.code", FinanceValidationErrorCode.FINANCE_INVALID_PAYMENT_TERMS, locale),
        type = type,
        dueInDays = dueInDays,
        discountPercentage = discountPercentage,
        discountDays = discountDays,
    )
}

private fun AddressRequest.toDomainAddress(
    fieldPrefix: String,
    locale: Locale,
): Address =
    Address(
        line1 = requireNotBlank(line1.sanitizeName(), "$fieldPrefix.line1", FinanceValidationErrorCode.FINANCE_INVALID_NAME, locale),
        line2 = line2?.sanitizeName(),
        city = requireNotBlank(city.sanitizeName(), "$fieldPrefix.city", FinanceValidationErrorCode.FINANCE_INVALID_NAME, locale),
        stateOrProvince = stateOrProvince?.sanitizeName(),
        postalCode = postalCode?.sanitizeAccountCode(),
        countryCode = requireNotBlank(countryCode.sanitizeAccountCode(), "$fieldPrefix.countryCode", FinanceValidationErrorCode.FINANCE_INVALID_NAME, locale),
    )

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

private fun requireNotBlank(
    value: String?,
    field: String,
    code: FinanceValidationErrorCode,
    locale: Locale,
): String {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty()) {
        throw FinanceValidationException(
            errorCode = code,
            field = field,
            rejectedValue = value,
            locale = locale,
            message = ValidationMessageResolver.resolve(code, locale, field),
        )
    }
    return trimmed
}

fun Customer.toResponse(): CustomerResponse =
    CustomerResponse(
        id = id.value,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerNumber = customerNumber.value,
        name = profile.name,
        status = status,
        currency = profile.preferredCurrency,
        paymentTerms =
            PaymentTermsRequest(
                code = profile.paymentTerms.code,
                type = profile.paymentTerms.type,
                dueInDays = profile.paymentTerms.dueInDays,
                discountPercentage = profile.paymentTerms.discountPercentage,
                discountDays = profile.paymentTerms.discountDays,
            ),
        billingAddress =
            AddressRequest(
                line1 = profile.billingAddress.line1,
                line2 = profile.billingAddress.line2,
                city = profile.billingAddress.city,
                stateOrProvince = profile.billingAddress.stateOrProvince,
                postalCode = profile.billingAddress.postalCode,
                countryCode = profile.billingAddress.countryCode,
            ),
        shippingAddress =
            profile.shippingAddress?.let {
                AddressRequest(
                    line1 = it.line1,
                    line2 = it.line2,
                    city = it.city,
                    stateOrProvince = it.stateOrProvince,
                    postalCode = it.postalCode,
                    countryCode = it.countryCode,
                )
            },
        contact =
            profile.primaryContact?.let {
                ContactRequest(name = it.name, email = it.email, phoneNumber = it.phoneNumber)
            },
        creditLimit =
            profile.creditLimit?.let {
                CreditLimitRequest(
                    amount = BigDecimal(it.amount).movePointLeft(2),
                    currency = it.currency,
                )
            },
        dimensionDefaults =
            DimensionDefaultsRequest(
                costCenterId = profile.dimensionDefaults.costCenterId,
                profitCenterId = profile.dimensionDefaults.profitCenterId,
                departmentId = profile.dimensionDefaults.departmentId,
                projectId = profile.dimensionDefaults.projectId,
                businessAreaId = profile.dimensionDefaults.businessAreaId,
            ),
    )
