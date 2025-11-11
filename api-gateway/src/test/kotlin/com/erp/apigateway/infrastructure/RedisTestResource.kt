package com.erp.apigateway.infrastructure

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class RedisTestResource : QuarkusTestResourceLifecycleManager {
    private lateinit var redis: GenericContainer<*>

    override fun start(): Map<String, String> {
        // Check if containers are disabled
        val withContainers = System.getProperty("withContainers", "false")
        if (withContainers != "true") {
            // Return empty map to skip container startup
            return emptyMap()
        }

        redis =
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .apply {
                    withReuse(true)
                    start()
                }

        val host = redis.host
        val port = redis.getMappedPort(6379)

        return mapOf(
            "quarkus.redis.hosts" to "redis://$host:$port",
            "REDIS_URL" to "redis://$host:$port",
        )
    }

    override fun stop() {
        if (::redis.isInitialized) {
            redis.stop()
        }
    }
}
