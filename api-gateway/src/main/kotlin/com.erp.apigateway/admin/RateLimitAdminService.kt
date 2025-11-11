package com.erp.apigateway.admin

import com.erp.apigateway.infrastructure.RedisService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class RateLimitAdminService {
    @Inject
    lateinit var redis: RedisService

    private fun encode(
        rpm: Int,
        windowSeconds: Int,
    ): String = "$rpm:$windowSeconds"

    private fun decode(value: String?): Pair<Int, Int>? =
        try {
            if (value.isNullOrBlank()) return null
            val parts = value.split(":")
            if (parts.size != 2) return null
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) {
            null
        }

    fun setTenantOverride(
        tenant: String,
        rpm: Int,
        windowSeconds: Int,
    ) {
        redis.set(tenantKey(tenant), encode(rpm, windowSeconds))
    }

    fun getTenantOverride(tenant: String): Pair<Int, Int>? = decode(redis.get(tenantKey(tenant)))

    fun deleteTenantOverride(tenant: String) {
        redis.del(tenantKey(tenant))
    }

    fun listTenantOverrides(): Map<String, Pair<Int, Int>> =
        redis
            .keys("$TENANT_PREFIX*")
            .associate { k ->
                val tenant = k.removePrefix(TENANT_PREFIX)
                val v = decode(redis.get(k))
                tenant to (v ?: Pair(-1, -1))
            }.filterValues { it.first >= 0 }

    fun setEndpointOverride(
        pattern: String,
        rpm: Int,
        windowSeconds: Int,
    ) {
        redis.set(endpointKey(pattern), encode(rpm, windowSeconds))
    }

    fun getEndpointOverride(pattern: String): Pair<Int, Int>? = decode(redis.get(endpointKey(pattern)))

    fun deleteEndpointOverride(pattern: String) {
        redis.del(endpointKey(pattern))
    }

    fun listEndpointOverrides(): Map<String, Pair<Int, Int>> =
        redis
            .keys("$ENDPOINT_PREFIX*")
            .associate { k ->
                val pattern = k.removePrefix(ENDPOINT_PREFIX)
                val v = decode(redis.get(k))
                pattern to (v ?: Pair(-1, -1))
            }.filterValues { it.first >= 0 }

    private fun tenantKey(tenant: String) = TENANT_PREFIX + tenant

    private fun endpointKey(pattern: String) = ENDPOINT_PREFIX + pattern

    companion object {
        private const val TENANT_PREFIX = "rl:tenant:"
        private const val ENDPOINT_PREFIX = "rl:endpoint:"
    }
}
