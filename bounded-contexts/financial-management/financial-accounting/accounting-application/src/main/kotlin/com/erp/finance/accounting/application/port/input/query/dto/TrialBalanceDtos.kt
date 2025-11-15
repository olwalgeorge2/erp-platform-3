package com.erp.finance.accounting.application.port.input.query.dto

import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionType
import java.util.UUID

data class TrialBalanceQuery(
    val tenantId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val dimensionFilters: Map<DimensionType, UUID> = emptyMap(),
)

data class TrialBalanceLine(
    val accountId: UUID,
    val accountCode: String,
    val accountName: String,
    val accountType: AccountType,
    val debitTotalMinor: Long,
    val creditTotalMinor: Long,
    val netBalanceMinor: Long,
)

data class TrialBalanceResult(
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val currency: String,
    val lines: List<TrialBalanceLine>,
)

data class GlSummaryQuery(
    val tenantId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val dimensionType: DimensionType,
)

data class GlSummaryLine(
    val dimensionId: UUID,
    val dimensionCode: String,
    val dimensionName: String,
    val debitTotalMinor: Long,
    val creditTotalMinor: Long,
    val netBalanceMinor: Long,
)

data class GlSummaryResult(
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val dimensionType: DimensionType,
    val currency: String,
    val lines: List<GlSummaryLine>,
)
