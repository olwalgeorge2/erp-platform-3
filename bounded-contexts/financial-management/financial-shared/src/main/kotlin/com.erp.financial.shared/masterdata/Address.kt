package com.erp.financial.shared.masterdata

/**
 * Lightweight postal address representation reused by vendors and customers.
 */
data class Address(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val stateOrProvince: String? = null,
    val postalCode: String? = null,
    val countryCode: String,
) {
    init {
        require(line1.isNotBlank()) { "Address line1 must be provided" }
        require(city.isNotBlank()) { "City must be provided" }
        require(countryCode.length == 2) { "Country code must follow ISO-3166 alpha-2" }
    }
}
