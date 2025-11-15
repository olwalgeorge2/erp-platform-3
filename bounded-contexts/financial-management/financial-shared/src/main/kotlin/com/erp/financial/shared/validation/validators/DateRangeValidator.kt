package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidDateRange
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.Temporal
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Validator for @ValidDateRange constraint.
 *
 * Validates that startDate is before or equal to endDate in date range objects.
 * Supports LocalDate, Instant, LocalDateTime, and other Temporal types.
 * Handles nullable endDate (open-ended ranges are valid).
 */
class DateRangeValidator : ConstraintValidator<ValidDateRange, Any> {
    private lateinit var startField: String
    private lateinit var endField: String
    private lateinit var message: String

    override fun initialize(constraintAnnotation: ValidDateRange) {
        startField = constraintAnnotation.startField
        endField = constraintAnnotation.endField
        message = constraintAnnotation.message
    }

    override fun isValid(
        value: Any?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) {
            return true // Use @NotNull for null checks
        }

        try {
            val startValue = getFieldValue(value, startField)
            val endValue = getFieldValue(value, endField)

            // If end date is null, the range is open-ended (valid)
            if (endValue == null) {
                return true
            }

            // Both dates must be present for comparison
            if (startValue == null) {
                return true // Field-level validation should catch this
            }

            // Compare dates based on their type
            return when {
                startValue is LocalDate && endValue is LocalDate ->
                    !startValue.isAfter(endValue)
                startValue is Instant && endValue is Instant ->
                    !startValue.isAfter(endValue)
                startValue is LocalDateTime && endValue is LocalDateTime ->
                    !startValue.isAfter(endValue)
                startValue is Temporal && endValue is Temporal ->
                    compareTemporals(startValue, endValue)
                else -> {
                    context.disableDefaultConstraintViolation()
                    context
                        .buildConstraintViolationWithTemplate(
                            "Unsupported date types: ${startValue::class.simpleName} and ${endValue::class.simpleName}",
                        ).addConstraintViolation()
                    false
                }
            }
        } catch (e: Exception) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(
                    "Failed to validate date range: ${e.message}",
                ).addConstraintViolation()
            return false
        }
    }

    private fun getFieldValue(
        obj: Any,
        fieldName: String,
    ): Any? {
        val kClass = obj::class
        val property =
            kClass.memberProperties
                .firstOrNull { it.name == fieldName }
                ?: throw IllegalArgumentException("Field '$fieldName' not found in ${kClass.simpleName}")

        @Suppress("UNCHECKED_CAST")
        val kProperty = property as KProperty1<Any, *>
        kProperty.isAccessible = true
        return kProperty.get(obj)
    }

    private fun compareTemporals(
        start: Temporal,
        end: Temporal,
    ): Boolean =
        try {
            // Try to convert both to Instant for comparison
            val startInstant =
                when (start) {
                    is Instant -> start
                    is LocalDate -> start.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                    is LocalDateTime -> start.toInstant(java.time.ZoneOffset.UTC)
                    else -> throw IllegalArgumentException("Unsupported temporal type: ${start::class.simpleName}")
                }
            val endInstant =
                when (end) {
                    is Instant -> end
                    is LocalDate -> end.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                    is LocalDateTime -> end.toInstant(java.time.ZoneOffset.UTC)
                    else -> throw IllegalArgumentException("Unsupported temporal type: ${end::class.simpleName}")
                }
            !startInstant.isAfter(endInstant)
        } catch (e: Exception) {
            false
        }
}
