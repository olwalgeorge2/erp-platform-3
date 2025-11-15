package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.input.dto.AccountingPeriodDto
import com.erp.finance.accounting.application.port.input.dto.LedgerPeriodInfoDto
import com.erp.finance.accounting.application.port.input.query.FinanceQueryUseCase
import com.erp.finance.accounting.application.port.input.query.GetLedgerForCompanyCodeQuery
import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryLine
import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryQuery
import com.erp.finance.accounting.application.port.input.query.dto.GlSummaryResult
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceLine
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceQuery
import com.erp.finance.accounting.application.port.input.query.dto.TrialBalanceResult
import com.erp.finance.accounting.application.port.output.AccountingPeriodRepository
import com.erp.finance.accounting.application.port.output.CompanyCodeLedgerRepository
import com.erp.finance.accounting.application.port.output.LedgerRepository
import com.erp.finance.accounting.application.port.output.TrialBalanceRepository
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.LedgerId
import io.micrometer.core.annotation.Timed
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class FinanceQueryService(
    private val trialBalanceRepository: TrialBalanceRepository,
    private val ledgerRepository: LedgerRepository,
    private val companyCodeLedgerRepository: CompanyCodeLedgerRepository,
    private val accountingPeriodRepository: AccountingPeriodRepository,
) : FinanceQueryUseCase {
    @Timed(value = "finance.trialbalance.fetch", percentiles = [0.5, 0.95, 0.99])
    override fun getTrialBalance(query: TrialBalanceQuery): TrialBalanceResult {
        val ledger =
            ledgerRepository.findById(
                com.erp.finance.accounting.domain.model
                    .LedgerId(query.ledgerId),
                query.tenantId,
            )
                ?: error("Ledger ${query.ledgerId} not found for tenant ${query.tenantId}")

        val rows =
            trialBalanceRepository.fetchTrialBalance(
                tenantId = query.tenantId,
                ledgerId = query.ledgerId,
                accountingPeriodId = query.accountingPeriodId,
                dimensionFilters = query.dimensionFilters,
            )

        val lines =
            rows.map {
                TrialBalanceLine(
                    accountId = it.accountId,
                    accountCode = it.accountCode,
                    accountName = it.accountName,
                    accountType = it.accountType,
                    debitTotalMinor = it.debitTotalMinor,
                    creditTotalMinor = it.creditTotalMinor,
                    netBalanceMinor = it.debitTotalMinor - it.creditTotalMinor,
                )
            }

        return TrialBalanceResult(
            ledgerId = query.ledgerId,
            accountingPeriodId = query.accountingPeriodId,
            currency = ledger.baseCurrency,
            lines = lines,
        )
    }

    @Timed(value = "finance.glsummary.fetch", percentiles = [0.5, 0.95, 0.99])
    override fun getGlSummary(query: GlSummaryQuery): GlSummaryResult {
        val ledger =
            ledgerRepository.findById(LedgerId(query.ledgerId), query.tenantId)
                ?: error("Ledger ${query.ledgerId} not found for tenant ${query.tenantId}")

        val rows =
            trialBalanceRepository.fetchSummaryByDimension(
                tenantId = query.tenantId,
                ledgerId = query.ledgerId,
                accountingPeriodId = query.accountingPeriodId,
                dimensionType = query.dimensionType,
            )

        val lines =
            rows.map {
                GlSummaryLine(
                    dimensionId = it.dimensionId,
                    dimensionCode = it.dimensionCode,
                    dimensionName = it.dimensionName,
                    debitTotalMinor = it.debitTotalMinor,
                    creditTotalMinor = it.creditTotalMinor,
                    netBalanceMinor = it.debitTotalMinor - it.creditTotalMinor,
                )
            }

        return GlSummaryResult(
            ledgerId = query.ledgerId,
            accountingPeriodId = query.accountingPeriodId,
            dimensionType = query.dimensionType,
            currency = ledger.baseCurrency,
            lines = lines,
        )
    }

    override fun getLedgerAndPeriodForCompanyCode(query: GetLedgerForCompanyCodeQuery): LedgerPeriodInfoDto? {
        val ledgerIds = companyCodeLedgerRepository.findLedgersForCompanyCode(query.companyCodeId)
        if (ledgerIds.isEmpty()) {
            return null
        }

        val ledger =
            ledgerIds
                .asSequence()
                .mapNotNull { ledgerRepository.findById(LedgerId(it), query.tenantId) }
                .firstOrNull()
                ?: return null

        val openPeriods =
            accountingPeriodRepository
                .findOpenByLedger(ledger.id, query.tenantId)
                .sortedByDescending { it.endDate }

        val currentOpenPeriod =
            openPeriods
                .firstOrNull { it.status == AccountingPeriodStatus.OPEN }
                ?: openPeriods.firstOrNull()

        val periodDtos = openPeriods.map { it.toDto() }

        return LedgerPeriodInfoDto(
            ledgerId = ledger.id.value,
            ledgerCode = ledger.id.value.toString(),
            currentOpenPeriod = currentOpenPeriod?.toDto(),
            openPeriods = periodDtos,
        )
    }

    private fun AccountingPeriod.toDto(): AccountingPeriodDto =
        AccountingPeriodDto(
            periodId = id.value,
            code = code,
            startDate = startDate,
            endDate = endDate,
            status = status.name,
        )
}
