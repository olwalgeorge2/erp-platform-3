package com.erp.shared.types.errors

import com.erp.shared.types.results.DomainError
import com.erp.shared.types.results.ValidationError
import java.time.Instant
import java.util.UUID

object ErrorSanitizer {
    private val SENSITIVE_ERROR_CODES =
        setOf(
            "USERNAME_IN_USE",
            "EMAIL_IN_USE",
            "TENANT_SLUG_EXISTS",
            "USER_NOT_FOUND",
            "TENANT_NOT_FOUND",
        )

    fun sanitize(
        error: DomainError,
        validationErrors: List<ValidationError>,
        environment: Environment,
    ): SanitizedError =
        when (environment) {
            Environment.PRODUCTION -> sanitizeForProduction(error, validationErrors)
            Environment.STAGING -> sanitizeForStaging(error, validationErrors)
            Environment.DEVELOPMENT -> noSanitization(error, validationErrors)
        }

    private fun sanitizeForProduction(
        error: DomainError,
        validationErrors: List<ValidationError>,
    ): SanitizedError {
        if (error.code in SENSITIVE_ERROR_CODES) {
            return SanitizedError(
                message = getGenericMessage(error.code),
                errorId = UUID.randomUUID().toString(),
                timestamp = Instant.now(),
                validationErrors = emptyList(),
                suggestions = getRecoveryGuidance(error.code),
            )
        }

        return SanitizedError(
            message = getUserFriendlyMessage(error.code, error.message),
            errorId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            validationErrors = validationErrors.map(::sanitizeValidationError),
            suggestions = getRecoveryGuidance(error.code),
        )
    }

    private fun sanitizeForStaging(
        error: DomainError,
        validationErrors: List<ValidationError>,
    ): SanitizedError =
        SanitizedError(
            message = error.message,
            errorId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            validationErrors = validationErrors.map(::sanitizeValidationError),
            details = error.details,
            suggestions = getRecoveryGuidance(error.code),
        )

    private fun noSanitization(
        error: DomainError,
        validationErrors: List<ValidationError>,
    ): SanitizedError =
        SanitizedError(
            message = error.message,
            errorId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            validationErrors = validationErrors.map {
                SanitizedValidationError(
                    field = it.field,
                    message = it.message,
                    code = it.code,
                )
            },
            details = error.details,
        )

    private fun sanitizeValidationError(error: ValidationError): SanitizedValidationError =
        SanitizedValidationError(
            field = error.field,
            message = error.message,
        )

    private fun getGenericMessage(code: String): String =
        when (code) {
            "USERNAME_IN_USE",
            "EMAIL_IN_USE" ->
                "We couldn't complete your registration. Please try again or contact support."
            "USER_NOT_FOUND",
            "TENANT_NOT_FOUND" ->
                "We couldn't find that resource."
            "TENANT_SLUG_EXISTS" ->
                "That organization name is not available."
            else ->
                "We couldn't complete your request. Please try again later."
        }

    private fun getUserFriendlyMessage(
        code: String,
        fallback: String,
    ): String = USER_FRIENDLY_MESSAGES[code] ?: fallback

    private fun getRecoveryGuidance(code: String): List<String>? = RECOVERY_GUIDANCE[code]

    private val USER_FRIENDLY_MESSAGES =
        mapOf(
            "WEAK_PASSWORD" to "Your password doesn't meet our security requirements.",
            "INVALID_CREDENTIALS" to "The email or password you entered is incorrect.",
            "ACCOUNT_LOCKED" to "Your account has been temporarily locked for security reasons.",
            "ROLE_IMMUTABLE" to "This role cannot be modified because it's a system role.",
            "TENANT_STATE_INVALID" to "This action cannot be performed at this time.",
            "ROLE_NOT_FOUND" to "That role doesn't exist.",
            "ROLE_NAME_EXISTS" to "A role with that name already exists.",
        )

    private val RECOVERY_GUIDANCE =
        mapOf(
            "INVALID_CREDENTIALS" to
                listOf(
                    "Double-check your email and password for typos",
                    "Use 'Forgot Password' if you can't remember your password",
                    "Contact support if you continue having trouble",
                ),
            "ACCOUNT_LOCKED" to
                listOf(
                    "Wait 30 minutes for automatic unlock",
                    "Or contact support for immediate assistance",
                ),
            "WEAK_PASSWORD" to
                listOf(
                    "Use at least 12 characters",
                    "Include uppercase and lowercase letters",
                    "Include at least one number",
                    "Include at least one special character (!@#\$%^&*)",
                ),
        )
}

data class SanitizedError(
    val message: String,
    val errorId: String,
    val timestamp: Instant,
    val validationErrors: List<SanitizedValidationError>,
    val suggestions: List<String>? = null,
    val actions: List<ErrorAction>? = null,
    val details: Map<String, String>? = null,
)

data class SanitizedValidationError(
    val field: String,
    val message: String,
    val code: String? = null,
)

data class ErrorAction(
    val label: String,
    val url: String,
    val method: String = "GET",
)

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
}
