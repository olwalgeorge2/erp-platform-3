package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.VendorCommandUseCase
import com.erp.financial.ap.application.port.input.query.ListVendorsQuery
import com.erp.financial.ap.application.port.input.query.VendorDetailQuery
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorResponse
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.VendorStatusRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toRegisterCommand
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toStatusCommand
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toUpdateCommand
import com.erp.financial.shared.masterdata.MasterDataStatus
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
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@ApplicationScoped
@Path("/api/v1/finance/vendors")
@Tag(name = "Vendors", description = "Vendor master data management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class VendorResource {
    @Inject
    lateinit var vendorCommandUseCase: VendorCommandUseCase

    @POST
    @Operation(summary = "Register a vendor")
    fun registerVendor(request: VendorRequest): Response {
        val created = vendorCommandUseCase.registerVendor(request.toRegisterCommand())
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @PUT
    @Path("/{vendorId}")
    @Operation(summary = "Update a vendor")
    fun updateVendor(
        @PathParam("vendorId") vendorId: UUID,
        request: VendorRequest,
    ): VendorResponse = vendorCommandUseCase.updateVendor(request.toUpdateCommand(vendorId)).toResponse()

    @PUT
    @Path("/{vendorId}/status")
    @Operation(summary = "Activate or deactivate a vendor")
    fun updateVendorStatus(
        @PathParam("vendorId") vendorId: UUID,
        request: VendorStatusRequest,
    ): VendorResponse = vendorCommandUseCase.updateVendorStatus(request.toStatusCommand(vendorId)).toResponse()

    @GET
    @Operation(summary = "List vendors")
    fun listVendors(
        @QueryParam("tenantId") tenantId: UUID,
        @QueryParam("companyCodeId") companyCodeId: UUID?,
        @QueryParam("status") status: MasterDataStatus?,
    ): List<VendorResponse> =
        vendorCommandUseCase
            .listVendors(
                ListVendorsQuery(
                    tenantId = tenantId,
                    companyCodeId = companyCodeId,
                    status = status,
                ),
            ).map { it.toResponse() }

    @GET
    @Path("/{vendorId}")
    @Operation(summary = "Fetch a vendor by id")
    fun getVendor(
        @PathParam("vendorId") vendorId: UUID,
        @QueryParam("tenantId") tenantId: UUID,
    ): Response {
        val vendor =
            vendorCommandUseCase.getVendor(VendorDetailQuery(tenantId = tenantId, vendorId = vendorId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(vendor.toResponse()).build()
    }

    @DELETE
    @Path("/{vendorId}")
    @Operation(summary = "Delete a vendor (hard delete)")
    fun deleteVendor(
        @PathParam("vendorId") vendorId: UUID,
        @QueryParam("tenantId") tenantId: UUID,
    ): Response {
        vendorCommandUseCase.deleteVendor(tenantId, vendorId)
        return Response.noContent().build()
    }
}
