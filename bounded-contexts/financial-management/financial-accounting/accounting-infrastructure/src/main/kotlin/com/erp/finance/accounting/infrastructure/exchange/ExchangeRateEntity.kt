package com.erp.finance.accounting.infrastructure.exchange

import com.erp.finance.accounting.domain.policy.ExchangeRate
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "exchange_rates", schema = "financial_accounting")
class ExchangeRateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "base_currency", nullable = false, length = 3)
    var baseCurrency: String,
    @Column(name = "quote_currency", nullable = false, length = 3)
    var quoteCurrency: String,
    @Column(name = "rate", nullable = false, precision = 18, scale = 8)
    var rate: BigDecimal,
    @Column(name = "as_of", nullable = false)
    var asOf: Instant,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    fun toDomain(): ExchangeRate =
        ExchangeRate(
            baseCurrency = baseCurrency,
            quoteCurrency = quoteCurrency,
            rate = rate,
            asOf = asOf,
        )
}

@ApplicationScoped
class JpaExchangeRateRepository
    @Inject
    constructor(
        private val entityManager: EntityManager,
    ) {
        fun findLatest(
            baseCurrency: String,
            quoteCurrency: String,
            asOf: Instant,
        ): ExchangeRate? =
            entityManager
                .createQuery(
                    """
                    SELECT e
                    FROM ExchangeRateEntity e
                    WHERE e.baseCurrency = :base
                      AND e.quoteCurrency = :quote
                      AND e.asOf <= :asOf
                    ORDER BY e.asOf DESC
                    """.trimIndent(),
                    ExchangeRateEntity::class.java,
                ).setParameter("base", baseCurrency)
                .setParameter("quote", quoteCurrency)
                .setParameter("asOf", asOf)
                .setMaxResults(1)
                .resultList
                .firstOrNull()
                ?.toDomain()
    }
