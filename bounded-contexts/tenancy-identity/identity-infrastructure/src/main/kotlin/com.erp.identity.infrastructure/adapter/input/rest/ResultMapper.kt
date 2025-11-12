package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.shared.types.errors.Environment
import com.erp.shared.types.errors.ErrorSanitizer
import com.erp.shared.types.results.Result
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status
import jakarta.ws.rs.core.Response.Status.Family
import jakarta.ws.rs.core.Response.StatusType
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.openapi.annotations.media.Schema

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
    val sanitized =
        ErrorSanitizer.sanitize(
            error = error,
            validationErrors = validationErrors,
            environment = currentEnvironment(),
        )
    return Response
        .status(status)
        .header("X-Error-ID", sanitized.errorId)
        .entity(sanitized)
        .build()
}

private fun mapStatus(
    code: String,
    hasValidationErrors: Boolean,
): StatusType =
    when {
        hasValidationErrors -> UNPROCESSABLE_ENTITY
        code.equals("TENANT_NOT_FOUND", ignoreCase = true) -> Status.NOT_FOUND
        code.equals("USER_NOT_FOUND", ignoreCase = true) -> Status.NOT_FOUND
        code.equals("AUTHENTICATION_FAILED", ignoreCase = true) -> Status.UNAUTHORIZED
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

private val environment: Environment by lazy {
    val configured =
        ConfigProvider
            .getConfig()
            .getOptionalValue("app.environment", String::class.java)
            .orElse("PRODUCTION")

    runCatching { Environment.valueOf(configured.trim().uppercase()) }
        .getOrDefault(Environment.PRODUCTION)
}

private fun currentEnvironment(): Environment = environment

@Schema(name = "ErrorResponse", description = "Standard error payload for identity service")
data class ErrorResponse(
    @field:Schema(description = "Stable error code", example = "TENANT_NOT_FOUND")
    val code: String,
    @field:Schema(description = "Human-readable message", example = "Tenant not found")
    val message: String,
    @field:Schema(description = "Additional error details")
    val details: Map<String, String> = emptyMap(),
    @field:Schema(description = "Validation errors, if any")
    val validationErrors: List<ValidationErrorResponse> = emptyList(),
)

@Schema(name = "ValidationErrorResponse")
data class ValidationErrorResponse(
    @field:Schema(description = "Field name")
    val field: String,
    @field:Schema(description = "Validation code", example = "NOT_BLANK")
    val code: String,
    @field:Schema(description = "Validation message")
    val message: String,
    @field:Schema(description = "Rejected value")
    val rejectedValue: String?,
)
