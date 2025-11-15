package com.erp.financial.shared.validation.constraints

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Validates monetary amount format and scale.
 *
 * Enforces:
 * - Scale between minScale and maxScale decimal places
 * - Optional sign validation (positive/negative/both)
 * - Non-null BigDecimal values
 *
 * Common use cases:
 * - Standard amounts: @ValidAmount(minScale=2, maxScale=2) for $10.00
 * - High-precision: @ValidAmount(minScale=2, maxScale=4) for forex rates
 * - Positive only: @ValidAmount(allowNegative=false) for asset values
 * - Credit balances: @ValidAmount(allowPositive=false) for liability accounts
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [com.erp.financial.shared.validation.validators.AmountValidator::class])
@MustBeDocumented
annotation class ValidAmount(
    val message: String = "Invalid amount format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val minScale: Int = 2,
    val maxScale: Int = 4,
    val allowNegative: Boolean = true,
    val allowPositive: Boolean = true,
    val allowZero: Boolean = true,
)
