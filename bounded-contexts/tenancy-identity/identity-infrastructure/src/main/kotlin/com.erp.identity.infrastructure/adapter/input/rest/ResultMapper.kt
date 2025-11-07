package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.shared.types.results.Result
import com.erp.shared.types.results.ValidationError
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status
import jakarta.ws.rs.core.Response.Status.Family
import jakarta.ws.rs.core.Response.StatusType

data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val validationErrors: List<ValidationErrorResponse> = emptyList(),
)

data class ValidationErrorResponse(
    val field: String,
    val code: String,
    val message: String,
    val rejectedValue: String?,
) {
    companion object {
        fun from(error: ValidationError): ValidationErrorResponse =
            ValidationErrorResponse(
                field = error.field,
                code = error.code,
                message = error.message,
                rejectedValue = error.rejectedValue,
            )
    }
}

fun <T, R> Result<T>.toResponse(
    successStatus: Status = Status.OK,
    transform: (T) -> R,
): Response =
    when (this) {
        is Result.Success ->
            Response
                .status(successStatus)
                .entity(transform(value))
                .build()
        is Result.Failure -> failureResponse()
    }

fun Result.Failure.failureResponse(): Response {
    val status = mapStatus(error.code, validationErrors.isNotEmpty())
    return Response
        .status(status)
        .entity(
            ErrorResponse(
                code = error.code,
                message = error.message,
                details = error.details,
                validationErrors = validationErrors.map(ValidationErrorResponse::from),
            ),
        ).build()
}

private fun mapStatus(
    code: String,
    hasValidationErrors: Boolean,
): StatusType =
    when {
        hasValidationErrors -> UNPROCESSABLE_ENTITY
        code.equals("TENANT_NOT_FOUND", ignoreCase = true) -> Status.NOT_FOUND
        code.equals("USER_NOT_FOUND", ignoreCase = true) -> Status.NOT_FOUND
        code.equals("TENANT_SLUG_EXISTS", ignoreCase = true) -> Status.CONFLICT
        code.equals("USERNAME_IN_USE", ignoreCase = true) -> Status.CONFLICT
        code.equals("EMAIL_IN_USE", ignoreCase = true) -> Status.CONFLICT
        code.equals("INVALID_CREDENTIALS", ignoreCase = true) -> Status.UNAUTHORIZED
        code.equals("ACCOUNT_LOCKED", ignoreCase = true) -> Status.FORBIDDEN
        code.equals("USER_NOT_ALLOWED", ignoreCase = true) -> Status.FORBIDDEN
        code.equals("PASSWORD_POLICY_VIOLATION", ignoreCase = true) -> UNPROCESSABLE_ENTITY
        code.equals("WEAK_PASSWORD", ignoreCase = true) -> UNPROCESSABLE_ENTITY
        else -> Status.BAD_REQUEST
    }

private val UNPROCESSABLE_ENTITY: StatusType =
    object : StatusType {
        override fun getStatusCode(): Int = 422

        override fun getReasonPhrase(): String = "Unprocessable Entity"

        override fun getFamily(): Family = Family.CLIENT_ERROR
    }
