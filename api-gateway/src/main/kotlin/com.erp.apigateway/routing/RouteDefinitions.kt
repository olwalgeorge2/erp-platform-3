package com.erp.apigateway.routing

object RouteDefinitions {
    fun defaultRoutes(identityBaseUrl: String): List<ServiceRoute> =
        listOf(
            ServiceRoute(
                pattern = "/api/v1/identity/*",
                target =
                    ServiceTarget(
                        baseUrl = identityBaseUrl,
                        timeoutSeconds = 5,
                        retries = 2,
                    ),
                authRequired = false, // login/register will be public; refined later per-endpoint
            ),
        )
}
