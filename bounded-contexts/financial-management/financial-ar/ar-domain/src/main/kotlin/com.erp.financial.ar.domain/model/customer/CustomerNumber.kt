package com.erp.financial.ar.domain.model.customer

@JvmInline
value class CustomerNumber(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Customer number cannot be blank" }
        require(value.length <= 32) { "Customer number cannot exceed 32 characters" }
    }
}
