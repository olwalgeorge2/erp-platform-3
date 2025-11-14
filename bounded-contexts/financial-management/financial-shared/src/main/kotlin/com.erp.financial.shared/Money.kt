package com.erp.financial.shared

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Monetary value represented in **minor units** (e.g., cents).
 *
 * This shared value object is the canonical representation that bounded contexts
 * can use when exchanging financial amounts. Currency codes follow ISO-4217.
 */
data class Money(
    val amount: Long,
    val currency: String,
) : Comparable<Money> {
    init {
        require(currency.length == 3) { "Currency must be a 3-letter ISO-4217 code" }
    }

    operator fun plus(other: Money): Money {
        verifyCurrency(other)
        return copy(amount = amount + other.amount)
    }

    operator fun minus(other: Money): Money {
        verifyCurrency(other)
        return copy(amount = amount - other.amount)
    }

    operator fun unaryMinus(): Money = copy(amount = -amount)

    fun abs(): Money = if (amount >= 0) this else unaryMinus()

    fun isZero(): Boolean = amount == 0L

    override fun compareTo(other: Money): Int {
        verifyCurrency(other)
        return amount.compareTo(other.amount)
    }

    private fun verifyCurrency(other: Money) {
        require(currency == other.currency) {
            "Currency mismatch: $currency vs ${other.currency}"
        }
    }

    companion object {
        fun zero(currency: String): Money = Money(0, currency.uppercase())

        /**
         * Create a [Money] from major units (e.g., dollars) by specifying the scale.
         *
         * @param major The major-unit amount
         * @param currency ISO-4217 code
         * @param scale Number of decimal digits for the currency (defaults to 2)
         */
        fun fromMajor(
            major: BigDecimal,
            currency: String,
            scale: Int = 2,
        ): Money {
            val amount =
                major
                    .setScale(scale, RoundingMode.HALF_UP)
                    .movePointRight(scale)
                    .longValueExact()
            return Money(amount, currency.uppercase())
        }
    }
}
