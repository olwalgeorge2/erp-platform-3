package com.erp.shared.types.errors

import com.erp.shared.types.results.DomainError
import com.erp.shared.types.results.ValidationError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErrorSanitizerTest {
    @Test
    fun `production mode returns generic message for sensitive errors`() {
        val sanitized =
            ErrorSanitizer.sanitize(
                error =
                    DomainError(
                        code = "USERNAME_IN_USE",
                        message = "Username already exists: john",
                    ),
                validationErrors = emptyList(),
                environment = Environment.PRODUCTION,
            )

        assertEquals(
            "We couldn't complete your registration. Please try again or contact support.",
            sanitized.message,
        )
        assertTrue(sanitized.validationErrors.isEmpty())
        assertTrue(sanitized.suggestions == null)
    }

    @Test
    fun `production mode keeps validation errors for non sensitive codes`() {
        val sanitized =
            ErrorSanitizer.sanitize(
                error =
                    DomainError(
                        code = "ROLE_NAME_EXISTS",
                        message = "Role already exists",
                        details = mapOf("role" to "admin"),
                    ),
                validationErrors =
                    listOf(
                        ValidationError(
                            field = "name",
                            code = "duplicate",
                            message = "Name already taken",
                        ),
                    ),
                environment = Environment.PRODUCTION,
            )

        assertEquals("A role with that name already exists.", sanitized.message)
        assertEquals(1, sanitized.validationErrors.size)
        assertEquals("name", sanitized.validationErrors.first().field)
        assertEquals("Name already taken", sanitized.validationErrors.first().message)
        assertTrue(sanitized.details == null)
    }

    @Test
    fun `development mode does not sanitize`() {
        val sanitized =
            ErrorSanitizer.sanitize(
                error =
                    DomainError(
                        code = "ROLE_NOT_FOUND",
                        message = "Role missing",
                        details = mapOf("roleId" to "123"),
                    ),
                validationErrors =
                    listOf(
                        ValidationError(
                            field = "roleId",
                            code = "not_found",
                            message = "Role not found",
                        ),
                    ),
                environment = Environment.DEVELOPMENT,
            )

        assertEquals("Role missing", sanitized.message)
        assertEquals("not_found", sanitized.validationErrors.first().code)
        assertEquals(mapOf("roleId" to "123"), sanitized.details)
    }
}
