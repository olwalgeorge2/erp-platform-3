package com.erp.apigateway.routing

class RouteNotFoundException(
    message: String,
) : RuntimeException(message)

data class ServiceTarget(
    val baseUrl: String,
    val timeoutSeconds: Int = 5,
    val retries: Int = 2,
)

data class ServiceRoute(
    val pattern: String,
    val target: ServiceTarget,
    val authRequired: Boolean = true,
)

class RouteResolver(
    private val routes: List<ServiceRoute>,
) {
    fun resolve(path: String): ServiceRoute =
        routes.firstOrNull { matches(it.pattern, path) }
            ?: throw RouteNotFoundException("No route for $path")

    private fun matches(
        pattern: String,
        path: String,
    ): Boolean {
        if (pattern.endsWith("/*")) {
            val prefix = pattern.removeSuffix("/*")
            return path.startsWith(prefix)
        }
        return pattern == path
    }
}
