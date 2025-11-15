package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidAmount
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AmountValidatorTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    data class StandardAmountDto(
        @field:ValidAmount(minScale = 2, maxScale = 2)
        val amount: BigDecimal?,
    )

    data class HighPrecisionAmountDto(
        @field:ValidAmount(minScale = 2, maxScale = 4)
        val amount: BigDecimal?,
    )

    data class PositiveOnlyDto(
        @field:ValidAmount(allowNegative = false)
        val amount: BigDecimal?,
    )

    data class NegativeOnlyDto(
        @field:ValidAmount(allowPositive = false)
        val amount: BigDecimal?,
    )

    data class NonZeroDto(
        @field:ValidAmount(allowZero = false)
        val amount: BigDecimal?,
    )

    @Test
    fun `should accept standard amounts with 2 decimal places`() {
        val dto = StandardAmountDto(BigDecimal("100.00"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject standard amounts with 0 decimal places`() {
        val dto = StandardAmountDto(BigDecimal("100"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("between 2 and 2 decimal places"))
    }

    @Test
    fun `should reject standard amounts with 1 decimal place`() {
        val dto = StandardAmountDto(BigDecimal("100.0"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
    }

    @Test
    fun `should reject standard amounts with 3 decimal places`() {
        val dto = StandardAmountDto(BigDecimal("100.123"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("between 2 and 2 decimal places"))
    }

    @Test
    fun `should accept high-precision amounts with 2 decimal places`() {
        val dto = HighPrecisionAmountDto(BigDecimal("100.00"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept high-precision amounts with 3 decimal places`() {
        val dto = HighPrecisionAmountDto(BigDecimal("100.123"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept high-precision amounts with 4 decimal places`() {
        val dto = HighPrecisionAmountDto(BigDecimal("100.1234"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject high-precision amounts with 1 decimal place`() {
        val dto = HighPrecisionAmountDto(BigDecimal("100.1"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("between 2 and 4 decimal places"))
    }

    @Test
    fun `should reject high-precision amounts with 5 decimal places`() {
        val dto = HighPrecisionAmountDto(BigDecimal("100.12345"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("between 2 and 4 decimal places"))
    }

    @Test
    fun `should accept positive amounts when negative not allowed`() {
        val dto = PositiveOnlyDto(BigDecimal("100.00"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject negative amounts when negative not allowed`() {
        val dto = PositiveOnlyDto(BigDecimal("-100.00"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("cannot be negative"))
    }

    @Test
    fun `should accept negative amounts when positive not allowed`() {
        val dto = NegativeOnlyDto(BigDecimal("-100.00"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject positive amounts when positive not allowed`() {
        val dto = NegativeOnlyDto(BigDecimal("100.00"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("cannot be positive"))
    }

    @Test
    fun `should accept zero by default`() {
        val dto = StandardAmountDto(BigDecimal("0.00"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject zero when zero not allowed`() {
        val dto = NonZeroDto(BigDecimal("0.00"))
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("cannot be zero"))
    }

    @Test
    fun `should accept null amounts`() {
        val dto = StandardAmountDto(null)
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept large amounts`() {
        val dto = StandardAmountDto(BigDecimal("9999999999999.99"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept small positive amounts`() {
        val dto = StandardAmountDto(BigDecimal("0.01"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept small negative amounts`() {
        val dto = StandardAmountDto(BigDecimal("-0.01"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should handle BigDecimal from string constructor correctly`() {
        val dto = StandardAmountDto(BigDecimal("123.45"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should handle BigDecimal from double constructor correctly`() {
        // Note: Double constructors can create unexpected scales
        val dto = StandardAmountDto(BigDecimal(123.45).setScale(2, java.math.RoundingMode.HALF_UP))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should validate trailing zeros in scale`() {
        val dto = StandardAmountDto(BigDecimal("100.00"))
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }
}
