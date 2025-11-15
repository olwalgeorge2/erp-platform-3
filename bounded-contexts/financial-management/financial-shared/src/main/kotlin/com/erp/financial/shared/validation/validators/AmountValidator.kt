package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidAmount
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.math.BigDecimal

/**
 * Validator for @ValidAmount annotation.
 *
 * Validates:
 * - Decimal scale within configured range (minScale to maxScale)
 * - Sign (positive/negative/zero) based on configuration
 * - Non-null values (use with @NotNull for mandatory amounts)
 */
class AmountValidator : ConstraintValidator<ValidAmount, BigDecimal?> {
    private var minScale: Int = 2
    private var maxScale: Int = 4
    private var allowNegative: Boolean = true
    private var allowPositive: Boolean = true
    private var allowZero: Boolean = true

    override fun initialize(constraintAnnotation: ValidAmount) {
        this.minScale = constraintAnnotation.minScale
        this.maxScale = constraintAnnotation.maxScale
        this.allowNegative = constraintAnnotation.allowNegative
        this.allowPositive = constraintAnnotation.allowPositive
        this.allowZero = constraintAnnotation.allowZero
    }

    override fun isValid(
        value: BigDecimal?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        // Null values are considered valid (use @NotNull separately if required)
        if (value == null) {
            return true
        }

        // Validate scale
        val scale = value.scale()
        if (scale < minScale || scale > maxScale) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Amount must have between $minScale and $maxScale decimal places (found $scale)",
                )?.addConstraintViolation()
            return false
        }

        // Validate sign
        val signum = value.signum()
        when {
            signum < 0 && !allowNegative -> {
                context?.disableDefaultConstraintViolation()
                context
                    ?.buildConstraintViolationWithTemplate(
                        "Amount cannot be negative",
                    )?.addConstraintViolation()
                return false
            }
            signum > 0 && !allowPositive -> {
                context?.disableDefaultConstraintViolation()
                context
                    ?.buildConstraintViolationWithTemplate(
                        "Amount cannot be positive",
                    )?.addConstraintViolation()
                return false
            }
            signum == 0 && !allowZero -> {
                context?.disableDefaultConstraintViolation()
                context
                    ?.buildConstraintViolationWithTemplate(
                        "Amount cannot be zero",
                    )?.addConstraintViolation()
                return false
            }
        }

        return true
    }
}
