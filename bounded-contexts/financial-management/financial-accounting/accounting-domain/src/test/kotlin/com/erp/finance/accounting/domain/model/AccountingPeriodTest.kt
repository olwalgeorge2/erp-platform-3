package com.erp.finance.accounting.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AccountingPeriodTest {
    private val ledgerId = LedgerId()
    private val tenantId = UUID.randomUUID()

    @Test
    fun `transitions from open to frozen to closed`() {
        val period =
            AccountingPeriod(
                ledgerId = ledgerId,
                tenantId = tenantId,
                code = "2025-01",
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 1, 31),
            )

        val frozen = period.freeze()
        val reopened = frozen.reopen()
        val closed = reopened.close()

        assertEquals(AccountingPeriodStatus.OPEN, period.status)
        assertEquals(AccountingPeriodStatus.FROZEN, frozen.status)
        assertEquals(AccountingPeriodStatus.OPEN, reopened.status)
        assertEquals(AccountingPeriodStatus.CLOSED, closed.status)
    }

    @Test
    fun `cannot reopen closed period`() {
        val period =
            AccountingPeriod(
                ledgerId = ledgerId,
                tenantId = tenantId,
                code = "2025-01",
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 1, 31),
                status = AccountingPeriodStatus.CLOSED,
            )

        assertThrows(IllegalStateException::class.java) { period.reopen() }
        assertThrows(IllegalStateException::class.java) { period.freeze() }
    }
}
