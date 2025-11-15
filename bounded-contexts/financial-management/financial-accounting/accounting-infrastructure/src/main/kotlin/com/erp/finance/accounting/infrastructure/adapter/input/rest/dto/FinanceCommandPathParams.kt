package com.erp.finance.accounting.infrastructure.adapter.input.rest.dto

import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.parseUuidParam
import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.PathParam
import java.util.Locale
import java.util.UUID

data class ChartPathParams(
    @field:NotBlank
    @field:PathParam("chartId")
    var chartId: String? = null,
) {
    fun chartId(locale: Locale): UUID =
        parseUuidParam(
            raw = chartId ?: "",
            field = "chartId",
            code = FinanceValidationErrorCode.FINANCE_INVALID_CHART_ID,
            locale = locale,
        )
}

data class LedgerPeriodPathParams(
    @field:NotBlank
    @field:PathParam("ledgerId")
    var ledgerId: String? = null,
    @field:NotBlank
    @field:PathParam("periodId")
    var periodId: String? = null,
) {
    fun ledgerId(locale: Locale): UUID =
        parseUuidParam(
            raw = ledgerId ?: "",
            field = "ledgerId",
            code = FinanceValidationErrorCode.FINANCE_INVALID_LEDGER_ID,
            locale = locale,
        )

    fun periodId(locale: Locale): UUID =
        parseUuidParam(
            raw = periodId ?: "",
            field = "periodId",
            code = FinanceValidationErrorCode.FINANCE_INVALID_PERIOD_ID,
            locale = locale,
        )
}
