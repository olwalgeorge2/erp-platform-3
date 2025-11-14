package com.erp.finance.accounting.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class ChartOfAccountsTest {
    private val tenantId = UUID.randomUUID()

    @Test
    fun `defines posting account`() {
        val coa =
            ChartOfAccounts(
                tenantId = tenantId,
                baseCurrency = "USD",
            )

        val updated =
            coa.defineAccount(
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
            )

        assertEquals(1, updated.accounts.size)
        val account = updated.accounts.values.first()
        assertEquals("1000", account.code)
        assertEquals(AccountType.ASSET, account.type)
    }

    @Test
    fun `prevents duplicate codes`() {
        val coa =
            ChartOfAccounts(
                tenantId = tenantId,
                baseCurrency = "USD",
            ).defineAccount(
                code = "2000",
                name = "AR",
                type = AccountType.ASSET,
            )

        assertThrows(IllegalArgumentException::class.java) {
            coa.defineAccount(
                code = "2000",
                name = "Duplicate",
                type = AccountType.ASSET,
            )
        }
    }
}
