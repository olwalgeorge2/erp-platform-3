package com.erp.financial.ap.domain.model.vendor

import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.financial.shared.masterdata.Address
import com.erp.financial.shared.masterdata.BankAccountDetails
import com.erp.financial.shared.masterdata.ContactPerson
import com.erp.financial.shared.masterdata.PaymentTerms

/**
 * Snapshot of vendor-facing metadata used whenever we create or update a vendor record.
 */
data class VendorProfile(
    val name: String,
    val preferredCurrency: String,
    val paymentTerms: PaymentTerms,
    val address: Address,
    val primaryContact: ContactPerson? = null,
    val bankAccount: BankAccountDetails? = null,
    val dimensionDefaults: DimensionAssignments = DimensionAssignments(),
) {
    init {
        require(name.isNotBlank()) { "Vendor name cannot be blank" }
        require(preferredCurrency.length == 3) { "Currency must be ISO-4217" }
    }
}
