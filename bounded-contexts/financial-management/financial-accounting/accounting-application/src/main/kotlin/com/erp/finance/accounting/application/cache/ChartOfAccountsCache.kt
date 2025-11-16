package com.erp.finance.accounting.application.cache

import com.erp.finance.accounting.application.port.output.ChartOfAccountsRepository
import com.erp.finance.accounting.domain.model.Account
import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import com.erp.financial.shared.validation.security.ValidationCircuitBreaker
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import java.util.Optional
import java.util.UUID

@ApplicationScoped
class ChartOfAccountsCache(
    private val chartRepository: ChartOfAccountsRepository,
    private val validationCircuitBreaker: ValidationCircuitBreaker,
    private val meterRegistry: MeterRegistry,
    @ConfigProperty(name = "validation.performance.cache.chart.max-size", defaultValue = "2000")
    private val maxSize: Long,
    @ConfigProperty(name = "validation.performance.cache.chart.ttl", defaultValue = "PT10M")
    private val ttl: Duration,
) {
    private data class CacheKey(
        val tenantId: UUID,
        val chartId: UUID,
    )

    private val cache: Cache<CacheKey, Optional<ChartOfAccounts>> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxSize)
            .recordStats()
            .build { key -> loadChart(key) }

    init {
        registerMetrics()
    }

    fun find(
        tenantId: UUID,
        chartId: UUID,
    ): ChartOfAccounts? = cache.get(CacheKey(tenantId, chartId)) { key -> loadChart(key) }.orElse(null)

    fun findAccount(
        tenantId: UUID,
        chartId: UUID,
        accountId: UUID,
    ): Account? =
        find(tenantId, chartId)
            ?.accounts
            ?.get(AccountId(accountId))

    fun put(chart: ChartOfAccounts) {
        cache.put(CacheKey(chart.tenantId, chart.id.value), Optional.of(chart))
    }

    fun evict(
        tenantId: UUID,
        chartId: UUID,
    ) {
        cache.invalidate(CacheKey(tenantId, chartId))
    }

    fun warmup(charts: Collection<ChartOfAccounts>) {
        charts.forEach(::put)
    }

    private fun loadChart(key: CacheKey): Optional<ChartOfAccounts> =
        Optional.ofNullable(
            validationCircuitBreaker.guard("chart_lookup") {
                chartRepository.findById(ChartOfAccountsId(key.chartId), key.tenantId)
            },
        )

    private fun registerMetrics() {
        val tags = Tags.of("cache", "chart-of-accounts")
        meterRegistry.gauge("validation.cache.size", tags, cache) { it.estimatedSize().toDouble() }
        meterRegistry.gauge("validation.cache.hitratio", tags, cache) { it.stats().hitRate() }
        meterRegistry.gauge("validation.cache.missratio", tags, cache) { it.stats().missRate() }
    }
}
