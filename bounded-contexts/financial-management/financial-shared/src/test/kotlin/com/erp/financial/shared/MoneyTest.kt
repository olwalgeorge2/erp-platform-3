package com.erp.financial.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyTest {
    @Test
    fun `supports arithmetic in same currency`() {
        val usd100 = Money(10_000, "USD")
        val usd25 = Money(2_500, "USD")

        assertEquals(Money(12_500, "USD"), usd100 + usd25)
        assertEquals(Money(7_500, "USD"), usd100 - usd25)
    }

    @Test
    fun `prevents arithmetic across currencies`() {
        val usd = Money(1_000, "USD")
        val eur = Money(1_000, "EUR")

        assertThrows(IllegalArgumentException::class.java) {
            usd + eur
        }
    }

    @Test
    fun `creates from major units`() {
        val amount = Money.fromMajor(BigDecimal("10.55"), "usd")

        assertEquals(Money(1_055, "USD"), amount)
    }
}
