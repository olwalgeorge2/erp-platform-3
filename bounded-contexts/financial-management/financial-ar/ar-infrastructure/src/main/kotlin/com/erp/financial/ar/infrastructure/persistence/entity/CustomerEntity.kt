package com.erp.financial.ar.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ar.domain.model.customer.Customer
import com.erp.financial.ar.domain.model.customer.CustomerId
import com.erp.financial.ar.domain.model.customer.CustomerNumber
import com.erp.financial.ar.domain.model.customer.CustomerProfile
import com.erp.financial.shared.Money
import com.erp.financial.shared.masterdata.Address
import com.erp.financial.shared.masterdata.ContactPerson
import com.erp.financial.shared.masterdata.MasterDataStatus
import com.erp.financial.shared.masterdata.PaymentTermType
import com.erp.financial.shared.masterdata.PaymentTerms
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "customers", schema = "financial_ar")
class CustomerEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID,
    @Column(name = "customer_number", nullable = false, length = 32)
    var customerNumber: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: MasterDataStatus = MasterDataStatus.ACTIVE,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "name", nullable = false, length = 255)
    var name: String,
    @Column(name = "payment_term_code", nullable = false, length = 32)
    var paymentTermCode: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_term_type", nullable = false, length = 32)
    var paymentTermType: PaymentTermType,
    @Column(name = "payment_term_due_days", nullable = false)
    var paymentTermDueDays: Int,
    @Column(name = "payment_term_discount_percent")
    var paymentTermDiscountPercent: BigDecimal? = null,
    @Column(name = "payment_term_discount_days")
    var paymentTermDiscountDays: Int? = null,
    @Column(name = "billing_address_line1", nullable = false)
    var billingAddressLine1: String,
    @Column(name = "billing_address_line2")
    var billingAddressLine2: String? = null,
    @Column(name = "billing_city", nullable = false)
    var billingCity: String,
    @Column(name = "billing_state")
    var billingState: String? = null,
    @Column(name = "billing_postal_code")
    var billingPostalCode: String? = null,
    @Column(name = "billing_country_code", nullable = false, length = 2)
    var billingCountryCode: String,
    @Column(name = "shipping_address_line1")
    var shippingAddressLine1: String? = null,
    @Column(name = "shipping_address_line2")
    var shippingAddressLine2: String? = null,
    @Column(name = "shipping_city")
    var shippingCity: String? = null,
    @Column(name = "shipping_state")
    var shippingState: String? = null,
    @Column(name = "shipping_postal_code")
    var shippingPostalCode: String? = null,
    @Column(name = "shipping_country_code", length = 2)
    var shippingCountryCode: String? = null,
    @Column(name = "contact_name")
    var contactName: String? = null,
    @Column(name = "contact_email")
    var contactEmail: String? = null,
    @Column(name = "contact_phone")
    var contactPhone: String? = null,
    @Column(name = "credit_limit_amount")
    var creditLimitAmount: Long? = null,
    @Column(name = "credit_limit_currency", length = 3)
    var creditLimitCurrency: String? = null,
    @Column(name = "default_cost_center_id")
    var defaultCostCenterId: UUID? = null,
    @Column(name = "default_profit_center_id")
    var defaultProfitCenterId: UUID? = null,
    @Column(name = "default_department_id")
    var defaultDepartmentId: UUID? = null,
    @Column(name = "default_project_id")
    var defaultProjectId: UUID? = null,
    @Column(name = "default_business_area_id")
    var defaultBusinessAreaId: UUID? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0,
) {
    fun toDomain(): Customer =
        Customer(
            id = CustomerId(id),
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            customerNumber = CustomerNumber(customerNumber),
            status = status,
            profile =
                CustomerProfile(
                    name = name,
                    billingAddress =
                        Address(
                            line1 = billingAddressLine1,
                            line2 = billingAddressLine2,
                            city = billingCity,
                            stateOrProvince = billingState,
                            postalCode = billingPostalCode,
                            countryCode = billingCountryCode,
                        ),
                    shippingAddress =
                        shippingAddressLine1?.let {
                            Address(
                                line1 = it,
                                line2 = shippingAddressLine2,
                                city = shippingCity ?: billingCity,
                                stateOrProvince = shippingState,
                                postalCode = shippingPostalCode,
                                countryCode = shippingCountryCode ?: billingCountryCode,
                            )
                        },
                    preferredCurrency = currency,
                    paymentTerms =
                        PaymentTerms(
                            code = paymentTermCode,
                            type = paymentTermType,
                            dueInDays = paymentTermDueDays,
                            discountPercentage = paymentTermDiscountPercent,
                            discountDays = paymentTermDiscountDays,
                        ),
                    primaryContact =
                        contactName?.let {
                            ContactPerson(
                                name = it,
                                email = contactEmail,
                                phoneNumber = contactPhone,
                            )
                        },
                    creditLimit =
                        creditLimitAmount?.let {
                            Money(amount = it, currency = creditLimitCurrency ?: currency)
                        },
                    dimensionDefaults =
                        DimensionAssignments(
                            costCenterId = defaultCostCenterId,
                            profitCenterId = defaultProfitCenterId,
                            departmentId = defaultDepartmentId,
                            projectId = defaultProjectId,
                            businessAreaId = defaultBusinessAreaId,
                        ),
                ),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: Customer) {
        id = domain.id.value
        tenantId = domain.tenantId
        companyCodeId = domain.companyCodeId
        customerNumber = domain.customerNumber.value
        status = domain.status
        currency = domain.profile.preferredCurrency
        name = domain.profile.name
        paymentTermCode = domain.profile.paymentTerms.code
        paymentTermType = domain.profile.paymentTerms.type
        paymentTermDueDays = domain.profile.paymentTerms.dueInDays
        paymentTermDiscountPercent = domain.profile.paymentTerms.discountPercentage
        paymentTermDiscountDays = domain.profile.paymentTerms.discountDays
        billingAddressLine1 = domain.profile.billingAddress.line1
        billingAddressLine2 = domain.profile.billingAddress.line2
        billingCity = domain.profile.billingAddress.city
        billingState = domain.profile.billingAddress.stateOrProvince
        billingPostalCode = domain.profile.billingAddress.postalCode
        billingCountryCode = domain.profile.billingAddress.countryCode
        shippingAddressLine1 = domain.profile.shippingAddress?.line1
        shippingAddressLine2 = domain.profile.shippingAddress?.line2
        shippingCity = domain.profile.shippingAddress?.city
        shippingState = domain.profile.shippingAddress?.stateOrProvince
        shippingPostalCode = domain.profile.shippingAddress?.postalCode
        shippingCountryCode = domain.profile.shippingAddress?.countryCode
        contactName = domain.profile.primaryContact?.name
        contactEmail = domain.profile.primaryContact?.email
        contactPhone = domain.profile.primaryContact?.phoneNumber
        creditLimitAmount = domain.profile.creditLimit?.amount
        creditLimitCurrency = domain.profile.creditLimit?.currency
        defaultCostCenterId = domain.profile.dimensionDefaults.costCenterId
        defaultProfitCenterId = domain.profile.dimensionDefaults.profitCenterId
        defaultDepartmentId = domain.profile.dimensionDefaults.departmentId
        defaultProjectId = domain.profile.dimensionDefaults.projectId
        defaultBusinessAreaId = domain.profile.dimensionDefaults.businessAreaId
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }

    companion object {
        fun from(domain: Customer): CustomerEntity =
            CustomerEntity(
                id = domain.id.value,
                tenantId = domain.tenantId,
                companyCodeId = domain.companyCodeId,
                customerNumber = domain.customerNumber.value,
                status = domain.status,
                currency = domain.profile.preferredCurrency,
                name = domain.profile.name,
                paymentTermCode = domain.profile.paymentTerms.code,
                paymentTermType = domain.profile.paymentTerms.type,
                paymentTermDueDays = domain.profile.paymentTerms.dueInDays,
                paymentTermDiscountPercent = domain.profile.paymentTerms.discountPercentage,
                paymentTermDiscountDays = domain.profile.paymentTerms.discountDays,
                billingAddressLine1 = domain.profile.billingAddress.line1,
                billingAddressLine2 = domain.profile.billingAddress.line2,
                billingCity = domain.profile.billingAddress.city,
                billingState = domain.profile.billingAddress.stateOrProvince,
                billingPostalCode = domain.profile.billingAddress.postalCode,
                billingCountryCode = domain.profile.billingAddress.countryCode,
                shippingAddressLine1 = domain.profile.shippingAddress?.line1,
                shippingAddressLine2 = domain.profile.shippingAddress?.line2,
                shippingCity = domain.profile.shippingAddress?.city,
                shippingState = domain.profile.shippingAddress?.stateOrProvince,
                shippingPostalCode = domain.profile.shippingAddress?.postalCode,
                shippingCountryCode = domain.profile.shippingAddress?.countryCode,
                contactName = domain.profile.primaryContact?.name,
                contactEmail = domain.profile.primaryContact?.email,
                contactPhone = domain.profile.primaryContact?.phoneNumber,
                creditLimitAmount = domain.profile.creditLimit?.amount,
                creditLimitCurrency = domain.profile.creditLimit?.currency,
                defaultCostCenterId = domain.profile.dimensionDefaults.costCenterId,
                defaultProfitCenterId = domain.profile.dimensionDefaults.profitCenterId,
                defaultDepartmentId = domain.profile.dimensionDefaults.departmentId,
                defaultProjectId = domain.profile.dimensionDefaults.projectId,
                defaultBusinessAreaId = domain.profile.dimensionDefaults.businessAreaId,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
