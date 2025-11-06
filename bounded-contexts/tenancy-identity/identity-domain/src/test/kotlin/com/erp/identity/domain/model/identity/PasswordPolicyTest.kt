package com.erp.identity.domain.model.identity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordPolicyTest {
    private val policy = PasswordPolicy(minLength = 12)

    @Test
    fun `valid password passes policy`() {
        val errors = policy.validate("ValidPassword123!")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `password missing uppercase fails`() {
        val errors = policy.validate("lowercase123!")

        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.code == "MISSING_UPPERCASE" })
    }

    @Test
    fun `password missing special character fails`() {
        val errors = policy.validate("NoSpecial123")

        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.code == "MISSING_SPECIAL" })
    }

    @Test
    fun `password shorter than minimum is rejected`() {
        val policy = PasswordPolicy(minLength = 10)

        val errors = policy.validate("Short1!")

        assertTrue(errors.any { it.code == "TOO_SHORT" })
    }

    @Test
    fun `password exceeding maximum is rejected`() {
        val policy = PasswordPolicy(minLength = 8, maxLength = 10)
        val password = "VeryLongPwd123!"

        val errors = policy.validate(password)

        assertTrue(errors.any { it.code == "TOO_LONG" })
        val lengthError = errors.first { it.code == "TOO_LONG" }
        assertEquals(password.length.toString(), lengthError.rejectedValue)
    }

    @Test
    fun `password missing number fails`() {
        val errors = policy.validate("NoNumberPassword!")

        assertTrue(errors.any { it.code == "MISSING_NUMBER" })
    }

    @Test
    fun `isSatisfiedBy returns false when lowercase required`() {
        val policy = PasswordPolicy(requireLowercase = true)

        val satisfied = policy.isSatisfiedBy("ALLUPPERCASE123!")

        assertFalse(satisfied)
    }
}
