package com.erp.finance.accounting.application.port.input.query

import com.erp.finance.accounting.application.port.input.dto.LedgerPeriodInfoDto
import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryQuery
import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryResult
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceQuery
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceResult

interface FinanceQueryUseCase {
    fun getTrialBalance(query: TrialBalanceQuery): TrialBalanceResult

    fun getGlSummary(query: GlSummaryQuery): GlSummaryResult

    fun getLedgerAndPeriodForCompanyCode(query: GetLedgerForCompanyCodeQuery): LedgerPeriodInfoDto?
}
