package com.erp.finance.accounting.domain.policy

import com.erp.finance.accounting.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * Represents a conversion rate from [baseCurrency] -> [quoteCurrency].
 *
 * Money amounts are expressed in minor units (same convention as the existing [Money] value class).
 */
data class ExchangeRate(
    val baseCurrency: String,
    val quoteCurrency: String,
    val rate: BigDecimal,
    val asOf: Instant = Instant.now(),
) {
    init {
        require(baseCurrency.length == 3) { "Base currency must be ISO-4217 (3 letters)" }
        require(quoteCurrency.length == 3) { "Quote currency must be ISO-4217 (3 letters)" }
        require(rate > BigDecimal.ZERO) { "Rate must be positive" }
    }

    /**
     * Convert [amount] denominated in [baseCurrency] into [quoteCurrency].
     */
    fun convert(amount: Money): Money {
        val converted =
            BigDecimal(amount.amount)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
        return Money(converted.longValueExact())
    }

    fun invert(): ExchangeRate =
        ExchangeRate(
            baseCurrency = quoteCurrency,
            quoteCurrency = baseCurrency,
            rate = BigDecimal.ONE.divide(rate, 12, RoundingMode.HALF_UP),
            asOf = asOf,
        )
}

/**
 * Abstraction for looking up exchange rates. Implementation will arrive in Phase 5.
 */
interface ExchangeRateProvider {
    fun findRate(
        baseCurrency: String,
        quoteCurrency: String,
        asOf: Instant = Instant.now(),
    ): ExchangeRate?
}
