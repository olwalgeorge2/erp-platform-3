package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidCurrencyCode
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CurrencyCodeValidatorTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    data class TestDto(
        @field:ValidCurrencyCode
        val currency: String?,
    )

    @ParameterizedTest
    @ValueSource(strings = ["USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "MXN"])
    fun `should accept supported currency codes`(currency: String) {
        val dto = TestDto(currency)
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty(), "Expected no violations for: $currency")
    }

    @ParameterizedTest
    @ValueSource(strings = ["XXX", "ZZZ", "BTC", "ETH", "ABC"])
    fun `should reject unsupported currency codes`(currency: String) {
        val dto = TestDto(currency)
        val violations = validator.validate(dto)
        assertEquals(1, violations.size, "Expected 1 violation for: $currency")
        val message = violations.first().message
        assertTrue(message.contains("not supported"))
        assertTrue(message.contains("USD, EUR, GBP"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["usd", "eur", "gbp"])
    fun `should reject lowercase currency codes`(currency: String) {
        val dto = TestDto(currency)
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("must be uppercase"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["Usd", "Eur", "GbP"])
    fun `should reject mixed case currency codes`(currency: String) {
        val dto = TestDto(currency)
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("must be uppercase"))
    }

    @Test
    fun `should accept null currency codes`() {
        val dto = TestDto(null)
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject blank currency codes`() {
        val dto = TestDto("   ")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("cannot be blank"))
    }

    @Test
    fun `should reject empty currency codes`() {
        val dto = TestDto("")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("cannot be blank"))
    }

    @Test
    fun `should reject currency codes with 2 characters`() {
        val dto = TestDto("US")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("exactly 3 characters"))
    }

    @Test
    fun `should reject currency codes with 4 characters`() {
        val dto = TestDto("USDT")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("exactly 3 characters"))
    }

    @Test
    fun `should reject currency codes with numbers`() {
        val dto = TestDto("US1")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("not supported"))
    }

    @Test
    fun `should reject currency codes with special characters`() {
        val dto = TestDto("US$")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("not supported"))
    }

    @Test
    fun `should reject currency codes with spaces`() {
        val dto = TestDto("U D")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
    }

    @Test
    fun `should provide helpful error message with supported currencies list`() {
        val dto = TestDto("XXX")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        val message = violations.first().message
        assertTrue(message.contains("USD"))
        assertTrue(message.contains("EUR"))
        assertTrue(message.contains("GBP"))
        assertTrue(message.contains("JPY"))
        assertTrue(message.contains("CAD"))
        assertTrue(message.contains("AUD"))
        assertTrue(message.contains("CHF"))
        assertTrue(message.contains("CNY"))
        assertTrue(message.contains("INR"))
        assertTrue(message.contains("MXN"))
    }

    @Test
    fun `should handle all supported currencies correctly`() {
        val supportedCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "MXN")
        supportedCurrencies.forEach { currency ->
            val dto = TestDto(currency)
            val violations = validator.validate(dto)
            assertTrue(violations.isEmpty(), "Currency $currency should be valid")
        }
    }

    @Test
    fun `should reject single character`() {
        val dto = TestDto("U")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("exactly 3 characters"))
    }

    @Test
    fun `should reject very long strings`() {
        val dto = TestDto("USDDOLLAR")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("exactly 3 characters"))
    }
}
