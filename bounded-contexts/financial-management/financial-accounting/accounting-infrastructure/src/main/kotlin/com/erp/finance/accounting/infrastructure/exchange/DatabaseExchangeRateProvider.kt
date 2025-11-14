package com.erp.finance.accounting.infrastructure.exchange

import com.erp.finance.accounting.domain.policy.ExchangeRate
import com.erp.finance.accounting.domain.policy.ExchangeRateProvider
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal
import java.time.Instant

@ApplicationScoped
class DatabaseExchangeRateProvider(
    private val repository: JpaExchangeRateRepository,
) : ExchangeRateProvider {
    override fun findRate(
        baseCurrency: String,
        quoteCurrency: String,
        asOf: Instant,
    ): ExchangeRate? {
        val normalizedBase = baseCurrency.uppercase()
        val normalizedQuote = quoteCurrency.uppercase()
        if (normalizedBase == normalizedQuote) {
            return ExchangeRate(
                baseCurrency = normalizedBase,
                quoteCurrency = normalizedQuote,
                rate = BigDecimal.ONE,
                asOf = asOf,
            )
        }
        return repository.findLatest(
            baseCurrency = normalizedBase,
            quoteCurrency = normalizedQuote,
            asOf = asOf,
        )
    }
}
