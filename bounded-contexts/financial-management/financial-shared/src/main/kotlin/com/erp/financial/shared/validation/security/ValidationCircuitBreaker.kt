package com.erp.financial.shared.validation.security

import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.faulttolerance.CircuitBreaker
import org.eclipse.microprofile.faulttolerance.Timeout
import java.time.Duration
import java.util.Locale
import java.util.function.Supplier

@ApplicationScoped
class ValidationCircuitBreaker {
    @CircuitBreaker(
        requestVolumeThreshold = 10,
        failureRatio = 0.5,
        delay = 5000,
        successThreshold = 3,
    )
    @Timeout(750)
    private fun <T> execute(supplier: Supplier<T>): T = supplier.get()

    fun <T> guard(
        operation: String,
        supplier: Supplier<T>,
    ): T =
        try {
            execute(supplier)
        } catch (ex: Exception) {
            throw FinanceValidationException(
                errorCode = FinanceValidationErrorCode.FINANCE_DEPENDENCY_UNAVAILABLE,
                field = operation,
                rejectedValue = null,
                locale = Locale.getDefault(),
                message = "Validation dependency unavailable for $operation. Please retry later.",
                cause = ex,
            )
        }

    fun <T> guard(
        operation: String,
        block: () -> T,
    ): T = guard(operation, Supplier(block))
}
