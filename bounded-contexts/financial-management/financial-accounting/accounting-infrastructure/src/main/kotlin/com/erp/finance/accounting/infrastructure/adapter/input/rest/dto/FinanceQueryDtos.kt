package com.erp.finance.accounting.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.application.port.input.query.GetLedgerForCompanyCodeQuery
import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryQuery
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceQuery
import com.erp.finance.accounting.domain.model.DimensionType
import com.erp.finance.accounting.infrastructure.adapter.input.rest.parseDimensionType
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.ValidationMessageResolver
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.util.Locale
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
    fun toQuery(locale: Locale): TrialBalanceQuery {
        val filterMap = parseDimensionFilters(locale)
        return TrialBalanceQuery(
            tenantId =
                tenantId ?: throw missingParam(
                    "tenantId",
                    FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID,
                    locale,
                ),
            ledgerId =
                ledgerId ?: throw missingParam(
                    "ledgerId",
                    FinanceValidationErrorCode.FINANCE_INVALID_LEDGER_ID,
                    locale,
                ),
            accountingPeriodId =
                accountingPeriodId
                    ?: throw missingParam(
                        "accountingPeriodId",
                        FinanceValidationErrorCode.FINANCE_INVALID_PERIOD_ID,
                        locale,
                    ),
            dimensionFilters = filterMap,
        )
    }

    private fun parseDimensionFilters(locale: Locale): Map<DimensionType, UUID> {
        val filters = dimensionFilters ?: return emptyMap()
        return filters.associate { token ->
            val parts = token.split(":")
            if (parts.size != 2) {
                throw invalidDimensionFilter(token, locale)
            }
            val type = parseDimensionType(parts[0], locale)
            val value =
                runCatching { UUID.fromString(parts[1]) }
                    .getOrElse { throw invalidDimensionFilter(token, locale) }
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
    fun toQuery(locale: Locale): GlSummaryQuery {
        val rawType =
            dimensionType?.takeIf { it.isNotBlank() }
                ?: throw FinanceValidationException(
                    errorCode = FinanceValidationErrorCode.FINANCE_INVALID_DIMENSION_TYPE,
                    field = "dimensionType",
                    rejectedValue = null,
                    locale = locale,
                    message =
                        ValidationMessageResolver.resolve(
                            FinanceValidationErrorCode.FINANCE_INVALID_DIMENSION_TYPE,
                            locale,
                            "",
                            DimensionType.entries.joinToString(),
                        ),
                )
        val type = parseDimensionType(rawType, locale)

        return GlSummaryQuery(
            tenantId =
                tenantId ?: throw missingParam(
                    "tenantId",
                    FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID,
                    locale,
                ),
            ledgerId =
                ledgerId ?: throw missingParam(
                    "ledgerId",
                    FinanceValidationErrorCode.FINANCE_INVALID_LEDGER_ID,
                    locale,
                ),
            accountingPeriodId =
                accountingPeriodId
                    ?: throw missingParam(
                        "accountingPeriodId",
                        FinanceValidationErrorCode.FINANCE_INVALID_PERIOD_ID,
                        locale,
                    ),
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
    fun toQuery(locale: Locale) =
        GetLedgerForCompanyCodeQuery(
            companyCodeId =
                companyCodeId
                    ?: throw missingParam(
                        "companyCodeId",
                        FinanceValidationErrorCode.FINANCE_INVALID_COMPANY_CODE_ID,
                        locale,
                    ),
            tenantId =
                tenantId ?: throw missingParam(
                    "tenantId",
                    FinanceValidationErrorCode.FINANCE_INVALID_TENANT_ID,
                    locale,
                ),
        )
}

private fun missingParam(
    field: String,
    code: FinanceValidationErrorCode,
    locale: Locale,
): FinanceValidationException =
    FinanceValidationException(
        errorCode = code,
        field = field,
        rejectedValue = null,
        locale = locale,
        message = ValidationMessageResolver.resolve(code, locale, "<missing>"),
    )

private fun invalidDimensionFilter(
    token: String,
    locale: Locale,
): FinanceValidationException =
    FinanceValidationException(
        errorCode = FinanceValidationErrorCode.FINANCE_INVALID_DIMENSION_FILTER,
        field = "dimensionFilter",
        rejectedValue = token,
        locale = locale,
        message =
            ValidationMessageResolver.resolve(
                FinanceValidationErrorCode.FINANCE_INVALID_DIMENSION_FILTER,
                locale,
                token,
            ),
    )
