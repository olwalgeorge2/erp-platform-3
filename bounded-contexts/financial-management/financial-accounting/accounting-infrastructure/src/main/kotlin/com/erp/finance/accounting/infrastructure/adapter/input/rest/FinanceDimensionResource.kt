package com.erp.finance.accounting.infrastructure.adapter.input.rest

import com.erp.finance.accounting.application.port.input.DimensionCommandUseCase
import com.erp.finance.accounting.domain.model.CompanyCodeFiscalYearVariant
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.AssignFiscalYearVariantRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.CompanyCodeResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.CreateCompanyCodeRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.DimensionListRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.DimensionPolicyRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.DimensionPolicyResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.DimensionRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.DimensionResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.FiscalYearVariantAssignmentResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.FiscalYearVariantPeriodResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.FiscalYearVariantRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.FiscalYearVariantResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.LinkLedgerRequest
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.PeriodBlackoutResponse
import com.erp.finance.accounting.infrastructure.adapter.input.rest.dto.SchedulePeriodBlackoutRequest
import com.erp.financial.shared.validation.preferredLocale
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.Locale
import java.util.UUID

@ApplicationScoped
@Path("/api/v1/finance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Finance Dimensions", description = "Dimension and company code management")
class FinanceDimensionResource {
    @Inject
    lateinit var dimensionCommandUseCase: DimensionCommandUseCase

    @Context
    var httpHeaders: HttpHeaders? = null

    @POST
    @Path("/company-codes")
    @Operation(summary = "Create a company code")
    fun createCompanyCode(request: CreateCompanyCodeRequest): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(dimensionCommandUseCase.createCompanyCode(request.toCommand()).toCompanyCodeResponse())
            .build()

    @GET
    @Path("/company-codes")
    @Operation(summary = "List company codes for a tenant")
    fun listCompanyCodes(
        @QueryParam("tenantId") tenantId: UUID,
    ): List<CompanyCodeResponse> = dimensionCommandUseCase.listCompanyCodes(tenantId).map { it.toCompanyCodeResponse() }

    @POST
    @Path("/dimensions/{dimensionType}")
    @Operation(summary = "Create a dimension (cost center, profit center, etc.)")
    fun createDimension(
        @PathParam("dimensionType") dimensionType: String,
        request: DimensionRequest,
    ): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(
                dimensionCommandUseCase
                    .upsertDimension(request.toCommand(parseDimensionType(dimensionType, currentLocale())))
                    .toDimensionResponse(),
            ).build()

    @PUT
    @Path("/dimensions/{dimensionType}/{dimensionId}")
    @Operation(summary = "Update a dimension")
    fun updateDimension(
        @PathParam("dimensionType") dimensionType: String,
        @PathParam("dimensionId") dimensionId: UUID,
        request: DimensionRequest,
    ): Response =
        Response
            .ok(
                dimensionCommandUseCase
                    .upsertDimension(
                        request.toCommand(
                            type = parseDimensionType(dimensionType, currentLocale()),
                            dimensionId = dimensionId,
                        ),
                    ).toDimensionResponse(),
            ).build()

    @GET
    @Path("/dimensions/{dimensionType}")
    @Operation(summary = "List dimensions by type")
    fun listDimensions(
        @PathParam("dimensionType") dimensionType: String,
        @QueryParam("tenantId") tenantId: UUID,
        @QueryParam("companyCodeId") companyCodeId: UUID?,
        @QueryParam("status") status: com.erp.finance.accounting.domain.model.DimensionStatus?,
    ): List<DimensionResponse> =
        dimensionCommandUseCase
            .listDimensions(
                DimensionListRequest(
                    tenantId = tenantId,
                    companyCodeId = companyCodeId,
                    status = status,
                ).toQuery(parseDimensionType(dimensionType, currentLocale())),
            ).map { it.toDimensionResponse() }

    @POST
    @Path("/fiscal-year-variants")
    @Operation(summary = "Create a fiscal year variant")
    fun createFiscalYearVariant(request: FiscalYearVariantRequest): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(dimensionCommandUseCase.createFiscalYearVariant(request.toCommand()).toFiscalYearVariantResponse())
            .build()

