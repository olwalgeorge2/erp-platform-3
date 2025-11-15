package com.erp.financial.ap.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.ap.domain.model.vendor.Vendor
import com.erp.financial.ap.domain.model.vendor.VendorId
import com.erp.financial.ap.domain.model.vendor.VendorNumber
import com.erp.financial.ap.domain.model.vendor.VendorProfile
import com.erp.financial.shared.masterdata.Address
import com.erp.financial.shared.masterdata.BankAccountDetails
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "vendors", schema = "financial_ap")
class VendorEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "company_code_id", nullable = false)
    var companyCodeId: UUID,
    @Column(name = "vendor_number", nullable = false, length = 32)
    var vendorNumber: String,
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
    @Column(name = "address_line1", nullable = false, length = 255)
    var addressLine1: String,
    @Column(name = "address_line2", length = 255)
    var addressLine2: String? = null,
    @Column(name = "city", nullable = false, length = 128)
    var city: String,
    @Column(name = "state", length = 128)
    var state: String? = null,
    @Column(name = "postal_code", length = 32)
    var postalCode: String? = null,
    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String,
    @Column(name = "contact_name", length = 255)
    var contactName: String? = null,
    @Column(name = "contact_email", length = 320)
    var contactEmail: String? = null,
    @Column(name = "contact_phone", length = 64)
    var contactPhone: String? = null,
    @Column(name = "bank_name", length = 255)
    var bankName: String? = null,
    @Column(name = "bank_account", length = 64)
    var bankAccount: String? = null,
    @Column(name = "bank_routing", length = 64)
    var bankRouting: String? = null,
    @Column(name = "bank_iban", length = 64)
    var bankIban: String? = null,
    @Column(name = "bank_swift", length = 32)
    var bankSwift: String? = null,
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
    fun toDomain(): Vendor =
        Vendor(
            id = VendorId(id),
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            vendorNumber = VendorNumber(vendorNumber),
            status = status,
            profile =
                VendorProfile(
                    name = name,
                    preferredCurrency = currency,
                    paymentTerms =
                        PaymentTerms(
                            code = paymentTermCode,
                            type = paymentTermType,
                            dueInDays = paymentTermDueDays,
                            discountPercentage = paymentTermDiscountPercent,
                            discountDays = paymentTermDiscountDays,
                        ),
                    address =
                        Address(
                            line1 = addressLine1,
                            line2 = addressLine2,
                            city = city,
                            stateOrProvince = state,
                            postalCode = postalCode,
                            countryCode = countryCode,
                        ),
                    primaryContact =
                        contactName?.let {
                            ContactPerson(
                                name = it,
                                email = contactEmail,
                                phoneNumber = contactPhone,
                            )
                        },
                    bankAccount =
                        bankAccount?.let {
                            BankAccountDetails(
                                bankName = bankName,
                                accountNumber = it,
                                routingNumber = bankRouting,
                                iban = bankIban,
                                swiftCode = bankSwift,
                            )
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

    fun updateFrom(domain: Vendor) {
        id = domain.id.value
        tenantId = domain.tenantId
        companyCodeId = domain.companyCodeId
        vendorNumber = domain.vendorNumber.value
        status = domain.status
        currency = domain.profile.preferredCurrency
        name = domain.profile.name
        paymentTermCode = domain.profile.paymentTerms.code
        paymentTermType = domain.profile.paymentTerms.type
        paymentTermDueDays = domain.profile.paymentTerms.dueInDays
        paymentTermDiscountPercent = domain.profile.paymentTerms.discountPercentage
        paymentTermDiscountDays = domain.profile.paymentTerms.discountDays
        addressLine1 = domain.profile.address.line1
        addressLine2 = domain.profile.address.line2
        city = domain.profile.address.city
        state = domain.profile.address.stateOrProvince
        postalCode = domain.profile.address.postalCode
        countryCode = domain.profile.address.countryCode
        contactName = domain.profile.primaryContact?.name
        contactEmail = domain.profile.primaryContact?.email
        contactPhone = domain.profile.primaryContact?.phoneNumber
        bankName = domain.profile.bankAccount?.bankName
        bankAccount = domain.profile.bankAccount?.accountNumber
        bankRouting = domain.profile.bankAccount?.routingNumber
        bankIban = domain.profile.bankAccount?.iban
        bankSwift = domain.profile.bankAccount?.swiftCode
        defaultCostCenterId = domain.profile.dimensionDefaults.costCenterId
        defaultProfitCenterId = domain.profile.dimensionDefaults.profitCenterId
        defaultDepartmentId = domain.profile.dimensionDefaults.departmentId
        defaultProjectId = domain.profile.dimensionDefaults.projectId
        defaultBusinessAreaId = domain.profile.dimensionDefaults.businessAreaId
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }

    companion object {
        fun from(domain: Vendor): VendorEntity =
            VendorEntity(
                id = domain.id.value,
                tenantId = domain.tenantId,
                companyCodeId = domain.companyCodeId,
                vendorNumber = domain.vendorNumber.value,
                status = domain.status,
                currency = domain.profile.preferredCurrency,
                name = domain.profile.name,
                paymentTermCode = domain.profile.paymentTerms.code,
                paymentTermType = domain.profile.paymentTerms.type,
                paymentTermDueDays = domain.profile.paymentTerms.dueInDays,
                paymentTermDiscountPercent = domain.profile.paymentTerms.discountPercentage,
                paymentTermDiscountDays = domain.profile.paymentTerms.discountDays,
                addressLine1 = domain.profile.address.line1,
                addressLine2 = domain.profile.address.line2,
                city = domain.profile.address.city,
                state = domain.profile.address.stateOrProvince,
                postalCode = domain.profile.address.postalCode,
                countryCode = domain.profile.address.countryCode,
                contactName = domain.profile.primaryContact?.name,
                contactEmail = domain.profile.primaryContact?.email,
                contactPhone = domain.profile.primaryContact?.phoneNumber,
                bankName = domain.profile.bankAccount?.bankName,
                bankAccount = domain.profile.bankAccount?.accountNumber,
                bankRouting = domain.profile.bankAccount?.routingNumber,
                bankIban = domain.profile.bankAccount?.iban,
                bankSwift = domain.profile.bankAccount?.swiftCode,
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
