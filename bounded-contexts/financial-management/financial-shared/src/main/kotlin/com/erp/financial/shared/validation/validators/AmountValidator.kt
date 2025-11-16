package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidAmount
import com.erp.financial.shared.validation.metrics.ValidationMetrics
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
        val start = System.nanoTime()
        val result =
            when {
                value == null -> true
                else -> validateScale(value, context) && validateSign(value, context)
            }
        ValidationMetrics.recordRule("amount", System.nanoTime() - start, result)
        return result
    }

    private fun validateScale(
        value: BigDecimal,
        context: ConstraintValidatorContext?,
    ): Boolean {
        val scale = value.scale()
        if (scale < minScale || scale > maxScale) {
            context?.disableDefaultConstraintViolation()
            context
                ?.buildConstraintViolationWithTemplate(
                    "Amount must have between $minScale and $maxScale decimal places (found $scale)",
                )?.addConstraintViolation()
            return false
        }
        return true
    }

    private fun validateSign(
        value: BigDecimal,
        context: ConstraintValidatorContext?,
    ): Boolean {
        val signum = value.signum()
        return when {
            signum < 0 && !allowNegative -> {
                context?.disableDefaultConstraintViolation()
                context
                    ?.buildConstraintViolationWithTemplate(
                        "Amount cannot be negative",
                    )?.addConstraintViolation()
                false
            }
            signum > 0 && !allowPositive -> {
                context?.disableDefaultConstraintViolation()
                context
                    ?.buildConstraintViolationWithTemplate(
                        "Amount cannot be positive",
                    )?.addConstraintViolation()
                false
            }
            signum == 0 && !allowZero -> {
                context?.disableDefaultConstraintViolation()
                context
                    ?.buildConstraintViolationWithTemplate(
                        "Amount cannot be zero",
                    )?.addConstraintViolation()
                false
            }
            else -> true
        }
    }
}
