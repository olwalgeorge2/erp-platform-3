package com.erp.financial.ar.infrastructure.adapter.input.rest

import com.erp.financial.ar.application.port.input.CustomerCommandUseCase
import com.erp.financial.ar.application.port.input.query.CustomerDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomersQuery
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerStatusRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toRegisterCommand
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toStatusCommand
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toUpdateCommand
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
@Path("/api/v1/finance/customers")
@Tag(name = "Customers", description = "Customer master data management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CustomerResource {
    @Inject
    lateinit var customerCommandUseCase: CustomerCommandUseCase

    @POST
    @Operation(summary = "Create a customer")
    fun registerCustomer(request: CustomerRequest): Response {
        val created = customerCommandUseCase.registerCustomer(request.toRegisterCommand())
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @PUT
    @Path("/{customerId}")
    @Operation(summary = "Update a customer profile")
    fun updateCustomer(
        @PathParam("customerId") customerId: UUID,
        request: CustomerRequest,
    ): CustomerResponse =
        customerCommandUseCase.updateCustomer(request.toUpdateCommand(customerId)).toResponse()

    @PUT
    @Path("/{customerId}/status")
    @Operation(summary = "Change customer status")
    fun changeStatus(
        @PathParam("customerId") customerId: UUID,
        request: CustomerStatusRequest,
    ): CustomerResponse =
        customerCommandUseCase.updateCustomerStatus(request.toStatusCommand(customerId)).toResponse()

    @GET
    @Operation(summary = "List customers")
    fun listCustomers(
        @QueryParam("tenantId") tenantId: UUID,
        @QueryParam("companyCodeId") companyCodeId: UUID?,
        @QueryParam("status") status: MasterDataStatus?,
    ): List<CustomerResponse> =
        customerCommandUseCase
            .listCustomers(
                ListCustomersQuery(
                    tenantId = tenantId,
                    companyCodeId = companyCodeId,
                    status = status,
                ),
            ).map { it.toResponse() }

    @GET
    @Path("/{customerId}")
    @Operation(summary = "Fetch a customer by id")
    fun getCustomer(
        @PathParam("customerId") customerId: UUID,
        @QueryParam("tenantId") tenantId: UUID,
    ): Response {
        val result =
            customerCommandUseCase.getCustomer(CustomerDetailQuery(tenantId = tenantId, customerId = customerId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(result.toResponse()).build()
    }

    @DELETE
    @Path("/{customerId}")
    @Operation(summary = "Delete a customer")
    fun deleteCustomer(
        @PathParam("customerId") customerId: UUID,
        @QueryParam("tenantId") tenantId: UUID,
    ): Response {
        customerCommandUseCase.deleteCustomer(tenantId, customerId)
        return Response.noContent().build()
    }
}
