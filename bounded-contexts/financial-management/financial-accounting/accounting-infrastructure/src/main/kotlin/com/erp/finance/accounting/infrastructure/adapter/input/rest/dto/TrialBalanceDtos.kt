package com.erp.finance.accounting.infrastructure.adapter.input.rest.dto

import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryResult
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceResult
import java.util.UUID

data class TrialBalanceResponse(
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val currency: String,
    val lines: List<TrialBalanceLineResponse>,
)

data class TrialBalanceLineResponse(
    val accountId: UUID,
    val accountCode: String,
    val accountName: String,
    val accountType: String,
    val debitTotalMinor: Long,
    val creditTotalMinor: Long,
    val netBalanceMinor: Long,
)

fun TrialBalanceResult.toResponse(): TrialBalanceResponse =
    TrialBalanceResponse(
        ledgerId = ledgerId,
        accountingPeriodId = accountingPeriodId,
        currency = currency,
        lines =
            lines.map {
                TrialBalanceLineResponse(
                    accountId = it.accountId,
                    accountCode = it.accountCode,
                    accountName = it.accountName,
                    accountType = it.accountType.name,
                    debitTotalMinor = it.debitTotalMinor,
                    creditTotalMinor = it.creditTotalMinor,
                    netBalanceMinor = it.netBalanceMinor,
                )
            },
    )

data class GlSummaryResponse(
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val dimensionType: String,
    val currency: String,
    val lines: List<GlSummaryLineResponse>,
)

data class GlSummaryLineResponse(
    val dimensionId: UUID,
    val dimensionCode: String,
    val dimensionName: String,
    val debitTotalMinor: Long,
    val creditTotalMinor: Long,
    val netBalanceMinor: Long,
)

fun GlSummaryResult.toResponse(): GlSummaryResponse =
    GlSummaryResponse(
        ledgerId = ledgerId,
        accountingPeriodId = accountingPeriodId,
        dimensionType = dimensionType.name,
        currency = currency,
        lines =
            lines.map {
                GlSummaryLineResponse(
                    dimensionId = it.dimensionId,
                    dimensionCode = it.dimensionCode,
                    dimensionName = it.dimensionName,
                    debitTotalMinor = it.debitTotalMinor,
                    creditTotalMinor = it.creditTotalMinor,
                    netBalanceMinor = it.netBalanceMinor,
                )
            },
    )
