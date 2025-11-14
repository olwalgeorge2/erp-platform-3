package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.adapter.input.rest.dto.ActivateUserRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.AssignRoleRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.AuthenticateRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.CreateUserRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.ReactivateUserRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.ResetPasswordRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.SuspendUserRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.UpdateCredentialsRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.UserResponse
import com.erp.identity.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.identity.infrastructure.service.IdentityCommandService
import com.erp.shared.types.results.Result
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
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
import java.util.UUID

open class BaseAuthResource() {
    @Inject
    protected lateinit var commandService: IdentityCommandService

    constructor(commandService: IdentityCommandService) : this() {
        this.commandService = commandService
    }

    @POST
    @Operation(summary = "Create user")
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "201",
                description = "User created",
                content = [
                    Content(
                        schema = Schema(implementation = UserResponse::class),
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
    @Path("/users")
    fun createUser(
        @RequestBody(description = "Create user payload", required = true)
        request: CreateUserRequest,
        uriInfo: UriInfo,
    ): Response =
        when (val result = commandService.createUser(request.toCommand())) {
            is Result.Success -> {
                val user = result.value
                val location =
                    uriInfo
                        .absolutePathBuilder
                        .path(user.id.toString())
                        .build()
                Response
                    .created(location)
                    .entity(user.toResponse())
                    .build()
            }
            is Result.Failure -> result.failureResponse()
        }

    @POST
    @Operation(summary = "Authenticate user")
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Authenticated"),
            APIResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @Path("/login")
    fun authenticate(
        @RequestBody(description = "Authenticate payload", required = true)
        request: AuthenticateRequest,
    ): Response =
        when (val result = commandService.authenticate(request.toCommand())) {
            is Result.Success -> Response.ok(result.value.toResponse()).build()
            is Result.Failure -> result.failureResponse()
        }

    @POST
    @Operation(summary = "Assign role to user")
    @APIResponses(
        value = [
            APIResponse(responseCode = "200", description = "Assigned"),
            APIResponse(
                responseCode = "400",
                description = "Invalid identifier or request",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))],
            ),
        ],
    )
    @Path("/users/{userId}/roles")
    fun assignRole(
        @PathParam("userId") userIdRaw: String,
        @RequestBody(description = "Assign role payload", required = true)
        request: AssignRoleRequest,
    ): Response =
        parseUuid(userIdRaw)
            ?.let { userId -> commandService.assignRole(request.toCommand(userId)) }
            ?.let { result ->
                when (result) {
                    is Result.Success -> Response.ok(result.value.toResponse()).build()
                    is Result.Failure -> result.failureResponse()
                }
            } ?: invalidUuidResponse("userId", userIdRaw)

    @POST
    @Operation(summary = "Activate user")
    @APIResponses(value = [APIResponse(responseCode = "200", description = "Activated")])
    @Path("/users/{userId}/activate")
    fun activateUser(
        @PathParam("userId") userIdRaw: String,
        @RequestBody(description = "Activation payload", required = true)
        request: ActivateUserRequest,
    ): Response =
        parseUuid(userIdRaw)
            ?.let { userId -> commandService.activateUser(request.toCommand(userId)) }
            ?.let { result ->
                when (result) {
                    is Result.Success -> Response.ok(result.value.toResponse()).build()
                    is Result.Failure -> result.failureResponse()
                }
            } ?: invalidUuidResponse("userId", userIdRaw)

    @PUT
    @Operation(summary = "Update user credentials")
    @APIResponses(value = [APIResponse(responseCode = "200", description = "Updated")])
    @Path("/users/{userId}/credentials")
    fun updateCredentials(
        @PathParam("userId") userIdRaw: String,
        @RequestBody(description = "Update credentials payload", required = true)
        request: UpdateCredentialsRequest,
    ): Response =
        parseUuid(userIdRaw)
            ?.let { userId -> commandService.updateCredentials(request.toCommand(userId)) }
            ?.let { result ->
                when (result) {
                    is Result.Success -> Response.ok(result.value.toResponse()).build()
                    is Result.Failure -> result.failureResponse()
                }
            } ?: invalidUuidResponse("userId", userIdRaw)

    @POST
    @Operation(summary = "Reset user password (admin)")
    @APIResponses(value = [APIResponse(responseCode = "200", description = "Password reset")])
    @Path("/users/{userId}/reset-password")
    fun resetPassword(
        @PathParam("userId") userIdRaw: String,
        @RequestBody(description = "Reset password payload", required = true)
        request: ResetPasswordRequest,
    ): Response =
        parseUuid(userIdRaw)
            ?.let { userId -> commandService.resetPassword(request.toCommand(userId)) }
            ?.let { result ->
                when (result) {
                    is Result.Success -> Response.ok(result.value.toResponse()).build()
                    is Result.Failure -> result.failureResponse()
                }
            } ?: invalidUuidResponse("userId", userIdRaw)

    @POST
    @Operation(summary = "Suspend user")
    @APIResponses(value = [APIResponse(responseCode = "200", description = "Suspended")])
    @Path("/users/{userId}/suspend")
    fun suspendUser(
        @PathParam("userId") userIdRaw: String,
        @RequestBody(description = "Suspend payload", required = true)
        request: SuspendUserRequest,
    ): Response =
        parseUuid(userIdRaw)
            ?.let { userId -> commandService.suspendUser(request.toCommand(userId)) }
            ?.let { result ->
                when (result) {
                    is Result.Success -> Response.ok(result.value.toResponse()).build()
                    is Result.Failure -> result.failureResponse()
                }
            } ?: invalidUuidResponse("userId", userIdRaw)

    @POST
    @Operation(summary = "Reactivate user")
    @APIResponses(value = [APIResponse(responseCode = "200", description = "Reactivated")])
    @Path("/users/{userId}/reactivate")
    fun reactivateUser(
        @PathParam("userId") userIdRaw: String,
        @RequestBody(description = "Reactivation payload", required = true)
        request: ReactivateUserRequest,
    ): Response =
        parseUuid(userIdRaw)
            ?.let { userId -> commandService.reactivateUser(request.toCommand(userId)) }
            ?.let { result ->
                when (result) {
                    is Result.Success -> Response.ok(result.value.toResponse()).build()
                    is Result.Failure -> result.failureResponse()
                }
            } ?: invalidUuidResponse("userId", userIdRaw)

    private fun parseUuid(value: String): UUID? =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            null
        }

    private fun invalidUuidResponse(
        field: String,
        value: String,
    ): Response =
        Response
            .status(Response.Status.BAD_REQUEST)
            .entity(
                ErrorResponse(
                    code = "INVALID_IDENTIFIER",
                    message = "Invalid UUID for parameter '$field'",
                    details = mapOf(field to value),
                ),
            ).build()
}

@ApplicationScoped
@Path("$IDENTITY_API_V1_PREFIX/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Authentication and user lifecycle endpoints")
open class AuthResource() : BaseAuthResource() {
    constructor(commandService: IdentityCommandService) : this() {
        this.commandService = commandService
    }
}

@ApplicationScoped
@Path("$IDENTITY_API_COMPAT_PREFIX/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(
    name = "Auth (legacy)",
    description = "Temporary alias for /api/v1/identity/auth - will be removed after clients migrate",
)
@Deprecated("Use /api/v1/identity/auth")
open class LegacyAuthResource() : BaseAuthResource() {
    constructor(commandService: IdentityCommandService) : this() {
        this.commandService = commandService
    }
}
