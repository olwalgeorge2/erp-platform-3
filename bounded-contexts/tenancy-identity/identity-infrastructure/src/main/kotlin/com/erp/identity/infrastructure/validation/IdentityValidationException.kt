package com.erp.identity.infrastructure.validation

import java.util.Locale

class IdentityValidationException(
    val errorCode: ValidationErrorCode,
    val field: String,
    val rejectedValue: String?,
    val locale: Locale,
    message: String,
) : RuntimeException(message)
