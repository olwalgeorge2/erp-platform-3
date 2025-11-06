package com.erp.identity.domain.model.identity

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
}
