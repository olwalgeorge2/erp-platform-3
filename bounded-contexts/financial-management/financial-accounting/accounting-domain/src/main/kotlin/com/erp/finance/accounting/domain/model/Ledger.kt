package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.util.UUID

@JvmInline
value class LedgerId(
    val value: UUID = UUID.randomUUID(),
)

@JvmInline
value class AccountId(
    val value: UUID = UUID.randomUUID(),
)

@JvmInline
value class ChartOfAccountsId(
    val value: UUID = UUID.randomUUID(),
)

@JvmInline
value class AccountingPeriodId(
    val value: UUID = UUID.randomUUID(),
)

@JvmInline
value class Money(
    val amount: Long,
) {
    init {
        require(amount >= 0) { "Money cannot be negative when represented in minor units" }
    }

    operator fun plus(other: Money): Money = Money(amount + other.amount)

    operator fun minus(other: Money): Money {
        require(amount >= other.amount) { "Resulting amount cannot be negative" }
        return Money(amount - other.amount)
    }

    companion object {
        val ZERO = Money(0)
    }
}

enum class LedgerStatus { ACTIVE, ARCHIVED }

data class Ledger(
    val id: LedgerId = LedgerId(),
    val tenantId: UUID,
    val chartOfAccountsId: ChartOfAccountsId,
    val baseCurrency: String,
    val status: LedgerStatus = LedgerStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
