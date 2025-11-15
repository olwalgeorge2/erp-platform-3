package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.VendorCommandUseCase
import com.erp.financial.ap.application.port.input.query.VendorDetailQuery
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorListRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorPathParams
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorResponse
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorScopedRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorStatusRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toRegisterCommand
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toStatusCommand
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toUpdateCommand
import com.erp.financial.shared.validation.preferredLocale
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
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.Locale

@ApplicationScoped
@Path("/api/v1/finance/vendors")
@Tag(name = "Vendors", description = "Vendor master data management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class VendorResource {
    @Inject
    lateinit var vendorCommandUseCase: VendorCommandUseCase

    @Context
    lateinit var httpHeaders: HttpHeaders

    @POST
    @Operation(summary = "Register a vendor")
    fun registerVendor(
        @Valid request: VendorRequest,
    ): Response {
        val created = vendorCommandUseCase.registerVendor(request.toRegisterCommand(currentLocale()))
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @PUT
    @Path("/{vendorId}")
    @Operation(summary = "Update a vendor")
    fun updateVendor(
        @Valid @BeanParam vendorPath: VendorPathParams,
        @Valid request: VendorRequest,
    ): VendorResponse {
        val locale = currentLocale()
        val vendorId = vendorPath.vendorId(locale)
        return vendorCommandUseCase.updateVendor(request.toUpdateCommand(vendorId, locale)).toResponse()
    }

    @PUT
    @Path("/{vendorId}/status")
    @Operation(summary = "Activate or deactivate a vendor")
    fun updateVendorStatus(
        @Valid @BeanParam vendorPath: VendorPathParams,
        @Valid request: VendorStatusRequest,
    ): VendorResponse {
        val locale = currentLocale()
        val vendorId = vendorPath.vendorId(locale)
        return vendorCommandUseCase.updateVendorStatus(request.toStatusCommand(vendorId)).toResponse()
    }

    @GET
    @Operation(summary = "List vendors")
    fun listVendors(
        @Valid @BeanParam request: VendorListRequest,
    ): List<VendorResponse> =
        vendorCommandUseCase
            .listVendors(request.toQuery(currentLocale()))
            .map { it.toResponse() }

    @GET
    @Path("/{vendorId}")
    @Operation(summary = "Fetch a vendor by id")
    fun getVendor(
        @Valid @BeanParam request: VendorScopedRequest,
    ): Response {
        val locale = currentLocale()
        val tenant = request.tenantId(locale)
        val vendorId = request.vendorId(locale)
        val vendor =
            vendorCommandUseCase.getVendor(VendorDetailQuery(tenantId = tenant, vendorId = vendorId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(vendor.toResponse()).build()
    }

    @DELETE
    @Path("/{vendorId}")
    @Operation(summary = "Delete a vendor (hard delete)")
    fun deleteVendor(
        @Valid @BeanParam request: VendorScopedRequest,
    ): Response {
        val locale = currentLocale()
        val tenant = request.tenantId(locale)
        val vendorId = request.vendorId(locale)
        vendorCommandUseCase.deleteVendor(tenant, vendorId)
        return Response.noContent().build()
    }

    private fun currentLocale(): Locale = httpHeaders.preferredLocale()
}
