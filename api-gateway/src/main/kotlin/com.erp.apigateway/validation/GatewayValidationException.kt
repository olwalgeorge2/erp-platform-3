package com.erp.apigateway.validation

import java.util.Locale

class GatewayValidationException(
    val errorCode: GatewayValidationErrorCode,
    val field: String,
    val rejectedValue: String?,
    val locale: Locale,
    message: String,
) : RuntimeException(message)
