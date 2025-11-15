package com.erp.identity.infrastructure.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates username format for user identifiers.
 *
 * Valid format:
 * - 3-50 characters
 * - Alphanumeric with dots and underscores
 * - Must start and end with alphanumeric
 * - No consecutive special characters
 *
 * Examples:
 * - Valid: "john.doe", "user_123", "johndoe", "john.doe123"
 * - Invalid: ".john", "john.", "john..doe", "John", "john-doe", "jd", "j"
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [com.erp.identity.infrastructure.validation.validators.UsernameValidator::class])
@MustBeDocumented
annotation class ValidUsername(
    val message: String =
        "Invalid username format. Must be 3-50 alphanumeric characters " +
            "with dots or underscores (e.g., john.doe)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
