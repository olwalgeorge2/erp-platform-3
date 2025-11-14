package com.erp.finance.accounting.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoneyTest {
    @Test
    fun `supports basic arithmetic`() {
        val base = Money(1000)
        val credit = Money(400)

        assertEquals(Money(1400), base + credit)
        assertEquals(Money(600), base - credit)
    }
}
