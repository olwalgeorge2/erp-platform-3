package com.erp.apigateway.health

import com.erp.apigateway.routing.RouteResolver
import com.erp.apigateway.routing.ServiceRoute
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Backend services readiness health check.
 *
 * Verifies that configured backend services (e.g., tenancy-identity) are reachable
 * before marking the gateway as ready. This prevents routing requests to unavailable services.
 *
 * This check runs on every readiness probe invocation (typically every 5-10 seconds).
 */
@Readiness
@ApplicationScoped
class BackendServicesCheck
    @Inject
    constructor(
        private val routeResolver: RouteResolver,
    ) : HealthCheck {
        private val logger = LoggerFactory.getLogger(BackendServicesCheck::class.java)

        // HTTP client with aggressive timeout for fast health checks
        private val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build()

        override fun call(): HealthCheckResponse {
            val builder =
                HealthCheckResponse
                    .builder()
                    .name("Backend services")

            val serviceChecks = mutableMapOf<String, String>()
            var allHealthy = true

            try {
                // Check identity service health endpoint via route target
                val identityHealth = checkService("/api/v1/identity/")
                serviceChecks["identity-service"] = identityHealth.status
                if (!identityHealth.healthy) {
                    allHealthy = false
                }

                // Add more backend checks here as services are added
                // val financialHealth = checkService("/api/v1/financial/health/live")
                // serviceChecks["financial-service"] = financialHealth.status

                if (allHealthy) {
                    builder.up()
                    logger.debug("All backend services healthy: {}", serviceChecks)
                } else {
                    builder.down()
                    logger.warn("Some backend services unhealthy: {}", serviceChecks)
                }
            } catch (e: Exception) {
                allHealthy = false
                serviceChecks["error"] = e.message ?: "Unknown error"
                builder.down()
                logger.error("Backend health check failed: {}", e.message, e)
            }

            // Add service statuses to health check response
            serviceChecks.forEach { (service, status) ->
                builder.withData(service, status)
            }

            return builder.build()
        }

        private fun checkService(path: String): ServiceHealth =
            try {
                val route = routeResolver.resolve(path)
                val healthUrl = buildHealthUrl(route, path)

                val request =
                    HttpRequest
                        .newBuilder()
                        .uri(URI.create(healthUrl))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    ServiceHealth(healthy = true, status = "UP")
                } else {
                    ServiceHealth(healthy = false, status = "DOWN (HTTP ${response.statusCode()})")
                }
            } catch (e: Exception) {
                logger.debug("Service health check failed for {}: {}", path, e.message)
                ServiceHealth(healthy = false, status = "DOWN (${e.javaClass.simpleName})")
            }

        private fun buildHealthUrl(
            route: ServiceRoute,
            path: String,
        ): String {
            val baseUrl = route.target.baseUrl.trimEnd('/')
            val healthPath =
                if (route.target.healthPath.startsWith("/")) {
                    route.target.healthPath
                } else {
                    "/${route.target.healthPath}"
                }
            return "$baseUrl$healthPath"
        }

        private data class ServiceHealth(
            val healthy: Boolean,
            val status: String,
        )
    }
