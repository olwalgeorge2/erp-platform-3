package com.erp.identity.infrastructure

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.PostgreSQLContainer

class PostgresTestResource : QuarkusTestResourceLifecycleManager {
    private lateinit var postgres: PostgreSQLContainer<*>

    override fun start(): Map<String, String> {
        postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withReuse(true)
                start()
            }

        return mapOf(
            "quarkus.datasource.jdbc.url" to postgres.jdbcUrl,
            "quarkus.datasource.username" to postgres.username,
            "quarkus.datasource.password" to postgres.password,
            // Disable scheduled jobs during tests to avoid EMF closed/rollback noise on shutdown
            "quarkus.scheduler.enabled" to "false",
        )
    }

    override fun stop() {
        if (::postgres.isInitialized) {
            postgres.stop()
        }
    }
}
