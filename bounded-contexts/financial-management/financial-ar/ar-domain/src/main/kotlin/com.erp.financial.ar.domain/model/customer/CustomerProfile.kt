package com.erp.financial.ar.domain.model.customer

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.shared.Money
import com.erp.financial.shared.masterdata.Address
import com.erp.financial.shared.masterdata.ContactPerson
import com.erp.financial.shared.masterdata.PaymentTerms

data class CustomerProfile(
    val name: String,
    val billingAddress: Address,
    val shippingAddress: Address? = null,
    val preferredCurrency: String,
    val paymentTerms: PaymentTerms,
    val primaryContact: ContactPerson? = null,
    val creditLimit: Money? = null,
    val dimensionDefaults: DimensionAssignments = DimensionAssignments(),
) {
    init {
        require(name.isNotBlank()) { "Customer name cannot be blank" }
        require(preferredCurrency.length == 3) { "Currency must be ISO-4217" }
        if (shippingAddress != null) {
            require(shippingAddress.countryCode.isNotBlank()) { "Shipping country required" }
        }
    }
}
