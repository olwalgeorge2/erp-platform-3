package com.erp.apigateway.admin

import com.erp.apigateway.infrastructure.RedisTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
@QuarkusTestResource(RedisTestResource::class)
class RateLimitAdminServiceTest {
    @Inject
    lateinit var service: RateLimitAdminService

    @Test
    fun `tenant override lifecycle`() {
        val t = "acme"
        assertNull(service.getTenantOverride(t))
        service.setTenantOverride(t, 123, 60)
        val got = service.getTenantOverride(t)
        assertEquals(Pair(123, 60), got)
        val listed = service.listTenantOverrides()
        assertEquals(Pair(123, 60), listed[t])
        service.deleteTenantOverride(t)
        assertNull(service.getTenantOverride(t))
    }

    @Test
    fun `endpoint override lifecycle`() {
        val p = "/api/v1/identity/*"
        assertNull(service.getEndpointOverride(p))
        service.setEndpointOverride(p, 5, 60)
        val got = service.getEndpointOverride(p)
        assertEquals(Pair(5, 60), got)
        val listed = service.listEndpointOverrides()
        assertEquals(Pair(5, 60), listed[p])
        service.deleteEndpointOverride(p)
        assertNull(service.getEndpointOverride(p))
    }
}
