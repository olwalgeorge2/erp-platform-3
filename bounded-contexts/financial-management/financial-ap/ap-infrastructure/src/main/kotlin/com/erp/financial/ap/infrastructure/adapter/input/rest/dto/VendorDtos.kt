package com.erp.financial.ap.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ap.application.port.input.command.RegisterVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorCommand
import com.erp.financial.ap.application.port.input.command.UpdateVendorStatusCommand
import com.erp.financial.ap.application.port.input.query.ListVendorsQuery
import com.erp.financial.ap.domain.model.vendor.Vendor
import com.erp.financial.ap.domain.model.vendor.VendorNumber
import com.erp.financial.ap.domain.model.vendor.VendorProfile
import com.erp.financial.shared.masterdata.Address
import com.erp.financial.shared.masterdata.BankAccountDetails
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
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import java.util.UUID

data class VendorRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val companyCodeId: UUID,
    @field:NotBlank
    val vendorNumber: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val currency: String,
    @field:Valid
    val paymentTerms: PaymentTermsRequest,
    @field:Valid
    val address: AddressRequest,
    @field:Valid
    val contact: ContactRequest? = null,
    @field:Valid
    val bankAccount: BankAccountRequest? = null,
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

data class BankAccountRequest(
    val bankName: String? = null,
    val accountNumber: String,
    val routingNumber: String? = null,
    val iban: String? = null,
    val swiftCode: String? = null,
)

data class DimensionDefaultsRequest(
    val costCenterId: UUID? = null,
    val profitCenterId: UUID? = null,
    val departmentId: UUID? = null,
    val projectId: UUID? = null,
    val businessAreaId: UUID? = null,
)

data class VendorResponse(
    val id: UUID,
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorNumber: String,
    val name: String,
    val status: MasterDataStatus,
    val currency: String,
    val paymentTerms: PaymentTermsRequest,
    val address: AddressRequest,
    val contact: ContactRequest?,
    val bankAccount: BankAccountRequest?,
    val dimensionDefaults: DimensionDefaultsRequest,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class VendorStatusRequest(
    @field:NotNull
    val tenantId: UUID,
    @field:NotNull
    val status: MasterDataStatus,
)

data class VendorSearchRequest(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: MasterDataStatus? = null,
)

data class VendorListRequest(
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:QueryParam("status")
    var status: String? = null,
) {
    fun toQuery(locale: Locale): ListVendorsQuery =
        ListVendorsQuery(
            tenantId =
                tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale),
            companyCodeId = companyCodeId,
            status = status?.let { parseStatus(it, locale) },
        )

    private fun parseStatus(
        raw: String,
        locale: Locale,
    ): MasterDataStatus =
        runCatching { MasterDataStatus.valueOf(raw.uppercase(Locale.getDefault())) }
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
                            MasterDataStatus.entries.joinToString(),
                        ),
                )
            }
}

data class VendorScopedRequest(
    @field:NotBlank
    @field:PathParam("vendorId")
    var vendorId: String? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun tenantId(locale: Locale): UUID =
        tenantId ?: missingField("tenantId", FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID, locale)

    fun vendorId(locale: Locale): UUID =
        vendorId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_VENDOR_ID,
                        field = "vendorId",
                        rejectedValue = vendorId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_VENDOR_ID,
                                locale,
                                vendorId,
                            ),
                    )
                }
        } ?: missingField("vendorId", FinanceValidationErrorCode.FINANCE_INVALID_VENDOR_ID, locale)
}

data class VendorPathParams(
    @field:NotBlank
    @field:PathParam("vendorId")
    var vendorId: String? = null,
) {
    fun vendorId(locale: Locale): UUID =
        vendorId?.let {
            runCatching { UUID.fromString(it) }
                .getOrElse {
                    throw FinanceValidationException(
                        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_VENDOR_ID,
                        field = "vendorId",
                        rejectedValue = vendorId,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                FinanceValidationErrorCode.FINANCE_INVALID_VENDOR_ID,
                                locale,
                                vendorId,
                            ),
                    )
                }
        } ?: missingField("vendorId", FinanceValidationErrorCode.FINANCE_INVALID_VENDOR_ID, locale)
}

