package com.erp.finance.accounting.infrastructure.adapter.input.rest

import com.erp.finance.accounting.application.port.input.query.FinanceQueryUseCase
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.GlSummaryRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.GlSummaryResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.LedgerInfoRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.LedgerPeriodInfoResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.TrialBalanceRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.TrialBalanceResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.toResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@ApplicationScoped
@Path("/api/v1/finance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Finance Queries", description = "Read/query endpoints for finance")
class FinanceQueryResource {
    @Inject
    lateinit var queryService: FinanceQueryUseCase

    @GET
    @Path("/ledgers/{ledgerId}/trial-balance")
    @Operation(summary = "Get trial balance for a ledger and period")
    fun getTrialBalance(
        @Valid @BeanParam request: TrialBalanceRequest,
    ): TrialBalanceResponse =
        queryService
            .getTrialBalance(request.toQuery())
            .toResponse()

    @GET
    @Path("/ledgers/{ledgerId}/dimension-summary")
    @Operation(summary = "Get GL summary grouped by dimension type")
    fun getGlSummary(
        @Valid @BeanParam request: GlSummaryRequest,
    ): GlSummaryResponse =
        queryService
            .getGlSummary(request.toQuery())
            .toResponse()

    @GET
    @Path("/company-codes/{companyCodeId}/ledger-info")
    @Operation(summary = "Resolve ledger and current open period for a company code")
    fun getLedgerInfo(
        @Valid @BeanParam request: LedgerInfoRequest,
    ): LedgerPeriodInfoResponse =
        queryService
            .getLedgerAndPeriodForCompanyCode(request.toQuery())
            ?.toResponse()
            ?: throw NotFoundException("No ledger mapping found for company code ${request.companyCodeId}")
}
