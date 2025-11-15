package com.erp.financial.shared.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates that a date range has startDate before or equal to endDate.
 *
 * This is a class-level constraint that validates two fields representing a date range.
 * Supports:
 * - java.time.LocalDate
 * - java.time.Instant
 * - java.time.LocalDateTime
 * - Nullable endDate (open-ended ranges)
 *
 * Usage:
 * ```kotlin
 * @ValidDateRange(startField = "startDate", endField = "endDate")
 * data class PeriodRequest(
 *     val startDate: LocalDate,
 *     val endDate: LocalDate?
 * )
 * ```
 *
 * @property startField The name of the field containing the start date
 * @property endField The name of the field containing the end date
 * @property message The error message when validation fails
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [com.erp.financial.shared.validation.validators.DateRangeValidator::class])
@MustBeDocumented
annotation class ValidDateRange(
    val startField: String,
    val endField: String,
    val message: String =
        "Invalid date range: end date must be after or equal to start date",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
