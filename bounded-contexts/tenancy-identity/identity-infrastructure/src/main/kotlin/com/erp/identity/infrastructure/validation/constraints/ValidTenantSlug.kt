package com.erp.identity.infrastructure.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates tenant slug format for URL-safe identifiers.
 *
 * Valid format:
 * - 3-50 characters
 * - Lowercase alphanumeric with hyphens
 * - Must start and end with alphanumeric
 * - No consecutive hyphens
 *
 * Examples:
 * - Valid: "acme-corp", "tenant-123", "my-company"
 * - Invalid: "-acme", "acme-", "acme--corp", "Acme", "acme_corp", "a", "ab"
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [com.erp.identity.infrastructure.validation.validators.TenantSlugValidator::class])
@MustBeDocumented
annotation class ValidTenantSlug(
    val message: String =
        "Invalid tenant slug format. Must be 3-50 lowercase alphanumeric " +
            "characters with hyphens (e.g., my-company)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
