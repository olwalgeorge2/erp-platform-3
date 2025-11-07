package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.application.port.input.command.DeleteRoleCommand
import com.erp.identity.application.port.input.query.ListRolesQuery
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.adapter.input.rest.dto.CreateRoleRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.UpdateRoleRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.identity.infrastructure.service.IdentityCommandService
import com.erp.identity.infrastructure.service.IdentityQueryService
import com.erp.identity.infrastructure.service.security.AuthorizationService
import com.erp.shared.types.results.Result
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import kotlin.math.max

@ApplicationScoped
@Path("/api/tenants/{tenantId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class RoleResource
    @Inject
    constructor(
        private val commandService: IdentityCommandService,
        private val queryService: IdentityQueryService,
        private val authorizationService: AuthorizationService,
    ) {
        @POST
        fun createRole(
            @PathParam("tenantId") tenantIdRaw: String,
            request: CreateRoleRequest,
            @Context uriInfo: UriInfo,
        ): Response =
            withTenant(tenantIdRaw) { tenantId ->
                authorizationService.requireRoleManagement(tenantId)?.let { return@withTenant it }
                when (val result = commandService.createRole(request.toCommand(tenantId))) {
                    is Result.Success -> {
                        val role = result.value
                        val location =
                            uriInfo.baseUriBuilder
                                .path("api")
                                .path("tenants")
                                .path(role.tenantId.toString())
                                .path("roles")
                                .path(role.id.toString())
                                .build()
                        Response
                            .created(location)
                            .entity(role.toResponse())
                            .build()
                    }
                    is Result.Failure -> result.failureResponse()
                }
            }

        @PUT
        @Path("/{roleId}")
        fun updateRole(
            @PathParam("tenantId") tenantIdRaw: String,
            @PathParam("roleId") roleIdRaw: String,
            request: UpdateRoleRequest,
        ): Response =
            withTenantAndRole(tenantIdRaw, roleIdRaw) { tenantId, roleId ->
                authorizationService.requireRoleManagement(tenantId)?.let { return@withTenantAndRole it }
                when (val result = commandService.updateRole(request.toCommand(tenantId, roleId))) {
                    is Result.Success -> Response.ok(result.value.toResponse()).build()
                    is Result.Failure -> result.failureResponse()
                }
            }

        @DELETE
        @Path("/{roleId}")
        fun deleteRole(
            @PathParam("tenantId") tenantIdRaw: String,
            @PathParam("roleId") roleIdRaw: String,
        ): Response =
            withTenantAndRole(tenantIdRaw, roleIdRaw) { tenantId, roleId ->
                authorizationService.requireRoleManagement(tenantId)?.let { return@withTenantAndRole it }
                val command =
                    DeleteRoleCommand(
                        tenantId = tenantId,
                        roleId = roleId,
                    )
                when (val result = commandService.deleteRole(command)) {
                    is Result.Success -> Response.noContent().build()
                    is Result.Failure -> result.failureResponse()
                }
            }

        @GET
        fun listRoles(
            @PathParam("tenantId") tenantIdRaw: String,
            @QueryParam("limit") limit: Int?,
            @QueryParam("offset") offset: Int?,
        ): Response =
            withTenant(tenantIdRaw) { tenantId ->
                authorizationService.requireRoleRead(tenantId)?.let { return@withTenant it }
                val safeLimit = limit?.coerceIn(1, 200) ?: 50
                val safeOffset = offset?.let { max(0, it) } ?: 0
                val query =
                    ListRolesQuery(
                        tenantId = tenantId,
                        limit = safeLimit,
                        offset = safeOffset,
                    )
                when (val result = queryService.listRoles(query)) {
                    is Result.Success -> Response.ok(result.value.map { it.toResponse() }).build()
                    is Result.Failure -> result.failureResponse()
                }
            }

        @GET
        @Path("/{roleId}")
        fun getRole(
            @PathParam("tenantId") tenantIdRaw: String,
            @PathParam("roleId") roleIdRaw: String,
        ): Response =
            withTenantAndRole(tenantIdRaw, roleIdRaw) { tenantId, roleId ->
                authorizationService.requireRoleRead(tenantId)?.let { return@withTenantAndRole it }
                when (val result = queryService.getRole(tenantId, roleId)) {
                    is Result.Success -> {
                        val role = result.value ?: return@withTenantAndRole notFoundResponse(roleId.toString())
                        Response.ok(role.toResponse()).build()
                    }
                    is Result.Failure -> result.failureResponse()
                }
            }

        private fun withTenant(
            tenantIdRaw: String,
            block: (TenantId) -> Response,
        ): Response {
            val tenantId = parseTenantId(tenantIdRaw) ?: return invalidTenantResponse(tenantIdRaw)
            return block(tenantId)
        }

        private fun withTenantAndRole(
            tenantIdRaw: String,
            roleIdRaw: String,
            block: (TenantId, RoleId) -> Response,
        ): Response {
            val tenantId = parseTenantId(tenantIdRaw) ?: return invalidTenantResponse(tenantIdRaw)
            val roleId = parseRoleId(roleIdRaw) ?: return invalidRoleResponse(roleIdRaw)
            return block(tenantId, roleId)
        }

        private fun parseTenantId(raw: String): TenantId? =
            try {
                TenantId.from(raw)
            } catch (_: IllegalArgumentException) {
                null
            }

        private fun parseRoleId(raw: String): RoleId? =
            try {
                RoleId.from(raw)
            } catch (_: IllegalArgumentException) {
                null
            }

        private fun invalidTenantResponse(value: String): Response =
            Response
                .status(Response.Status.BAD_REQUEST)
                .entity(
                    ErrorResponse(
                        code = "INVALID_TENANT_ID",
                        message = "Invalid tenant identifier",
                        details = mapOf("tenantId" to value),
                    ),
                ).build()

        private fun invalidRoleResponse(value: String): Response =
            Response
                .status(Response.Status.BAD_REQUEST)
                .entity(
                    ErrorResponse(
                        code = "INVALID_ROLE_ID",
                        message = "Invalid role identifier",
                        details = mapOf("roleId" to value),
                    ),
                ).build()

        private fun notFoundResponse(roleId: String): Response =
            Response
                .status(Response.Status.NOT_FOUND)
                .entity(
                    ErrorResponse(
                        code = "ROLE_NOT_FOUND",
                        message = "Role not found",
                        details = mapOf("roleId" to roleId),
                    ),
                ).build()
    }
