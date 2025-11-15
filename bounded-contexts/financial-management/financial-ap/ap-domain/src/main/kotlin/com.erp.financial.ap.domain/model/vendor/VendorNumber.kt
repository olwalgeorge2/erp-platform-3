package com.erp.financial.ap.domain.model.vendor

@JvmInline
value class VendorNumber(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Vendor number must not be blank" }
        require(value.length <= 32) { "Vendor number cannot exceed 32 characters" }
    }
}
