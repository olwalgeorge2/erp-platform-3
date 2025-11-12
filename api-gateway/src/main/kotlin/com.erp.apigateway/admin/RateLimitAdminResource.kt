package com.erp.apigateway.admin

import com.erp.apigateway.exception.ErrorResponse
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
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/admin/ratelimits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@Tag(name = "Admin: Rate Limits", description = "Manage per-tenant and per-endpoint rate limit overrides")
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
    @Operation(summary = "List tenant overrides", description = "List all tenant-level rate limit overrides")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "OK",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden (admin role required)",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
    @Operation(summary = "Get tenant override", description = "Get a specific tenant's override")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "OK",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            APIResponse(
                responseCode = "404",
                description = "Not found",
            ),
        ],
    )
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
    @Operation(summary = "Create/update tenant override")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "204",
                description = "Upserted",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @Path("/tenants/{tenant}")
    fun upsertTenant(
        @PathParam("tenant") tenant: String,
        @RequestBody(description = "Override payload", required = true) req: OverrideRequest,
    ): Response {
        ensureAdmin()
        service.setTenantOverride(tenant, req.requestsPerMinute, req.windowSeconds)
        return Response.noContent().build()
    }

    @DELETE
    @Operation(summary = "Delete tenant override")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "204",
                description = "Deleted",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
    @Operation(summary = "List endpoint overrides", description = "List all endpoint-level rate limit overrides")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "OK",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
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
    @Operation(summary = "Get endpoint override", description = "Get a specific endpoint override by pattern")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "OK",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            APIResponse(
                responseCode = "404",
                description = "Not found",
            ),
        ],
    )
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
    @Operation(summary = "Create/update endpoint override")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "204",
                description = "Upserted",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @Path("/endpoints/{pattern}")
    fun upsertEndpoint(
        @PathParam("pattern") pattern: String,
        @RequestBody(description = "Override payload", required = true) req: OverrideRequest,
    ): Response {
        ensureAdmin()
        service.setEndpointOverride(pattern, req.requestsPerMinute, req.windowSeconds)
        return Response.noContent().build()
    }

    @DELETE
    @Operation(summary = "Delete endpoint override")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "204",
                description = "Deleted",
            ),
            APIResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @Path("/endpoints/{pattern}")
    fun deleteEndpoint(
        @PathParam("pattern") pattern: String,
    ): Response {
        ensureAdmin()
        service.deleteEndpointOverride(pattern)
        return Response.noContent().build()
    }
}
