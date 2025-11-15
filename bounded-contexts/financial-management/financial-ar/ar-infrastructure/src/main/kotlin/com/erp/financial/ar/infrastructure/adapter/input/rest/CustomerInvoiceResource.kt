package com.erp.financial.ar.infrastructure.adapter.input.rest

import com.erp.financial.ar.application.port.input.CustomerInvoiceUseCase
import com.erp.financial.ar.application.port.input.query.CustomerInvoiceDetailQuery
import com.erp.financial.ar.application.port.input.query.ListCustomerInvoicesQuery
import com.erp.financial.ar.domain.model.invoice.CustomerInvoiceStatus
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CreateCustomerInvoiceRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerInvoiceResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerInvoiceSearchRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.PostCustomerInvoiceRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.ReceiptRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toCommand
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toQuery
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
@Path("/api/v1/finance/ar/invoices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AR Invoices")
class CustomerInvoiceResource {
    @Inject
    lateinit var useCase: CustomerInvoiceUseCase

    @POST
    @Operation(summary = "Create a customer invoice")
    fun create(@Valid request: CreateCustomerInvoiceRequest): Response {
        val created = useCase.createInvoice(request.toCommand())
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @POST
    @Path("/{invoiceId}/post")
    @Operation(summary = "Post a customer invoice to the ledger")
    fun post(
        @PathParam("invoiceId") invoiceId: UUID,
        @Valid request: PostCustomerInvoiceRequest,
    ): CustomerInvoiceResponse = useCase.postInvoice(request.toCommand(invoiceId)).toResponse()

    @POST
    @Path("/{invoiceId}/receipts")
    @Operation(summary = "Record a receipt against a customer invoice")
    fun recordReceipt(
        @PathParam("invoiceId") invoiceId: UUID,
        @Valid request: ReceiptRequest,
    ): CustomerInvoiceResponse = useCase.recordReceipt(request.toCommand(invoiceId)).toResponse()

    @GET
    @Operation(summary = "List customer invoices")
    fun list(
        @QueryParam("tenantId") tenantId: UUID,
        @QueryParam("companyCodeId") companyCodeId: UUID?,
        @QueryParam("customerId") customerId: UUID?,
        @QueryParam("status") status: CustomerInvoiceStatus?,
        @QueryParam("dueBefore") dueBefore: LocalDate?,
    ): List<CustomerInvoiceResponse> =
        useCase
            .listInvoices(
                ListCustomerInvoicesQuery(
                    tenantId = tenantId,
                    companyCodeId = companyCodeId,
                    customerId = customerId,
                    status = status,
                    dueBefore = dueBefore,
                ),
            ).map { it.toResponse() }

    @GET
    @Path("/{invoiceId}")
    @Operation(summary = "Fetch an AR invoice by id")
    fun get(
        @PathParam("invoiceId") invoiceId: UUID,
        @QueryParam("tenantId") tenantId: UUID,
    ): Response {
        val invoice =
            useCase.getInvoice(CustomerInvoiceDetailQuery(tenantId = tenantId, invoiceId = invoiceId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(invoice.toResponse()).build()
    }
}
