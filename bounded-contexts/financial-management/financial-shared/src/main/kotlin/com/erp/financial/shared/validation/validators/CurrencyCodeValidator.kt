package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidCurrencyCode
import com.erp.financial.shared.validation.metrics.ValidationMetrics
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validator for @ValidCurrencyCode annotation.
 *
 * Validates currency codes against a whitelist of supported ISO 4217 codes.
 * This prevents unsupported currencies from entering the system and ensures
 * proper exchange rate handling.
 */
class CurrencyCodeValidator : ConstraintValidator<ValidCurrencyCode, String?> {
    companion object {
        /**
         * Whitelist of supported currency codes (ISO 4217).
         * Add new currencies here when exchange rate support is added.
         */
        private val SUPPORTED_CURRENCIES =
            setOf(
                "USD", // US Dollar
                "EUR", // Euro
                "GBP", // British Pound
                "JPY", // Japanese Yen
                "CAD", // Canadian Dollar
                "AUD", // Australian Dollar
                "CHF", // Swiss Franc
                "CNY", // Chinese Yuan
                "INR", // Indian Rupee
                "MXN", // Mexican Peso
            )
    }

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        val start = System.nanoTime()
        val result =
            when {
                value == null -> true
                value.isBlank() -> {
                    context?.disableDefaultConstraintViolation()
                    context
                        ?.buildConstraintViolationWithTemplate(
                            "Currency code cannot be blank",
                        )?.addConstraintViolation()
                    false
                }
                value != value.uppercase() -> {
                    context?.disableDefaultConstraintViolation()
                    context
                        ?.buildConstraintViolationWithTemplate(
                            "Currency code must be uppercase (ISO 4217 standard)",
                        )?.addConstraintViolation()
                    false
                }
                value.length != 3 -> {
                    context?.disableDefaultConstraintViolation()
                    context
                        ?.buildConstraintViolationWithTemplate(
                            "Currency code must be exactly 3 characters (ISO 4217 standard)",
                        )?.addConstraintViolation()
                    false
                }
                value !in SUPPORTED_CURRENCIES -> {
                    context?.disableDefaultConstraintViolation()
                    context
                        ?.buildConstraintViolationWithTemplate(
                            "Currency code '$value' is not supported. Supported currencies: ${SUPPORTED_CURRENCIES.joinToString(
                                ", ",
                            )}",
                        )?.addConstraintViolation()
                    false
                }
                else -> true
            }
        ValidationMetrics.recordRule("currency_code", System.nanoTime() - start, result)
        return result
    }
}
