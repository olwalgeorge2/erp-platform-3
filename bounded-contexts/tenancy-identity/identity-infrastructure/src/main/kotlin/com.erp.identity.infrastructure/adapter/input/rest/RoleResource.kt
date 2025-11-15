package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.adapter.input.rest.dto.CreateRoleRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.DeleteRoleRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.ListRolesRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.RoleResponse
import com.erp.identity.infrastructure.adapter.input.rest.dto.UpdateRoleRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.identity.infrastructure.service.IdentityCommandService
import com.erp.identity.infrastructure.service.IdentityQueryService
import com.erp.identity.infrastructure.service.security.AuthorizationService
import com.erp.identity.infrastructure.validation.IdentityValidationException
import com.erp.identity.infrastructure.validation.ValidationErrorCode
import com.erp.identity.infrastructure.validation.ValidationMessageResolver
import com.erp.shared.types.results.Result
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.Locale

open class BaseRoleResource() {
    @Inject
    protected lateinit var commandService: IdentityCommandService

    @Inject
    protected lateinit var queryService: IdentityQueryService

    @Inject
    protected lateinit var authorizationService: AuthorizationService

    @Context
    protected var httpHeaders: HttpHeaders? = null

    constructor(
        commandService: IdentityCommandService,
        queryService: IdentityQueryService,
        authorizationService: AuthorizationService,
    ) : this() {
        this.commandService = commandService
        this.queryService = queryService
        this.authorizationService = authorizationService
    }

    @POST
    @Operation(summary = "Create role")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "201",
                description = "Role created",
                content = [
                    Content(
                        schema = Schema(implementation = RoleResponse::class),
                    ),
                ],
            ),
            APIResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    fun createRole(
        @PathParam("tenantId") tenantIdRaw: String,
        @RequestBody(description = "Create role payload", required = true)
        request: CreateRoleRequest,
        @Context uriInfo: UriInfo,
    ): Response =
        withTenant(tenantIdRaw) { tenantId ->
            authorizationService.requireRoleManagement(tenantId)?.let { return@withTenant it }
            when (val result = commandService.createRole(request.toCommand(tenantId))) {
                is Result.Success -> {
                    val role = result.value
                    val location =
                        uriInfo
                            .absolutePathBuilder
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
    @Operation(summary = "Update role")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "Updated",
                content = [Content(schema = Schema(implementation = RoleResponse::class))],
            ),
            APIResponse(
                responseCode = "400",
                description = "Invalid identifier or request",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @Path("/{roleId}")
    fun updateRole(
        @PathParam("tenantId") tenantIdRaw: String,
        @PathParam("roleId") roleIdRaw: String,
        @RequestBody(description = "Update role payload", required = true)
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
    @Operation(summary = "Delete role")
    @APIResponses(
        value = [
            APIResponse(responseCode = "204", description = "Deleted"),
            APIResponse(
                responseCode = "400",
                description = "Invalid identifier",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @Path("/{roleId}")
    fun deleteRole(
        @Valid @BeanParam request: DeleteRoleRequest,
    ): Response {
        val command = request.toCommand(currentLocale())
        authorizationService.requireRoleManagement(command.tenantId)?.let { return it }

        val result = commandService.deleteRole(command)
        return when (result) {
            is Result.Success -> Response.noContent().build()
            is Result.Failure -> result.failureResponse()
        }
    }

    @GET
    @Operation(summary = "List roles")
    @APIResponses(value = [APIResponse(responseCode = "200", description = "OK")])
    fun listRoles(
        @Valid @BeanParam request: ListRolesRequest,
    ): Response {
        val query = request.toQuery(currentLocale())
        authorizationService.requireRoleRead(query.tenantId)?.let { return it }

        val result = queryService.listRoles(query)
        return when (result) {
            is Result.Success -> Response.ok(result.value.map { it.toResponse() }).build()
            is Result.Failure -> result.failureResponse()
        }
    }

    @GET
    @Operation(summary = "Get role by ID")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(schema = Schema(implementation = RoleResponse::class))],
            ),
            APIResponse(
                responseCode = "404",
                description = "Not found",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
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
    ): Response = block(parseTenantId(tenantIdRaw))

    private fun withTenantAndRole(
        tenantIdRaw: String,
        roleIdRaw: String,
        block: (TenantId, RoleId) -> Response,
    ): Response {
        val tenantId = parseTenantId(tenantIdRaw)
        val roleId = parseRoleId(roleIdRaw)
        return block(tenantId, roleId)
    }

    private fun parseTenantId(raw: String): TenantId =
        runCatching { TenantId.from(raw) }.getOrElse {
            throw IdentityValidationException(
                errorCode = ValidationErrorCode.TENANCY_INVALID_TENANT_ID,
                field = "tenantId",
                rejectedValue = raw,
                locale = currentLocale(),
                message =
                    ValidationMessageResolver.resolve(
                        ValidationErrorCode.TENANCY_INVALID_TENANT_ID,
                        currentLocale(),
                    ),
            )
        }

    private fun parseRoleId(raw: String): RoleId =
        runCatching { RoleId.from(raw) }.getOrElse {
            throw IdentityValidationException(
                errorCode = ValidationErrorCode.TENANCY_INVALID_ROLE_ID,
                field = "roleId",
                rejectedValue = raw,
                locale = currentLocale(),
                message =
                    ValidationMessageResolver.resolve(
                        ValidationErrorCode.TENANCY_INVALID_ROLE_ID,
                        currentLocale(),
                    ),
            )
        }

    private fun currentLocale(): Locale =
        httpHeaders?.language
            ?: httpHeaders?.acceptableLanguages?.firstOrNull()
            ?: Locale.getDefault()

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

@ApplicationScoped
@Path("$IDENTITY_API_V1_PREFIX/tenants/{tenantId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Roles", description = "Role management per tenant")
open class RoleResource() : BaseRoleResource() {
    constructor(
        commandService: IdentityCommandService,
        queryService: IdentityQueryService,
        authorizationService: AuthorizationService,
    ) : this() {
        this.commandService = commandService
        this.queryService = queryService
        this.authorizationService = authorizationService
    }
}

@ApplicationScoped
@Path("$IDENTITY_API_COMPAT_PREFIX/tenants/{tenantId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(
    name = "Roles (legacy)",
    description = "Temporary alias for /api/v1/identity/tenants/{tenantId}/roles",
)
@Deprecated("Use /api/v1/identity/tenants/{tenantId}/roles")
open class LegacyRoleResource() : BaseRoleResource() {
    constructor(
        commandService: IdentityCommandService,
        queryService: IdentityQueryService,
        authorizationService: AuthorizationService,
    ) : this() {
        this.commandService = commandService
        this.queryService = queryService
        this.authorizationService = authorizationService
    }
}
