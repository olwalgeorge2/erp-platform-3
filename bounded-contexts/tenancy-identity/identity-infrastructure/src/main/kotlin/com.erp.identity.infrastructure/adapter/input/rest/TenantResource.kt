package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.identity.application.port.input.query.GetTenantQuery
import com.erp.identity.application.port.input.query.ListTenantsQuery
import com.erp.identity.infrastructure.adapter.input.rest.dto.ActivateTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.ProvisionTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.ResumeTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.SuspendTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.TenantResponse
import com.erp.identity.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.identity.infrastructure.service.IdentityCommandService
import com.erp.identity.infrastructure.service.IdentityQueryService
import com.erp.shared.types.results.Result
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import jakarta.ws.rs.core.Context

@ApplicationScoped
@Path("/api/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TenantResource
    @Inject
    constructor(
        private val commandService: IdentityCommandService,
        private val queryService: IdentityQueryService,
    ) {
        @POST
        fun provisionTenant(
            request: ProvisionTenantRequest,
            @Context uriInfo: UriInfo,
        ): Response =
            when (val result = commandService.provisionTenant(request.toCommand())) {
                is Result.Success -> {
                    val tenant = result.value
                    val location =
                        uriInfo.baseUriBuilder
                            .path("api")
                            .path("tenants")
                            .path(tenant.id.toString())
                            .build()
                    Response.created(location)
                        .entity(tenant.toResponse())
                        .build()
                }
                is Result.Failure -> result.failureResponse()
            }

        @GET
        @Path("/{tenantId}")
        fun getTenant(
            @PathParam("tenantId") tenantIdRaw: String,
        ): Response =
            parseTenantId(tenantIdRaw)
                ?.let { tenantId -> queryService.getTenant(GetTenantQuery(tenantId)) }
                ?.let { result ->
                    when (result) {
                        is Result.Success -> {
                            val tenant = result.value ?: return notFoundResponse(tenantIdRaw)
                            Response.ok(tenant.toResponse()).build()
                        }
                        is Result.Failure -> result.failureResponse()
                    }
                } ?: invalidIdentifierResponse("tenantId", tenantIdRaw)

        @GET
        fun listTenants(
            @QueryParam("status") statusRaw: String?,
            @QueryParam("limit") limit: Int?,
            @QueryParam("offset") offset: Int?,
        ): Response =
            try {
                val status = statusRaw?.let { TenantStatus.valueOf(it.uppercase()) }
                val query =
                    ListTenantsQuery(
                        status = status,
                        limit = limit ?: 50,
                        offset = offset ?: 0,
                    )
                when (val result = queryService.listTenants(query)) {
                    is Result.Success -> Response.ok(result.value.map { it.toResponse() }).build()
                    is Result.Failure -> result.failureResponse()
                }
            } catch (ex: IllegalArgumentException) {
                invalidQueryResponse(ex.message ?: "Invalid query parameters")
            }

        @POST
        @Path("/{tenantId}/activate")
        fun activateTenant(
            @PathParam("tenantId") tenantIdRaw: String,
            request: ActivateTenantRequest,
        ): Response =
            parseTenantId(tenantIdRaw)
                ?.let { tenantId ->
                    commandService.activateTenant(
                        request.toCommand(tenantId.value),
                    )
                }?.toTenantResponse() ?: invalidIdentifierResponse("tenantId", tenantIdRaw)

        @POST
        @Path("/{tenantId}/suspend")
        fun suspendTenant(
            @PathParam("tenantId") tenantIdRaw: String,
            request: SuspendTenantRequest,
        ): Response =
            parseTenantId(tenantIdRaw)
                ?.let { tenantId ->
                    commandService.suspendTenant(
                        request.toCommand(tenantId.value),
                    )
                }?.toTenantResponse() ?: invalidIdentifierResponse("tenantId", tenantIdRaw)

        @POST
        @Path("/{tenantId}/resume")
        fun resumeTenant(
            @PathParam("tenantId") tenantIdRaw: String,
            request: ResumeTenantRequest,
        ): Response =
            parseTenantId(tenantIdRaw)
                ?.let { tenantId ->
                    commandService.resumeTenant(
                        request.toCommand(tenantId.value),
                    )
                }?.toTenantResponse() ?: invalidIdentifierResponse("tenantId", tenantIdRaw)

        private fun parseTenantId(raw: String): TenantId? =
            try {
                TenantId.from(raw)
            } catch (ex: IllegalArgumentException) {
                null
            }

        private fun invalidIdentifierResponse(
            field: String,
            value: String,
        ): Response =
            Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    ErrorResponse(
                        code = "INVALID_IDENTIFIER",
                        message = "Invalid UUID for parameter '$field'",
                        details = mapOf(field to value),
                    ),
                ).build()

        private fun invalidQueryResponse(reason: String): Response =
            Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    ErrorResponse(
                        code = "INVALID_QUERY_PARAMETER",
                        message = reason,
                    ),
                ).build()

        private fun notFoundResponse(tenantId: String): Response =
            Response.status(Response.Status.NOT_FOUND)
                .entity(
                    ErrorResponse(
                        code = "TENANT_NOT_FOUND",
                        message = "Tenant not found",
                        details = mapOf("tenantId" to tenantId),
                    ),
                ).build()

        private fun Result<Tenant>.toTenantResponse(): Response =
            when (this) {
                is Result.Success -> Response.ok(value.toResponse()).build()
                is Result.Failure -> this.failureResponse()
            }
    }
