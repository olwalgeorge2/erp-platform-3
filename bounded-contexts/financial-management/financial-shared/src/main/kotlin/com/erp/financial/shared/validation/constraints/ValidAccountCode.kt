package com.erp.financial.shared.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates account code format for financial accounting.
 *
 * Valid formats:
 * - 4-6 digit account codes: "1000", "200000"
 * - With optional sub-account (2-4 digits): "1000-01", "200000-1234"
 *
 * Examples:
 * - Valid: "1000", "2000", "100000", "1000-01", "2000-99", "100000-1234"
 * - Invalid: "ABC", "1", "12", "123", "1000-", "1000-1", "1000-ABC"
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [com.erp.financial.shared.validation.validators.AccountCodeValidator::class])
@MustBeDocumented
annotation class ValidAccountCode(
    val message: String =
        "Invalid account code format. Expected format: 4-6 digits " +
            "with optional sub-account (e.g., 1000-01)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
