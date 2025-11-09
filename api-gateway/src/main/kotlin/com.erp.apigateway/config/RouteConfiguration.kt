package com.erp.apigateway.config

import com.erp.apigateway.routing.RouteDefinitions
import com.erp.apigateway.routing.RouteResolver
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class RouteConfiguration(
    @ConfigProperty(name = "gateway.services.tenancy-identity.url")
    private val identityServiceUrl: String,
) {
    @Produces
    fun routeResolver(): RouteResolver {
        val routes = RouteDefinitions.defaultRoutes(identityServiceUrl)
        return RouteResolver(routes)
    }
}
