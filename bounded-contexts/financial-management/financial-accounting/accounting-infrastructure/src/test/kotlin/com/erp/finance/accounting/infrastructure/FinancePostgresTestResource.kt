package com.erp.finance.accounting.infrastructure

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.PostgreSQLContainer

class FinancePostgresTestResource : QuarkusTestResourceLifecycleManager {
    private lateinit var postgres: PostgreSQLContainer<*>

    override fun start(): Map<String, String> {
        val withContainers = System.getProperty("withContainers", "false")
        if (withContainers != "true") {
            return emptyMap()
        }

        postgres =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withReuse(true)
                start()
            }

        return mapOf(
            "quarkus.datasource.jdbc.url" to postgres.jdbcUrl,
            "quarkus.datasource.username" to postgres.username,
            "quarkus.datasource.password" to postgres.password,
            "quarkus.scheduler.enabled" to "false",
        )
    }

    override fun stop() {
        if (::postgres.isInitialized) {
            postgres.stop()
        }
    }
}
