package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.BillCommandUseCase
import com.erp.financial.ap.application.port.input.query.ListVendorBillsQuery
import com.erp.financial.ap.application.port.input.query.VendorBillDetailQuery
import com.erp.financial.ap.domain.model.bill.BillStatus
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.BillResponse
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.CreateBillRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PaymentRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PostBillRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toCommand
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toResponse
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
@Path("/api/v1/finance/ap/invoices")
@Tag(name = "AP Invoices", description = "Accounts Payable invoice management")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class VendorBillResource {
    @Inject
    lateinit var billCommandUseCase: BillCommandUseCase

    @POST
    @Operation(summary = "Create a vendor bill")
    fun createBill(@Valid request: CreateBillRequest): Response {
        val created = billCommandUseCase.createBill(request.toCommand())
        return Response.status(Response.Status.CREATED).entity(created.toResponse()).build()
    }

    @POST
    @Path("/{billId}/post")
    @Operation(summary = "Post an AP invoice to the ledger")
    fun postBill(
        @PathParam("billId") billId: UUID,
        @Valid request: PostBillRequest,
    ): BillResponse = billCommandUseCase.postBill(request.toCommand(billId)).toResponse()

    @POST
    @Path("/{billId}/payments")
    @Operation(summary = "Record a payment against an AP invoice")
    fun recordPayment(
        @PathParam("billId") billId: UUID,
        @Valid request: PaymentRequest,
    ): BillResponse = billCommandUseCase.recordPayment(request.toCommand(billId)).toResponse()

    @GET
    @Operation(summary = "List AP invoices")
    fun listBills(
        @QueryParam("tenantId") tenantId: UUID,
        @QueryParam("companyCodeId") companyCodeId: UUID?,
        @QueryParam("vendorId") vendorId: UUID?,
        @QueryParam("status") status: BillStatus?,
        @QueryParam("dueBefore") dueBefore: LocalDate?,
    ): List<BillResponse> =
        billCommandUseCase
            .listBills(
                ListVendorBillsQuery(
                    tenantId = tenantId,
                    companyCodeId = companyCodeId,
                    vendorId = vendorId,
                    status = status,
                    dueBefore = dueBefore,
                ),
            ).map { it.toResponse() }

    @GET
    @Path("/{billId}")
    @Operation(summary = "Fetch an AP invoice by id")
    fun getBill(
        @PathParam("billId") billId: UUID,
        @QueryParam("tenantId") tenantId: UUID,
    ): Response {
        val bill =
            billCommandUseCase.getBill(VendorBillDetailQuery(tenantId = tenantId, billId = billId))
                ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(bill.toResponse()).build()
    }
}
