package com.erp.financial.ar.application.cache

import com.erp.financial.ar.application.port.output.CustomerRepository
import com.erp.financial.ar.domain.model.customer.Customer
import com.erp.financial.ar.domain.model.customer.CustomerId
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
class CustomerExistenceCache(
    private val customerRepository: CustomerRepository,
    private val validationCircuitBreaker: ValidationCircuitBreaker,
    private val meterRegistry: MeterRegistry,
    @ConfigProperty(name = "validation.performance.cache.customer.max-size", defaultValue = "10000")
    private val maxSize: Long,
    @ConfigProperty(name = "validation.performance.cache.customer.ttl", defaultValue = "PT5M")
    private val ttl: Duration,
) {
    private data class CacheKey(val tenantId: UUID, val customerId: UUID)

    private val cache: Cache<CacheKey, Optional<Customer>> =
        Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxSize)
            .recordStats()
            .build { key -> loadCustomer(key) }

    init {
        registerMetrics()
    }

    fun find(
        tenantId: UUID,
        customerId: UUID,
    ): Customer? =
        cache.get(CacheKey(tenantId, customerId)).orElse(null)

    fun put(customer: Customer) {
        cache.put(CacheKey(customer.tenantId, customer.id.value), Optional.of(customer))
    }

    fun evict(
        tenantId: UUID,
        customerId: UUID,
    ) {
        cache.invalidate(CacheKey(tenantId, customerId))
    }

    private fun loadCustomer(key: CacheKey): Optional<Customer> =
        Optional.ofNullable(
            validationCircuitBreaker.guard("customer_lookup") {
                customerRepository.findById(key.tenantId, CustomerId(key.customerId))
            },
        )

    private fun registerMetrics() {
        val tags = Tags.of("cache", "customer-existence")
        meterRegistry.gauge("validation.cache.size", tags, cache) { it.estimatedSize().toDouble() }
        meterRegistry.gauge("validation.cache.hitratio", tags, cache) { it.stats().hitRate() }
        meterRegistry.gauge("validation.cache.missratio", tags, cache) { it.stats().missRate() }
    }
}
