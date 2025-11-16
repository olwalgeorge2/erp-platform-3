package com.erp.financial.ap.application.cache

import com.erp.financial.ap.application.port.output.VendorRepository
import com.erp.financial.ap.domain.model.vendor.Vendor
import com.erp.financial.ap.domain.model.vendor.VendorId
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
class VendorExistenceCache(
    private val vendorRepository: VendorRepository,
    private val validationCircuitBreaker: ValidationCircuitBreaker,
    private val meterRegistry: MeterRegistry,
    @ConfigProperty(name = "validation.performance.cache.vendor.max-size", defaultValue = "10000")
    private val maxSize: Long,
    @ConfigProperty(name = "validation.performance.cache.vendor.ttl", defaultValue = "PT5M")
    private val ttl: Duration,
) {
    private data class CacheKey(val tenantId: UUID, val vendorId: UUID)

    private val cache: Cache<CacheKey, Optional<Vendor>> =
        Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxSize)
            .recordStats()
            .build { key -> loadVendor(key) }

    init {
        registerMetrics()
    }

    fun find(
        tenantId: UUID,
        vendorId: UUID,
    ): Vendor? =
        cache.get(CacheKey(tenantId, vendorId)).orElse(null)

    fun put(vendor: Vendor) {
        cache.put(CacheKey(vendor.tenantId, vendor.id.value), Optional.of(vendor))
    }

    fun evict(
        tenantId: UUID,
        vendorId: UUID,
    ) {
        cache.invalidate(CacheKey(tenantId, vendorId))
    }

    fun recordMisses() = cache.stats().missCount()

    private fun loadVendor(key: CacheKey): Optional<Vendor> =
        Optional.ofNullable(
            validationCircuitBreaker.guard("vendor_lookup") {
                vendorRepository.findById(key.tenantId, VendorId(key.vendorId))
            },
        )

    private fun registerMetrics() {
        val tags = Tags.of("cache", "vendor-existence")
        meterRegistry.gauge("validation.cache.size", tags, cache) { it.estimatedSize().toDouble() }
        meterRegistry.gauge("validation.cache.hitratio", tags, cache) { it.stats().hitRate() }
        meterRegistry.gauge("validation.cache.missratio", tags, cache) { it.stats().missRate() }
    }
}
