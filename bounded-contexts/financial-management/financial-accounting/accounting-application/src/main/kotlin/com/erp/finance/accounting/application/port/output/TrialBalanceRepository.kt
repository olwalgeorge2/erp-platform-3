package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionType
import java.util.UUID

data class TrialBalanceRow(
    val accountId: UUID,
    val accountCode: String,
    val accountName: String,
    val accountType: AccountType,
    val debitTotalMinor: Long,
    val creditTotalMinor: Long,
)

data class DimensionSummaryRow(
    val dimensionId: UUID,
    val dimensionCode: String,
    val dimensionName: String,
    val debitTotalMinor: Long,
    val creditTotalMinor: Long,
)

interface TrialBalanceRepository {
    fun fetchTrialBalance(
        tenantId: UUID,
        ledgerId: UUID,
        accountingPeriodId: UUID,
        dimensionFilters: Map<DimensionType, UUID>,
    ): List<TrialBalanceRow>

    fun fetchSummaryByDimension(
        tenantId: UUID,
        ledgerId: UUID,
        accountingPeriodId: UUID,
        dimensionType: DimensionType,
    ): List<DimensionSummaryRow>
}
