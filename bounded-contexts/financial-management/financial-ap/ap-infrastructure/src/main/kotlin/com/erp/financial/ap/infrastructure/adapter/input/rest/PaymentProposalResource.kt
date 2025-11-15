package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.PaymentProposalUseCase
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.GeneratePaymentProposalRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PaymentProposalDetailRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PaymentProposalListRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.PaymentProposalResponse
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
@Path("/api/v1/finance/ap/payment-proposals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AP Payment Proposals")
class PaymentProposalResource {
    @Inject
    lateinit var useCase: PaymentProposalUseCase

    @Context
    lateinit var httpHeaders: HttpHeaders

    @POST
    @Operation(summary = "Generate a payment proposal")
    fun generate(
        @Valid request: GeneratePaymentProposalRequest,
    ): Response {
        val proposal = useCase.generateProposal(request.toCommand(currentLocale()))
        return Response.status(Response.Status.CREATED).entity(proposal.toResponse()).build()
    }

    @GET
    @Operation(summary = "List payment proposals")
    fun list(
        @Valid @BeanParam request: PaymentProposalListRequest,
    ): List<PaymentProposalResponse> =
        useCase
            .listProposals(request.toQuery(currentLocale()))
            .map { it.toResponse() }

    @GET
    @Path("/{proposalId}")
    @Operation(summary = "Fetch a single payment proposal")
    fun get(
        @Valid @BeanParam request: PaymentProposalDetailRequest,
    ): Response {
        val locale = currentLocale()
        val tenant = request.tenantId(locale)
        val proposalId = request.proposalId(locale)
        val proposal =
            useCase.getProposal(tenant, proposalId) ?: return Response.status(Response.Status.NOT_FOUND).build()
        return Response.ok(proposal.toResponse()).build()
    }

    private fun currentLocale(): Locale = httpHeaders.preferredLocale()
}
