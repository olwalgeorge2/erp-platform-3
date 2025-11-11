package com.erp.apigateway.admin

import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext

@Path("/admin/ratelimits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
class RateLimitAdminResource {
    @Inject
    lateinit var service: RateLimitAdminService

    @Context
    lateinit var securityContext: SecurityContext

    private fun ensureAdmin() {
        if (!securityContext.isUserInRole("admin")) {
            val resp =
                Response
                    .status(Response.Status.FORBIDDEN)
                    .entity(
                        mapOf(
                            "code" to "FORBIDDEN",
                            "message" to "Admin role required",
                        ),
                    ).build()
            throw WebApplicationException(resp)
        }
    }

    data class OverrideRequest(
        val requestsPerMinute: Int,
        val windowSeconds: Int,
    )

    // Tenants
    @GET
    @Path("/tenants")
    fun listTenants(): Response {
        ensureAdmin()
        val items =
            service
                .listTenantOverrides()
                .map { (tenant, v) ->
                    mapOf(
                        "tenant" to tenant,
                        "requestsPerMinute" to v.first,
                        "windowSeconds" to v.second,
                    )
                }
        return Response.ok(items).build()
    }

    @GET
    @Path("/tenants/{tenant}")
    fun getTenant(
        @PathParam("tenant") tenant: String,
    ): Response {
        ensureAdmin()
        val v = service.getTenantOverride(tenant) ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response
            .ok(
                mapOf(
                    "tenant" to tenant,
                    "requestsPerMinute" to v.first,
                    "windowSeconds" to v.second,
                ),
            ).build()
    }

    @PUT
    @Path("/tenants/{tenant}")
    fun upsertTenant(
        @PathParam("tenant") tenant: String,
        req: OverrideRequest,
    ): Response {
        ensureAdmin()
        service.setTenantOverride(tenant, req.requestsPerMinute, req.windowSeconds)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/tenants/{tenant}")
    fun deleteTenant(
        @PathParam("tenant") tenant: String,
    ): Response {
        ensureAdmin()
        service.deleteTenantOverride(tenant)
        return Response.noContent().build()
    }

    // Endpoints
    @GET
    @Path("/endpoints")
    fun listEndpoints(): Response {
        ensureAdmin()
        val items =
            service
                .listEndpointOverrides()
                .map { (pattern, v) ->
                    mapOf(
                        "pattern" to pattern,
                        "requestsPerMinute" to v.first,
                        "windowSeconds" to v.second,
                    )
                }
        return Response.ok(items).build()
    }

    @GET
    @Path("/endpoints/{pattern}")
    fun getEndpoint(
        @PathParam("pattern") pattern: String,
    ): Response {
        ensureAdmin()
        val v = service.getEndpointOverride(pattern) ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response
            .ok(
                mapOf(
                    "pattern" to pattern,
                    "requestsPerMinute" to v.first,
                    "windowSeconds" to v.second,
                ),
            ).build()
    }

    @PUT
    @Path("/endpoints/{pattern}")
    fun upsertEndpoint(
        @PathParam("pattern") pattern: String,
        req: OverrideRequest,
    ): Response {
        ensureAdmin()
        service.setEndpointOverride(pattern, req.requestsPerMinute, req.windowSeconds)
        return Response.noContent().build()
    }

    @DELETE
    @Path("/endpoints/{pattern}")
    fun deleteEndpoint(
        @PathParam("pattern") pattern: String,
    ): Response {
        ensureAdmin()
        service.deleteEndpointOverride(pattern)
        return Response.noContent().build()
    }
}
