package com.erp.finance.accounting.application.port.input.command

import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.Money
import java.time.Instant
import java.util.UUID

data class PostJournalEntryCommand(
    val tenantId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val reference: String? = null,
    val description: String? = null,
    val bookedAt: Instant = Instant.now(),
    val lines: List<JournalEntryLineCommand>,
)

data class JournalEntryLineCommand(
    val accountId: AccountId,
    val direction: EntryDirection,
    val amount: Money,
    val currency: String? = null,
    val description: String? = null,
    val dimensions: DimensionAssignments = DimensionAssignments(),
)

data class RunCurrencyRevaluationCommand(
    val tenantId: UUID,
    val ledgerId: UUID,
    val accountingPeriodId: UUID,
    val asOf: Instant = Instant.now(),
    val bookedAt: Instant = asOf,
    val gainAccountId: AccountId,
    val lossAccountId: AccountId,
    val reference: String? = null,
    val description: String? = null,
)
