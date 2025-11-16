package com.erp.financial.shared.validation

import java.util.Locale

class FinanceValidationException(
    val errorCode: FinanceValidationErrorCode,
    val field: String,
    val rejectedValue: String?,
    val locale: Locale,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
