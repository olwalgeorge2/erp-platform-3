package com.erp.finance.accounting.application.port.input.dto

import java.time.LocalDate
import java.util.UUID

data class LedgerPeriodInfoDto(
    val ledgerId: UUID,
    val ledgerCode: String,
    val currentOpenPeriod: AccountingPeriodDto?,
    val openPeriods: List<AccountingPeriodDto> = emptyList(),
)

data class AccountingPeriodDto(
    val periodId: UUID,
    val code: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String,
)
