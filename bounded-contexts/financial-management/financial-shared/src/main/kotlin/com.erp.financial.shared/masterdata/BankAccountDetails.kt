package com.erp.financial.shared.masterdata

/**
 * Optional bank account information to speed up payment integrations.
 */
data class BankAccountDetails(
    val bankName: String? = null,
    val accountNumber: String,
    val routingNumber: String? = null,
    val iban: String? = null,
    val swiftCode: String? = null,
) {
    init {
        require(accountNumber.isNotBlank()) { "Account number must be provided" }
    }
}
