package com.erp.financial.shared.api

import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    name = "ErrorResponse",
    description = "Finance service error payload",
)
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
    val validationErrors: List<ValidationError> = emptyList(),
)

@Schema(
    name = "ValidationError",
    description = "Details about a specific invalid field/parameter",
)
data class ValidationError(
    val field: String,
    val code: String,
    val message: String,
    val rejectedValue: String?,
)
