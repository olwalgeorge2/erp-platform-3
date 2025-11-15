package com.erp.financial.ar.infrastructure.adapter.input.rest

import com.erp.financial.ar.application.port.input.CustomerCommandUseCase
import com.erp.financial.ar.application.port.input.query.CustomerDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomersQuery
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerIdPathParams
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerListRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerScopedRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerStatusRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toRegisterCommand
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toStatusCommand
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toUpdateCommand
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
@Path("/api/v1/finance/customers")
@Tag(name = "Customers", description = "Customer master data management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CustomerResource {
    @Inject
    lateinit var customerCommandUseCase: CustomerCommandUseCase

    @Context
    lateinit var httpHeaders: HttpHeaders

    @POST
    @Operation(summary = "Create a customer")
    fun registerCustomer(@Valid request: CustomerRequest): Response {
        val created = customerCommandUseCase.registerCustomer(request.toRegisterCommand(currentLocale()))
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @PUT
    @Path("/{customerId}")
    @Operation(summary = "Update a customer profile")
    fun updateCustomer(
        @Valid @BeanParam customerPath: CustomerIdPathParams,
        @Valid request: CustomerRequest,
    ): CustomerResponse =
        currentLocale().let { locale ->
            val customerId = customerPath.customerId(locale)
            customerCommandUseCase.updateCustomer(request.toUpdateCommand(customerId, locale)).toResponse()
        }

    @PUT
    @Path("/{customerId}/status")
    @Operation(summary = "Change customer status")
    fun changeStatus(
        @Valid @BeanParam customerPath: CustomerIdPathParams,
        @Valid request: CustomerStatusRequest,
    ): CustomerResponse =
        currentLocale().let { locale ->
            val customerId = customerPath.customerId(locale)
            customerCommandUseCase.updateCustomerStatus(request.toStatusCommand(customerId)).toResponse()
        }

    @GET
    @Operation(summary = "List customers")
    fun listCustomers(@Valid @BeanParam request: CustomerListRequest): List<CustomerResponse> =
        customerCommandUseCase
            .listCustomers(request.toQuery(currentLocale()))
            .map { it.toResponse() }

    @GET
    @Path("/{customerId}")
    @Operation(summary = "Fetch a customer by id")
    fun getCustomer(@Valid @BeanParam request: CustomerScopedRequest): Response {
        val locale = currentLocale()
        val customerId = request.customerId(locale)
        val tenant = request.tenantId(locale)
        val result =
            customerCommandUseCase.getCustomer(CustomerDetailQuery(tenantId = tenant, customerId = customerId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(result.toResponse()).build()
    }

    @DELETE
    @Path("/{customerId}")
    @Operation(summary = "Delete a customer")
    fun deleteCustomer(@Valid @BeanParam request: CustomerScopedRequest): Response {
        val locale = currentLocale()
        val customerId = request.customerId(locale)
        val tenant = request.tenantId(locale)
        customerCommandUseCase.deleteCustomer(tenant, customerId)
        return Response.noContent().build()
    }

    private fun currentLocale(): Locale = httpHeaders.preferredLocale()
}
