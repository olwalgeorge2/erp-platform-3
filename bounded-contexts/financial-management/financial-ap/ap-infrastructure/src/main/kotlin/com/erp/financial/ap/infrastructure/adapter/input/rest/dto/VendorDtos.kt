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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class VendorRequest(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val vendorNumber: String,
    val name: String,
    val currency: String,
    val paymentTerms: PaymentTermsRequest,
    val address: AddressRequest,
    val contact: ContactRequest? = null,
    val bankAccount: BankAccountRequest? = null,
    val dimensionDefaults: DimensionDefaultsRequest? = null,
)

data class PaymentTermsRequest(
    val code: String,
    val type: PaymentTermType,
    val dueInDays: Int,
    val discountPercentage: BigDecimal? = null,
    val discountDays: Int? = null,
)

data class AddressRequest(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val stateOrProvince: String? = null,
    val postalCode: String? = null,
    val countryCode: String,
)

data class ContactRequest(
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
    val tenantId: UUID,
    val status: MasterDataStatus,
)

data class VendorSearchRequest(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: MasterDataStatus? = null,
)

fun VendorRequest.toRegisterCommand(): RegisterVendorCommand =
    RegisterVendorCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        vendorNumber = VendorNumber(vendorNumber),
        profile = toProfile(),
    )

fun VendorRequest.toUpdateCommand(vendorId: UUID): UpdateVendorCommand =
    UpdateVendorCommand(
        tenantId = tenantId,
        vendorId = vendorId,
        profile = toProfile(),
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

private fun VendorRequest.toProfile(): VendorProfile =
    VendorProfile(
        name = name,
        preferredCurrency = currency.uppercase(),
        paymentTerms =
            PaymentTerms(
                code = paymentTerms.code,
                type = paymentTerms.type,
                dueInDays = paymentTerms.dueInDays,
                discountPercentage = paymentTerms.discountPercentage,
                discountDays = paymentTerms.discountDays,
            ),
        address =
            Address(
                line1 = address.line1,
                line2 = address.line2,
                city = address.city,
                stateOrProvince = address.stateOrProvince,
                postalCode = address.postalCode,
                countryCode = address.countryCode,
            ),
        primaryContact = contact?.let { ContactPerson(name = it.name, email = it.email, phoneNumber = it.phoneNumber) },
        bankAccount =
            bankAccount?.let {
                BankAccountDetails(
                    bankName = it.bankName,
                    accountNumber = it.accountNumber,
                    routingNumber = it.routingNumber,
                    iban = it.iban,
                    swiftCode = it.swiftCode,
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
