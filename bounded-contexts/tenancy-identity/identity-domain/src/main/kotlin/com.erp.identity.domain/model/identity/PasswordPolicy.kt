package com.erp.identity.domain.model.identity

import com.erp.shared.types.results.ValidationError

/**
 * Enforces password complexity rules.
 */
data class PasswordPolicy(
    val minLength: Int = 12,
    val maxLength: Int = 128,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireNumber: Boolean = true,
    val requireSpecialCharacter: Boolean = true,
) {
    init {
        require(minLength in 8..maxLength) { "Minimum length must be between 8 and $maxLength" }
        require(maxLength <= 256) { "Maximum length cannot exceed 256" }
    }

    fun validate(password: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (password.length < minLength) {
            errors +=
                ValidationError(
                    field = "password",
                    code = "TOO_SHORT",
                    message = "Password must be at least $minLength characters long",
                    rejectedValue = password.length.toString(),
                )
        }

        if (password.length > maxLength) {
            errors +=
                ValidationError(
                    field = "password",
                    code = "TOO_LONG",
                    message = "Password must be at most $maxLength characters long",
                    rejectedValue = password.length.toString(),
                )
        }

        if (requireUppercase && !password.any { it.isUpperCase() }) {
            errors +=
                ValidationError(
                    field = "password",
                    code = "MISSING_UPPERCASE",
                    message = "Password must contain at least one uppercase letter",
                )
        }

        if (requireLowercase && !password.any { it.isLowerCase() }) {
            errors +=
                ValidationError(
                    field = "password",
                    code = "MISSING_LOWERCASE",
                    message = "Password must contain at least one lowercase letter",
                )
        }

        if (requireNumber && !password.any { it.isDigit() }) {
            errors +=
                ValidationError(
                    field = "password",
                    code = "MISSING_NUMBER",
                    message = "Password must contain at least one number",
                )
        }

        val specialPredicate = { ch: Char ->
            !ch.isDigit() && !ch.isLetter()
        }
        if (requireSpecialCharacter && !password.any(specialPredicate)) {
            errors +=
                ValidationError(
                    field = "password",
                    code = "MISSING_SPECIAL",
                    message = "Password must contain at least one special character",
                )
        }

        return errors
    }

    fun isSatisfiedBy(password: String): Boolean = validate(password).isEmpty()
}