fun VendorRequest.toRegisterCommand(locale: Locale): RegisterVendorCommand {
    val vendorNumberValue =
        requireNotBlank(
            vendorNumber.sanitizeAccountCode(),
            "vendorNumber",
            FinanceValidationErrorCode.FINANCE_INVALID_VENDOR_NUMBER,
            locale,
        )
    return RegisterVendorCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorNumber = VendorNumber(vendorNumberValue),
        profile = toProfile(locale),
    )
}

fun VendorRequest.toUpdateCommand(
    vendorId: UUID,
    locale: Locale,
): UpdateVendorCommand =
    UpdateVendorCommand(
        tenantId = tenantId,
        vendorId = vendorId,
        profile = toProfile(locale),
    )

fun VendorStatusRequest.toStatusCommand(vendorId: UUID): UpdateVendorStatusCommand =
    UpdateVendorStatusCommand(
        tenantId = tenantId,
        vendorId = vendorId,
        targetStatus = status,
    )

fun VendorSearchRequest.toQuery(): ListVendorsQuery =
    ListVendorsQuery(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        status = status,
    )

private fun VendorRequest.toProfile(locale: Locale): VendorProfile =
    VendorProfile(
        name = requireNotBlank(name.sanitizeName(), "name", FinanceValidationErrorCode.FINANCE_INVALID_NAME, locale),
        preferredCurrency = normalizeCurrency(currency.sanitizeCurrencyCode(), "currency", locale),
        paymentTerms = paymentTerms.toDomain(locale),
        address =
            Address(
                line1 =
                    requireNotBlank(
                        address.line1.sanitizeName(),
                        "address.line1",
                        FinanceValidationErrorCode.FINANCE_INVALID_NAME,
                        locale,
                    ),
                line2 = address.line2?.sanitizeName(),
                city =
                    requireNotBlank(
                        address.city.sanitizeName(),
                        "address.city",
                        FinanceValidationErrorCode.FINANCE_INVALID_NAME,
                        locale,
                    ),
                stateOrProvince = address.stateOrProvince?.sanitizeName(),
                postalCode = address.postalCode?.sanitizeAccountCode(),
                countryCode =
                    requireNotBlank(
                        address.countryCode.sanitizeAccountCode(),
                        "address.countryCode",
                        FinanceValidationErrorCode.FINANCE_INVALID_NAME,
                        locale,
                    ),
            ),
        primaryContact =
            contact?.let {
                ContactPerson(
                    name = it.name.sanitizeName(),
                    email = it.email?.sanitizeEmail(),
                    phoneNumber = it.phoneNumber?.sanitizePhoneNumber(),
                )
            },
        bankAccount =
            bankAccount?.let {
                BankAccountDetails(
                    bankName = it.bankName?.sanitizeName(),
                    accountNumber = it.accountNumber.sanitizeAccountCode(),
                    routingNumber = it.routingNumber?.sanitizeAccountCode(),
                    iban = it.iban?.sanitizeAccountCode(),
                    swiftCode = it.swiftCode?.sanitizeAccountCode(),
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
        code =
            requireNotBlank(
                code.sanitizeAccountCode(),
                "paymentTerms.code",
                FinanceValidationErrorCode.FINANCE_INVALID_PAYMENT_TERMS,
                locale,
            ),
        type = type,
        dueInDays = dueInDays,
        discountPercentage = discountPercentage,
        discountDays = discountDays,
    )
}

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

fun Vendor.toResponse(): VendorResponse =
    VendorResponse(
        id = id.value,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorNumber = vendorNumber.value,
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
        address =
            AddressRequest(
                line1 = profile.address.line1,
                line2 = profile.address.line2,
                city = profile.address.city,
                stateOrProvince = profile.address.stateOrProvince,
                postalCode = profile.address.postalCode,
                countryCode = profile.address.countryCode,
            ),
        contact =
            profile.primaryContact?.let {
                ContactRequest(name = it.name, email = it.email, phoneNumber = it.phoneNumber)
            },
        bankAccount =
            profile.bankAccount?.let {
                BankAccountRequest(
                    bankName = it.bankName,
                    accountNumber = it.accountNumber,
                    routingNumber = it.routingNumber,
                    iban = it.iban,
                    swiftCode = it.swiftCode,
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
        createdAt = createdAt,
        updatedAt = updatedAt,
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
