package com.erp.financial.shared.masterdata

import java.math.BigDecimal

data class PaymentTerms(
    val code: String,
    val description: String? = null,
    val type: PaymentTermType,
    val dueInDays: Int,
    val discountPercentage: BigDecimal? = null,
    val discountDays: Int? = null,
) {
    init {
        require(code.isNotBlank()) { "Payment term code must not be blank" }
        require(dueInDays >= 0) { "Due days must be non-negative" }
        if (discountPercentage != null) {
            require(discountPercentage >= BigDecimal.ZERO) { "Discount percent must be positive" }
            require(discountPercentage <= BigDecimal("100")) { "Discount percent cannot exceed 100" }
            require(!discountPercentage.stripTrailingZeros().equals(BigDecimal.ZERO) || discountDays != null) {
                "Discount days required when discount percentage is defined"
            }
        }
        if (discountDays != null) {
            require(discountDays >= 0) { "Discount days must be non-negative" }
            require(discountPercentage != null) { "Discount percentage required when discount days provided" }
        }
    }
}

enum class PaymentTermType {
    NET,
    DUE_END_OF_MONTH,
    CASH_ON_DELIVERY,
}
