package com.erp.identity.infrastructure.validation.validators

import com.erp.identity.infrastructure.validation.constraints.ValidUsername
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validator for @ValidUsername annotation.
 *
 * Validates username format:
 * - Length: 3-50 characters
 * - Characters: lowercase/uppercase letters, numbers, dots, underscores
 * - Must start and end with alphanumeric
 * - No consecutive dots or underscores
 *
 * Regex pattern: ^[a-zA-Z0-9]([a-zA-Z0-9._]*[a-zA-Z0-9])?$
 */
class UsernameValidator : ConstraintValidator<ValidUsername, String?> {
    private val usernamePattern = Regex("^[a-zA-Z0-9]([a-zA-Z0-9._]*[a-zA-Z0-9])?\$")

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        // Null values are considered valid (use @NotNull separately if required)
        if (value == null) {
            return true
        }

        // Blank strings are invalid
        if (value.isBlank()) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Username cannot be blank",
                )?.addConstraintViolation()
            return false
        }

        // Validate length
        if (value.length < 3 || value.length > 50) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Username must be between 3 and 50 characters (found ${value.length})",
                )?.addConstraintViolation()
            return false
        }

        // Check for consecutive special characters
        if (value.contains("..") || value.contains("__") || value.contains("._") || value.contains("_.")) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Username cannot contain consecutive dots or underscores",
                )?.addConstraintViolation()
            return false
        }

        // Validate format
        if (!usernamePattern.matches(value)) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Username must contain only letters, numbers, dots, and underscores. Must start and end with alphanumeric. Example: john.doe",
                )?.addConstraintViolation()
            return false
        }

        return true
    }
}
