package com.erp.finance.accounting.application.cache

import com.erp.finance.accounting.application.port.output.LedgerRepository
import com.erp.finance.accounting.domain.model.Ledger
import com.erp.finance.accounting.domain.model.LedgerId
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
class LedgerExistenceCache(
    private val ledgerRepository: LedgerRepository,
    private val validationCircuitBreaker: ValidationCircuitBreaker,
    private val meterRegistry: MeterRegistry,
    @ConfigProperty(name = "validation.performance.cache.ledger.max-size", defaultValue = "5000")
    private val maxSize: Long,
    @ConfigProperty(name = "validation.performance.cache.ledger.ttl", defaultValue = "PT10M")
    private val ttl: Duration,
) {
    private data class CacheKey(val tenantId: UUID, val ledgerId: UUID)

    private val cache: Cache<CacheKey, Optional<Ledger>> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxSize)
            .recordStats()
            .build { key -> loadLedger(key) }

    init {
        registerMetrics()
    }

    fun find(
        tenantId: UUID,
        ledgerId: UUID,
    ): Ledger? = cache.get(CacheKey(tenantId, ledgerId)).orElse(null)

    fun put(ledger: Ledger) {
        cache.put(CacheKey(ledger.tenantId, ledger.id.value), Optional.of(ledger))
    }

    fun evict(
        tenantId: UUID,
        ledgerId: UUID,
    ) {
        cache.invalidate(CacheKey(tenantId, ledgerId))
    }

    fun warmup(ledgers: Collection<Ledger>) {
        ledgers.forEach(::put)
    }

    private fun loadLedger(key: CacheKey): Optional<Ledger> =
        Optional.ofNullable(
            validationCircuitBreaker.guard("ledger_lookup") {
                ledgerRepository.findById(LedgerId(key.ledgerId), key.tenantId)
            },
        )

    private fun registerMetrics() {
        val tags = Tags.of("cache", "ledger-existence")
        meterRegistry.gauge("validation.cache.size", tags, cache) { it.estimatedSize().toDouble() }
        meterRegistry.gauge("validation.cache.hitratio", tags, cache) { it.stats().hitRate() }
        meterRegistry.gauge("validation.cache.missratio", tags, cache) { it.stats().missRate() }
    }
}
