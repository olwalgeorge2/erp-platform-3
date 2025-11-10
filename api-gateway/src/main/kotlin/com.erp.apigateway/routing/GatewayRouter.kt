package com.erp.apigateway.routing

class RouteNotFoundException(
    message: String,
) : RuntimeException(message)

data class ServiceTarget(
    val baseUrl: String,
    val timeoutSeconds: Int = 5,
    val retries: Int = 2,
    val healthPath: String = "/q/health/ready",
    val backoffInitialMs: Long = 100,
    val backoffMaxMs: Long = 1000,
    val backoffJitterMs: Long = 50,
)

data class PathRewrite(
    val removePrefix: String,
    val addPrefix: String,
)

data class ServiceRoute(
    val pattern: String,
    val target: ServiceTarget,
    val authRequired: Boolean = true,
    val pathRewrite: PathRewrite? = null,
) {
    fun mapUpstreamPath(incomingPath: String): String {
        val rewrite = pathRewrite ?: return incomingPath
        val remove = normalizePrefix(rewrite.removePrefix)
        val add = normalizePrefix(rewrite.addPrefix)

        if (!incomingPath.startsWith(remove)) return incomingPath

        val remainder = incomingPath.removePrefix(remove)
        val tail = if (remainder.isEmpty()) "/" else remainder
        return add.trimEnd('/') + tail
    }

    private fun normalizePrefix(p: String): String {
        var v = if (p.isEmpty()) "/" else p
        if (!v.startsWith('/')) v = "/$v"
        // do not force trailing slash; logic handles both
        return v
    }
}

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
