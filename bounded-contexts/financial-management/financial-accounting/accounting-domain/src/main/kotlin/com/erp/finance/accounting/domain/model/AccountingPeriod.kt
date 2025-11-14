package com.erp.finance.accounting.domain.model

import java.time.LocalDate
import java.util.UUID

enum class AccountingPeriodStatus { OPEN, FROZEN, CLOSED }

data class AccountingPeriod(
    val id: AccountingPeriodId = AccountingPeriodId(UUID.randomUUID()),
    val ledgerId: LedgerId,
    val tenantId: UUID,
    val code: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: AccountingPeriodStatus = AccountingPeriodStatus.OPEN,
) {
    init {
        require(!endDate.isBefore(startDate)) { "End date must be on/after start date" }
    }

    fun freeze(): AccountingPeriod =
        when (status) {
            AccountingPeriodStatus.OPEN -> copy(status = AccountingPeriodStatus.FROZEN)
            AccountingPeriodStatus.FROZEN -> this
            AccountingPeriodStatus.CLOSED -> error("Cannot freeze a closed period")
        }

    fun reopen(): AccountingPeriod =
        when (status) {
            AccountingPeriodStatus.CLOSED -> error("Closed period cannot be reopened")
            AccountingPeriodStatus.OPEN -> this
            AccountingPeriodStatus.FROZEN -> copy(status = AccountingPeriodStatus.OPEN)
        }

    fun close(): AccountingPeriod =
        when (status) {
            AccountingPeriodStatus.CLOSED -> this
            AccountingPeriodStatus.OPEN,
            AccountingPeriodStatus.FROZEN,
            -> copy(status = AccountingPeriodStatus.CLOSED)
        }
}
