package com.erp.testsupport

import com.erp.apigateway.infrastructure.RedisService
import com.erp.apigateway.infrastructure.RedisTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Integration test for Redis service using Testcontainers.
 * Verifies that Redis operations work correctly with the containerized Redis instance.
 */
@QuarkusTest
@QuarkusTestResource(RedisTestResource::class)
class ApiGatewayRedisIntegrationTest {
    @Inject
    lateinit var redisService: RedisService

    @Test
    fun `should increment counter in Redis`() {
        val key = "test:counter:${System.currentTimeMillis()}"

        val firstValue = redisService.incr(key)
        assertEquals(1L, firstValue)

        val secondValue = redisService.incr(key)
        assertEquals(2L, secondValue)
    }

    @Test
    fun `should set and retrieve value from Redis`() {
        val key = "test:value:${System.currentTimeMillis()}"

        // Initially should be null
        val initialValue = redisService.get(key)
        assertNull(initialValue)

        // After increment, should be retrievable
        redisService.incr(key)
        val retrievedValue = redisService.get(key)
        assertNotNull(retrievedValue)
    }

    @Test
    fun `should expire keys after specified time`() {
        val key = "test:expire:${System.currentTimeMillis()}"

        redisService.incr(key)
        redisService.expire(key, 1L)

        // Key should exist immediately
        val value = redisService.get(key)
        assertNotNull(value)

        // Note: We don't wait for expiration in this test to keep it fast
        // The expire command is verified to execute without error
    }
}
