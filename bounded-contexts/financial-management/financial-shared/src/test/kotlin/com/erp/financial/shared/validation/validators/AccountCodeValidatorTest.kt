package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidAccountCode
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AccountCodeValidatorTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    data class TestDto(
        @field:ValidAccountCode
        val accountCode: String?,
    )

    @ParameterizedTest
    @ValueSource(
        strings = [
            "1000",
            "2000",
            "100000",
            "1000-01",
            "2000-99",
            "100000-1234",
            "5000-12",
            "999999",
            "1234-9999",
        ],
    )
    fun `should accept valid account codes`(accountCode: String) {
        val dto = TestDto(accountCode)
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty(), "Expected no violations for: $accountCode")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "ABC",
            "1",
            "12",
            "123",
            "1000-",
            "1000-1",
            "1000-ABC",
            "-1000",
            "1000-12345",
            "12345678",
            "1000--01",
            "1000-01-02",
            "A1000",
            "1000A",
        ],
    )
    fun `should reject invalid account codes`(accountCode: String) {
        val dto = TestDto(accountCode)
        val violations = validator.validate(dto)
        assertEquals(1, violations.size, "Expected 1 violation for: $accountCode")
        val violation = violations.first()
        assertEquals("accountCode", violation.propertyPath.toString())
    }

    @Test
    fun `should accept null account codes`() {
        val dto = TestDto(null)
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject blank account codes`() {
        val dto = TestDto("   ")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("cannot be blank"))
    }

    @Test
    fun `should reject empty account codes`() {
        val dto = TestDto("")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        assertTrue(violations.first().message.contains("cannot be blank"))
    }

    @Test
    fun `should provide helpful error message for invalid format`() {
        val dto = TestDto("ABC-123")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
        val message = violations.first().message
        assertTrue(message.contains("4-6 digits"))
        assertTrue(message.contains("sub-account"))
    }

    @Test
    fun `should reject account codes with only sub-account`() {
        val dto = TestDto("-01")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
    }

    @Test
    fun `should reject account codes with spaces`() {
        val dto = TestDto("1000 01")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
    }

    @Test
    fun `should accept minimum valid main account`() {
        val dto = TestDto("1000")
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept maximum valid main account`() {
        val dto = TestDto("999999")
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept minimum valid sub-account`() {
        val dto = TestDto("1000-10")
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should accept maximum valid sub-account`() {
        val dto = TestDto("1000-9999")
        val violations = validator.validate(dto)
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject sub-account with single digit`() {
        val dto = TestDto("1000-1")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
    }

    @Test
    fun `should reject sub-account with 5 digits`() {
        val dto = TestDto("1000-12345")
        val violations = validator.validate(dto)
        assertEquals(1, violations.size)
    }
}
