package com.erp.finance.accounting.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.application.port.input.query.GetLedgerForCompanyCodeQuery
import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryQuery
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceQuery
import com.erp.finance.accounting.domain.model.DimensionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.util.UUID

data class TrialBalanceRequest(
    @field:NotNull
    @field:PathParam("ledgerId")
    var ledgerId: UUID? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:NotNull
    @field:QueryParam("accountingPeriodId")
    var accountingPeriodId: UUID? = null,
    @field:QueryParam("dimensionFilter")
    var dimensionFilters: List<String>? = null,
) {
    fun toQuery(): TrialBalanceQuery {
        val filterMap = parseDimensionFilters()
        return TrialBalanceQuery(
            tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
            ledgerId = ledgerId ?: throw BadRequestException("ledgerId is required"),
            accountingPeriodId = accountingPeriodId ?: throw BadRequestException("accountingPeriodId is required"),
            dimensionFilters = filterMap,
        )
    }

    private fun parseDimensionFilters(): Map<DimensionType, UUID> {
        val filters = dimensionFilters ?: return emptyMap()
        return filters.associate { token ->
            val parts = token.split(":")
            if (parts.size != 2) {
                throw BadRequestException("dimensionFilter must be formatted as DIMENSION_TYPE:uuid, got: $token")
            }
            val type =
                try {
                    DimensionType.valueOf(parts[0].uppercase())
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException(
                        "Invalid dimension type: '${parts[0]}'. Valid values: ${DimensionType.entries.joinToString()}",
                    )
                }
            val value =
                try {
                    UUID.fromString(parts[1])
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("Invalid UUID in dimensionFilter: '${parts[1]}'")
                }
            type to value
        }
    }
}

data class GlSummaryRequest(
    @field:NotNull
    @field:PathParam("ledgerId")
    var ledgerId: UUID? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
    @field:NotNull
    @field:QueryParam("accountingPeriodId")
    var accountingPeriodId: UUID? = null,
    @field:NotBlank
    @field:QueryParam("dimensionType")
    var dimensionType: String? = null,
) {
    fun toQuery(): GlSummaryQuery {
        val type =
            try {
                DimensionType.valueOf(dimensionType!!.uppercase())
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(
                    "Invalid dimensionType: '$dimensionType'. Valid values: ${DimensionType.entries.joinToString()}",
                )
            }

        return GlSummaryQuery(
            tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
            ledgerId = ledgerId ?: throw BadRequestException("ledgerId is required"),
            accountingPeriodId = accountingPeriodId ?: throw BadRequestException("accountingPeriodId is required"),
            dimensionType = type,
        )
    }
}

data class LedgerInfoRequest(
    @field:NotNull
    @field:PathParam("companyCodeId")
    var companyCodeId: UUID? = null,
    @field:NotNull
    @field:QueryParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun toQuery() =
        GetLedgerForCompanyCodeQuery(
            companyCodeId = companyCodeId ?: throw BadRequestException("companyCodeId is required"),
            tenantId = tenantId ?: throw BadRequestException("tenantId is required"),
        )
}
