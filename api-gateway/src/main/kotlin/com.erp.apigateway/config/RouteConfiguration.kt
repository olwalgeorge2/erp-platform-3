package com.erp.apigateway.config

import com.erp.apigateway.routing.PathRewrite
import com.erp.apigateway.routing.RouteDefinitions
import com.erp.apigateway.routing.RouteResolver
import com.erp.apigateway.routing.ServiceRoute
import com.erp.apigateway.routing.ServiceTarget
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject

@ApplicationScoped
class RouteConfiguration {
    @Inject
    lateinit var routesConfig: GatewayRoutesConfig

    @Produces
    fun routeResolver(): RouteResolver {
        val configured = routesConfig.routes()
        val routes: List<ServiceRoute> =
            if (configured != null &&
                configured.isNotEmpty()
            ) {
                val mapped = configured.map { toServiceRoute(it) }
                validateRoutes(mapped)
                mapped
            } else {
                RouteDefinitions.defaultRoutes("http://localhost:8081")
            }
        return RouteResolver(routes)
    }

    private fun toServiceRoute(entry: GatewayRoutesConfig.RouteEntry): ServiceRoute {
        val timeoutSeconds = (entry.timeout() ?: java.time.Duration.ofSeconds(5)).seconds.toInt()
        val retries = entry.retries() ?: 2
        val authRequired = entry.authRequired() ?: true
        val healthPath = entry.healthPath() ?: "/q/health/ready"
        val target =
            ServiceTarget(
                baseUrl = entry.baseUrl(),
                timeoutSeconds = timeoutSeconds,
                retries = retries,
                healthPath = healthPath,
                backoffInitialMs = entry.backoffInitialMs() ?: 100,
                backoffMaxMs = entry.backoffMaxMs() ?: 1000,
                backoffJitterMs = entry.backoffJitterMs() ?: 50,
                cbFailureThreshold = entry.cbFailureThreshold() ?: 5,
                cbResetMs = entry.cbResetMs() ?: 30000,
            )

        val rewriteCfg = entry.rewrite()
        val rewrite =
            if (rewriteCfg !=
                null
            ) {
                PathRewrite(removePrefix = rewriteCfg.removePrefix(), addPrefix = rewriteCfg.addPrefix())
            } else {
                null
            }

        return ServiceRoute(
            pattern = entry.pattern(),
            target = target,
            authRequired = authRequired,
            pathRewrite = rewrite,
        )
    }

    private fun validateRoutes(routes: List<ServiceRoute>) {
        // healthPath must be absolute
        routes.forEach {
            val hp = it.target.healthPath
            require(hp.startsWith("/")) {
                "healthPath must start with '/': $hp (pattern ${it.pattern})"
            }
            require(it.target.timeoutSeconds in 1..120) {
                "timeoutSeconds must be 1..120 (pattern ${it.pattern})"
            }
        }
        // Overlap detection for /* prefixes
        val prefixes =
            routes
                .map { it.pattern }
                .filter { it.endsWith("/*") }
                .map { it.removeSuffix("/*") }
        for (i in prefixes.indices) {
            for (j in i + 1 until prefixes.size) {
                val a = prefixes[i]
                val b = prefixes[j]
                if (a.startsWith(b) || b.startsWith(a)) {
                    throw IllegalArgumentException("Overlapping route patterns detected: '$a/*' and '$b/*'")
                }
            }
        }
    }
}
