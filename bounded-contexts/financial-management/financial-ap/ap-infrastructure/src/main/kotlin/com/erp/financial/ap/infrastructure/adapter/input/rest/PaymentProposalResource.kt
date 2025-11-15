package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.PaymentProposalUseCase
import com.erp.financial.ap.application.port.output.ListPaymentProposalsQuery
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.GeneratePaymentProposalRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PaymentProposalResponse
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PaymentProposalSearchRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toCommand
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toQuery
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@ApplicationScoped
@Path("/api/v1/finance/ap/payment-proposals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AP Payment Proposals")
class PaymentProposalResource {
    @Inject
    lateinit var useCase: PaymentProposalUseCase

    @POST
    @Operation(summary = "Generate a payment proposal")
    fun generate(request: GeneratePaymentProposalRequest): Response {
        val proposal = useCase.generateProposal(request.toCommand())
        return Response.status(Response.Status.CREATED).entity(proposal.toResponse()).build()
    }

    @GET
    @Operation(summary = "List payment proposals")
    fun list(
        @jakarta.ws.rs.QueryParam("tenantId") tenantId: UUID,
        @jakarta.ws.rs.QueryParam("companyCodeId") companyCodeId: UUID?,
        @jakarta.ws.rs.QueryParam("status") status: String?,
    ): List<PaymentProposalResponse> {
        val statusEnum =
            status?.let {
                com.erp.financial.ap.domain.model.paymentproposal.PaymentProposalStatus.valueOf(it)
            }
        val search =
            PaymentProposalSearchRequest(
                tenantId = tenantId,
                companyCodeId = companyCodeId,
                status = statusEnum,
            )
        return useCase.listProposals(search.toQuery()).map { it.toResponse() }
    }

    @GET
    @Path("/{proposalId}")
    @Operation(summary = "Fetch a single payment proposal")
    fun get(
        @PathParam("proposalId") proposalId: UUID,
        @jakarta.ws.rs.QueryParam("tenantId") tenantId: UUID,
    ): Response {
        val proposal = useCase.getProposal(tenantId, proposalId) ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(proposal.toResponse()).build()
    }
}
