package com.erp.identity.domain.model.tenant

/**
 * Value object representing organization details for a tenant.
 */
data class Organization(
    val legalName: String,
    val taxId: String?,
    val industry: String?,
    val address: Address?,
    val contactEmail: String,
    val contactPhone: String?,
) {
    init {
        require(legalName.isNotBlank()) { "Legal name cannot be blank" }
        require(contactEmail.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"))) {
            "Invalid email format"
        }
    }
}

/**
 * Value object for physical address
 */
data class Address(
    val street: String,
    val city: String,
    val state: String?,
    val postalCode: String,
    val country: String,
) {
    init {
        require(street.isNotBlank()) { "Street cannot be blank" }
        require(city.isNotBlank()) { "City cannot be blank" }
        require(postalCode.isNotBlank()) { "Postal code cannot be blank" }
        require(country.isNotBlank()) { "Country cannot be blank" }
        require(country.length == 2) { "Country must be ISO 3166-1 alpha-2 code" }
    }
}