    @POST
    @Path("/company-codes/{companyCodeId}/fiscal-year-variants")
    @Operation(summary = "Assign a fiscal year variant to a company code")
    fun assignFiscalYearVariant(
        @PathParam("companyCodeId") companyCodeId: UUID,
        request: AssignFiscalYearVariantRequest,
    ): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(
                dimensionCommandUseCase
                    .assignFiscalYearVariant(request.toCommand(companyCodeId))
                    .toFiscalYearVariantAssignmentResponse(),
            ).build()

    @POST
    @Path("/company-codes/{companyCodeId}/period-blackouts")
    @Operation(summary = "Schedule a period blackout window")
    fun schedulePeriodBlackout(
        @PathParam("companyCodeId") companyCodeId: UUID,
        request: SchedulePeriodBlackoutRequest,
    ): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(
                dimensionCommandUseCase
                    .schedulePeriodBlackout(request.toCommand(companyCodeId))
                    .toPeriodBlackoutResponse(),
            ).build()

    @POST
    @Path("/dimension-policies")
    @Operation(summary = "Create or update an account dimension policy")
    fun upsertPolicy(request: DimensionPolicyRequest): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(dimensionCommandUseCase.upsertPolicy(request.toCommand()).toDimensionPolicyResponse())
            .build()

    @GET
    @Path("/dimension-policies")
    @Operation(summary = "List all dimension policies for a tenant")
    fun listPolicies(
        @QueryParam("tenantId") tenantId: UUID,
    ): List<DimensionPolicyResponse> =
        dimensionCommandUseCase.listPolicies(tenantId).map { it.toDimensionPolicyResponse() }

    @POST
    @Path("/company-codes/{companyCodeId}/ledgers")
    @Operation(summary = "Link a ledger to a company code")
    fun linkLedger(
        @PathParam("companyCodeId") companyCodeId: UUID,
        request: LinkLedgerRequest,
    ): Response {
        dimensionCommandUseCase.linkLedgerToCompanyCode(request.toCommand(companyCodeId))
        return Response.status(Response.Status.NO_CONTENT).build()
    }

    private fun currentLocale(): Locale = httpHeaders.preferredLocale()
}

private fun com.erp.finance.accounting.domain.model.CompanyCode.toCompanyCodeResponse(): CompanyCodeResponse =
    CompanyCodeResponse(
        id = id,
        tenantId = tenantId,
        code = code,
        name = name,
        legalEntityName = legalEntityName,
        countryCode = countryCode,
        baseCurrency = baseCurrency,
        timezone = timezone,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun com.erp.finance.accounting.domain.model.AccountingDimension.toDimensionResponse(): DimensionResponse =
    DimensionResponse(
        id = id,
        tenantId = tenantId,
        companyCodeId = companyCodeId,
        type = type,
        code = code,
        name = name,
        description = description,
        parentId = parentId,
        status = status,
        validFrom = validFrom,
        validTo = validTo,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun com.erp.finance.accounting.domain.model.FiscalYearVariant.toFiscalYearVariantResponse():
    FiscalYearVariantResponse =
    FiscalYearVariantResponse(
        id = id,
        tenantId = tenantId,
        code = code,
        name = name,
        description = description,
        startMonth = startMonth,
        calendarPattern = calendarPattern,
        periods =
            periods.map {
                FiscalYearVariantPeriodResponse(
                    periodNumber = it.periodNumber,
                    label = it.label,
                    lengthInDays = it.lengthInDays,
                    adjustment = it.adjustment,
                )
            },
    )

private fun CompanyCodeFiscalYearVariant.toFiscalYearVariantAssignmentResponse(): FiscalYearVariantAssignmentResponse =
    FiscalYearVariantAssignmentResponse(
        companyCodeId = companyCodeId,
        fiscalYearVariantId = fiscalYearVariantId,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
    )

private fun com.erp.finance.accounting.domain.model.PeriodBlackout.toPeriodBlackoutResponse(): PeriodBlackoutResponse =
    PeriodBlackoutResponse(
        id = id,
        companyCodeId = companyCodeId,
        periodCode = periodCode,
        blackoutStart = blackoutStart,
        blackoutEnd = blackoutEnd,
        status = status,
        reason = reason,
    )

private fun com.erp.finance.accounting.domain.model.AccountDimensionPolicy.toDimensionPolicyResponse():
    DimensionPolicyResponse =
    DimensionPolicyResponse(
        id = id,
        tenantId = tenantId,
        accountType = accountType.name,
        dimensionType = dimensionType,
        requirement = requirement,
    )
