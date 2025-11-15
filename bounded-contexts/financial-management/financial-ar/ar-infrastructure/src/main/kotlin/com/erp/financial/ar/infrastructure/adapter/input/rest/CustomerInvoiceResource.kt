package com.erp.financial.ar.infrastructure.adapter.input.rest

import com.erp.financial.ar.application.port.input.CustomerInvoiceUseCase
import com.erp.financial.ar.application.port.input.query.CustomerInvoiceDetailQuery
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CreateCustomerInvoiceRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerInvoiceListRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerInvoicePathParams
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerInvoiceResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.CustomerInvoiceScopedRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.PostCustomerInvoiceRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.ReceiptRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toCommand
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toQuery
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.financial.shared.validation.preferredLocale
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
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
@Path("/api/v1/finance/ar/invoices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AR Invoices")
class CustomerInvoiceResource {
    @Inject
    lateinit var useCase: CustomerInvoiceUseCase

    @Context
    lateinit var httpHeaders: HttpHeaders

    @POST
    @Operation(summary = "Create a customer invoice")
    fun create(
        @Valid request: CreateCustomerInvoiceRequest,
    ): Response {
        val created = useCase.createInvoice(request.toCommand(currentLocale()))
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @POST
    @Path("/{invoiceId}/post")
    @Operation(summary = "Post a customer invoice to the ledger")
    fun post(
        @Valid @BeanParam pathParams: CustomerInvoicePathParams,
        @Valid request: PostCustomerInvoiceRequest,
    ): CustomerInvoiceResponse {
        val invoiceId = pathParams.invoiceId(currentLocale())
        return useCase.postInvoice(request.toCommand(invoiceId)).toResponse()
    }

    @POST
    @Path("/{invoiceId}/receipts")
    @Operation(summary = "Record a receipt against a customer invoice")
    fun recordReceipt(
        @Valid @BeanParam pathParams: CustomerInvoicePathParams,
        @Valid request: ReceiptRequest,
    ): CustomerInvoiceResponse {
        val locale = currentLocale()
        val invoiceId = pathParams.invoiceId(locale)
        return useCase.recordReceipt(request.toCommand(invoiceId, locale)).toResponse()
    }

    @GET
    @Operation(summary = "List customer invoices")
    fun list(
        @Valid @BeanParam request: CustomerInvoiceListRequest,
    ): List<CustomerInvoiceResponse> =
        useCase
            .listInvoices(request.toQuery(currentLocale()))
            .map { it.toResponse() }

    @GET
    @Path("/{invoiceId}")
    @Operation(summary = "Fetch an AR invoice by id")
    fun get(
        @Valid @BeanParam request: CustomerInvoiceScopedRequest,
    ): Response {
        val locale = currentLocale()
        val invoiceId = request.invoiceId(locale)
        val tenant = request.tenantId(locale)
        val invoice =
            useCase.getInvoice(CustomerInvoiceDetailQuery(tenantId = tenant, invoiceId = invoiceId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(invoice.toResponse()).build()
    }

    private fun currentLocale(): Locale = httpHeaders.preferredLocale()
}
