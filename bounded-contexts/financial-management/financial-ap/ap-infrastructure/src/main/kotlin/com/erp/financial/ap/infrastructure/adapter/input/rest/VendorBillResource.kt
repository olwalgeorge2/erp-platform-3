package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.BillCommandUseCase
import com.erp.financial.ap.application.port.input.query.VendorBillDetailQuery
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.BillListRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.BillPathParams
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.BillResponse
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.BillScopedRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.CreateBillRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PaymentRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PostBillRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toCommand
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toResponse
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
@Path("/api/v1/finance/ap/invoices")
@Tag(name = "AP Invoices", description = "Accounts Payable invoice management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class VendorBillResource {
    @Inject
    lateinit var billCommandUseCase: BillCommandUseCase

    @Context
    lateinit var httpHeaders: HttpHeaders

    @POST
    @Operation(summary = "Create a vendor bill")
    fun createBill(
        @Valid request: CreateBillRequest,
    ): Response {
        val created = billCommandUseCase.createBill(request.toCommand(currentLocale()))
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @POST
    @Path("/{billId}/post")
    @Operation(summary = "Post an AP invoice to the ledger")
    fun postBill(
        @Valid @BeanParam pathParams: BillPathParams,
        @Valid request: PostBillRequest,
    ): BillResponse {
        val billId = pathParams.billId(currentLocale())
        return billCommandUseCase.postBill(request.toCommand(billId)).toResponse()
    }

    @POST
    @Path("/{billId}/payments")
    @Operation(summary = "Record a payment against an AP invoice")
    fun recordPayment(
        @Valid @BeanParam pathParams: BillPathParams,
        @Valid request: PaymentRequest,
    ): BillResponse {
        val locale = currentLocale()
        val billId = pathParams.billId(locale)
        return billCommandUseCase.recordPayment(request.toCommand(billId, locale)).toResponse()
    }

    @GET
    @Operation(summary = "List AP invoices")
    fun listBills(
        @Valid @BeanParam request: BillListRequest,
    ): List<BillResponse> =
        billCommandUseCase
            .listBills(request.toQuery(currentLocale()))
            .map { it.toResponse() }

    @GET
    @Path("/{billId}")
    @Operation(summary = "Fetch an AP invoice by id")
    fun getBill(
        @Valid @BeanParam request: BillScopedRequest,
    ): Response {
        val locale = currentLocale()
        val tenant = request.tenantId(locale)
        val billId = request.billId(locale)
        val bill =
            billCommandUseCase.getBill(VendorBillDetailQuery(tenantId = tenant, billId = billId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(bill.toResponse()).build()
    }

    private fun currentLocale(): Locale = httpHeaders.preferredLocale()
}
