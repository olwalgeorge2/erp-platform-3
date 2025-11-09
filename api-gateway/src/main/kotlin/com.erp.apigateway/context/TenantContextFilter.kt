package com.erp.apigateway.context

import com.erp.apigateway.config.PublicEndpointsConfig
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHENTICATION + 10)
class TenantContextFilter : ContainerRequestFilter {
    @Inject
    lateinit var tenantContext: TenantContext

    @Inject
    lateinit var publicEndpointsConfig: PublicEndpointsConfig

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.path
        if (publicEndpointsConfig.isPublic(path)) {
            return
        }

        val claimsTenantId = requestContext.securityContext.userPrincipal?.name
        val headerTenant = requestContext.getHeaderString("X-Tenant-Id")

        tenantContext.tenantId = headerTenant ?: claimsTenantId
        tenantContext.userId = requestContext.getHeaderString("X-User-Id")

        tenantContext.tenantId?.let { requestContext.headers.add("X-Tenant-Id", it) }
        tenantContext.userId?.let { requestContext.headers.add("X-User-Id", it) }
    }
}
