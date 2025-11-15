package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidAccountCode
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validator for @ValidAccountCode annotation.
 *
 * Validates account code format:
 * - Main account: 4-6 digits (e.g., "1000", "200000")
 * - Optional sub-account: 2-4 digits after hyphen (e.g., "-01", "-1234")
 *
 * Regex pattern: ^\d{4,6}(-\d{2,4})?$
 */
class AccountCodeValidator : ConstraintValidator<ValidAccountCode, String?> {
    private val accountCodePattern = Regex("^\\d{4,6}(-\\d{2,4})?\$")

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
                    "Account code cannot be blank",
                )?.addConstraintViolation()
            return false
        }

        // Validate format
        if (!accountCodePattern.matches(value)) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Account code must be 4-6 digits with optional sub-account (2-4 digits after hyphen). Example: 1000 or 1000-01",
                )?.addConstraintViolation()
            return false
        }

        return true
    }
}
