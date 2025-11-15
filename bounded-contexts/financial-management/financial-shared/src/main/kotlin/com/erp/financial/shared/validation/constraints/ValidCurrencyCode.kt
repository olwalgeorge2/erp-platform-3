package com.erp.financial.shared.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates ISO 4217 currency codes.
 *
 * Supported currencies (whitelist):
 * - USD (US Dollar)
 * - EUR (Euro)
 * - GBP (British Pound)
 * - JPY (Japanese Yen)
 * - CAD (Canadian Dollar)
 * - AUD (Australian Dollar)
 * - CHF (Swiss Franc)
 * - CNY (Chinese Yuan)
 * - INR (Indian Rupee)
 * - MXN (Mexican Peso)
 *
 * Examples:
 * - Valid: "USD", "EUR", "GBP"
 * - Invalid: "XXX", "usa", "Bitcoin"
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [com.erp.financial.shared.validation.validators.CurrencyCodeValidator::class])
@MustBeDocumented
annotation class ValidCurrencyCode(
    val message: String =
        "Invalid currency code. Supported currencies: " +
            "USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, INR, MXN",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
