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
import java.math.BigDecimal
import java.util.UUID

data class CustomerRequest(
    val tenantId: UUID,
    val companyCodeId: UUID,
    val customerNumber: String,
    val name: String,
    val currency: String,
    val paymentTerms: PaymentTermsRequest,
    val billingAddress: AddressRequest,
    val shippingAddress: AddressRequest? = null,
    val contact: ContactRequest? = null,
    val creditLimit: CreditLimitRequest? = null,
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

data class CreditLimitRequest(
    val amount: BigDecimal,
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
    val tenantId: UUID,
    val status: MasterDataStatus,
)

data class CustomerSearchRequest(
    val tenantId: UUID,
    val companyCodeId: UUID? = null,
    val status: MasterDataStatus? = null,
)

fun CustomerRequest.toRegisterCommand(): RegisterCustomerCommand =
    RegisterCustomerCommand(
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        customerNumber = CustomerNumber(customerNumber),
        profile = toProfile(),
    )

fun CustomerRequest.toUpdateCommand(customerId: UUID): UpdateCustomerCommand =
    UpdateCustomerCommand(
        tenantId = tenantId,
        customerId = customerId,
        profile = toProfile(),
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

private fun CustomerRequest.toProfile(): CustomerProfile =
    CustomerProfile(
        name = name,
        billingAddress =
            Address(
                line1 = billingAddress.line1,
                line2 = billingAddress.line2,
                city = billingAddress.city,
                stateOrProvince = billingAddress.stateOrProvince,
                postalCode = billingAddress.postalCode,
                countryCode = billingAddress.countryCode,
            ),
        shippingAddress =
            shippingAddress?.let {
                Address(
                    line1 = it.line1,
                    line2 = it.line2,
                    city = it.city,
                    stateOrProvince = it.stateOrProvince,
                    postalCode = it.postalCode,
                    countryCode = it.countryCode,
                )
            },
        preferredCurrency = currency.uppercase(),
        paymentTerms =
            PaymentTerms(
                code = paymentTerms.code,
                type = paymentTerms.type,
                dueInDays = paymentTerms.dueInDays,
                discountPercentage = paymentTerms.discountPercentage,
                discountDays = paymentTerms.discountDays,
            ),
        primaryContact =
            contact?.let { ContactPerson(name = it.name, email = it.email, phoneNumber = it.phoneNumber) },
        creditLimit =
            creditLimit?.let {
                Money.fromMajor(
                    major = it.amount,
                    currency = it.currency.uppercase(),
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
