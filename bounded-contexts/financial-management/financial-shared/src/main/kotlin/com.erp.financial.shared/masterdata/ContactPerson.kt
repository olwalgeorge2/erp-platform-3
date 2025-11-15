package com.erp.financial.shared.masterdata

/**
 * Contact information stored alongside counterparties.
 */
data class ContactPerson(
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
) {
    init {
        require(name.isNotBlank()) { "Contact name cannot be blank" }
        if (email != null) {
            require(email.contains('@')) { "Contact email must be valid" }
        }
    }
}
