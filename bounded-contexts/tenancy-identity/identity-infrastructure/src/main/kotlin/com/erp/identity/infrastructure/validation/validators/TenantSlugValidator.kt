package com.erp.identity.infrastructure.validation.validators

import com.erp.identity.infrastructure.validation.constraints.ValidTenantSlug
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validator for @ValidTenantSlug annotation.
 *
 * Validates tenant slug format:
 * - Length: 3-50 characters
 * - Characters: lowercase letters, numbers, hyphens
 * - Must start and end with alphanumeric
 * - No consecutive hyphens
 *
 * Regex pattern: ^[a-z0-9]([a-z0-9-]*[a-z0-9])?$
 */
class TenantSlugValidator : ConstraintValidator<ValidTenantSlug, String?> {
    private val tenantSlugPattern = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?\$")

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
                    "Tenant slug cannot be blank",
                )?.addConstraintViolation()
            return false
        }

        // Validate length
        if (value.length < 3 || value.length > 50) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Tenant slug must be between 3 and 50 characters (found ${value.length})",
                )?.addConstraintViolation()
            return false
        }

        // Check for consecutive hyphens
        if (value.contains("--")) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Tenant slug cannot contain consecutive hyphens",
                )?.addConstraintViolation()
            return false
        }

        // Validate format
        if (!tenantSlugPattern.matches(value)) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Tenant slug must contain only lowercase letters, numbers, and hyphens. Must start and end with alphanumeric. Example: my-company",
                )?.addConstraintViolation()
            return false
        }

        return true
    }
}
