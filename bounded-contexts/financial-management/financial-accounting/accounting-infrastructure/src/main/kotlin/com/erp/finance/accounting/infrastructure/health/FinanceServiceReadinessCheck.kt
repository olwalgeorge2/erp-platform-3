package com.erp.finance.accounting.infrastructure.health

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import javax.sql.DataSource

/**
 * Basic readiness check that verifies the finance database is reachable.
 * This acts as the entry point for future, richer health diagnostics (outbox lag, scheduler status, etc.).
 */
@Readiness
@ApplicationScoped
class FinanceServiceReadinessCheck
    @Inject
    constructor(
        private val dataSource: DataSource,
    ) : HealthCheck {
        override fun call(): HealthCheckResponse =
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("SELECT 1").execute()
                }
                HealthCheckResponse.up("finance-db")
            } catch (ex: Exception) {
                HealthCheckResponse
                    .builder()
                    .name("finance-db")
                    .down()
                    .withData("error", ex.message ?: "unknown")
                    .build()
            }
    }
