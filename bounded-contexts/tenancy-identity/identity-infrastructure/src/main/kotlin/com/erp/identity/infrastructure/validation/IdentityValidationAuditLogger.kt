package com.erp.identity.infrastructure.validation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.util.UUID

/**
 * Structured JSON logging for identity validation failures.
 * Provides comprehensive audit trail for security, SOX, and GDPR compliance.
 *
 * Critical for:
 * - Authentication/authorization failures
 * - Tenant provisioning audit trail
 * - User management activities
 * - Role/permission changes
 * - Security incident investigation
 */
class IdentityValidationAuditLogger(
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules(),
) {
    private val logger = LoggerFactory.getLogger("IDENTITY_VALIDATION_AUDIT")

    /**
     * Logs a validation failure with full security context.
     */
    fun logValidationFailure(
        errorCode: String,
        field: String,
        rejectedValue: String?,
        tenantId: UUID?,
        userId: UUID?,
        clientIp: String?,
        requestPath: String?,
        userAgent: String?,
        sessionId: String?,
        httpStatus: Int = 422,
    ) {
        val auditEvent =
            createAuditEvent(
                errorCode = errorCode,
                field = field,
                rejectedValue = sanitizeForLogging(rejectedValue),
                tenantId = tenantId,
                userId = userId,
                clientIp = clientIp,
                requestPath = requestPath,
                userAgent = userAgent,
                sessionId = sessionId,
                httpStatus = httpStatus,
            )

        // Log as structured JSON
        logger.warn(objectMapper.writeValueAsString(auditEvent))
    }

    private fun createAuditEvent(
        errorCode: String,
        field: String,
        rejectedValue: String?,
        tenantId: UUID?,
        userId: UUID?,
        clientIp: String?,
        requestPath: String?,
        userAgent: String?,
        sessionId: String?,
        httpStatus: Int,
    ): ObjectNode {
        val event = objectMapper.createObjectNode()

        event.put("@timestamp", Instant.now().toString())
        event.put("event_type", "identity_validation_failure")
        event.put("service", "tenancy-identity")

        // Correlation tracking
        val correlationId = MDC.get("X-Trace-Id") ?: MDC.get("traceId") ?: UUID.randomUUID().toString()
        event.put("correlation_id", correlationId)
        event.put("session_id", sessionId)

        // Security context
        tenantId?.let { event.put("tenant_id", it.toString()) }
        userId?.let { event.put("user_id", it.toString()) }
        clientIp?.let { event.put("client_ip", it) }
        userAgent?.let { event.put("user_agent", it) }

        // Validation error details
        event.put("error_code", errorCode)
        event.put("field", field)
        rejectedValue?.let { event.put("rejected_value", it) }
        event.put("http_status", httpStatus)
        requestPath?.let { event.put("request_path", it) }

        // Security flags
        event.put("is_authentication_failure", isAuthenticationFailure(errorCode, field))
        event.put("is_authorization_failure", isAuthorizationFailure(errorCode, field))
        event.put("contains_pii", isPiiField(field))
        event.put("requires_security_review", requiresSecurityReview(errorCode, field))
        event.put("severity", calculateSeverity(errorCode, field))

        return event
    }

    private fun sanitizeForLogging(value: String?): String? {
        if (value == null) return null

        val truncated = if (value.length > 100) value.take(100) + "..." else value

        // Mask sensitive patterns
        return truncated
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "***@***.***")
            .replace(Regex("(?i)password|pwd|token|secret|key", RegexOption.IGNORE_CASE), "***REDACTED***")
            .replace(Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"), "***-**-****")
    }

    private fun isAuthenticationFailure(
        errorCode: String,
        field: String,
    ): Boolean =
        errorCode.contains("AUTH") ||
            errorCode.contains("CREDENTIAL") ||
            field.lowercase().contains("password") ||
            field.lowercase().contains("username")

    private fun isAuthorizationFailure(
        errorCode: String,
        field: String,
    ): Boolean =
        errorCode.contains("PERMISSION") ||
            errorCode.contains("ROLE") ||
            errorCode.contains("ACCESS") ||
            field.lowercase().contains("permission") ||
            field.lowercase().contains("role")

    private fun isPiiField(field: String): Boolean {
        val piiPatterns =
            listOf(
                "email",
                "phone",
                "address",
                "name",
                "ssn",
                "tax",
                "username",
                "credential",
                "contact",
                "organization",
            )
        return piiPatterns.any { field.lowercase().contains(it) }
    }

    private fun requiresSecurityReview(
        errorCode: String,
        field: String,
    ): Boolean =
        isAuthenticationFailure(errorCode, field) ||
            isAuthorizationFailure(errorCode, field) ||
            errorCode.contains("TENANT_SUSPENDED") ||
            errorCode.contains("USER_LOCKED")

    private fun calculateSeverity(
        errorCode: String,
        field: String,
    ): String =
        when {
            errorCode.contains("FRAUD") || errorCode.contains("BREACH") -> "CRITICAL"
            isAuthenticationFailure(errorCode, field) -> "HIGH"
            isAuthorizationFailure(errorCode, field) -> "HIGH"
            errorCode.contains("INVALID") || errorCode.contains("MISSING") -> "MEDIUM"
            else -> "LOW"
        }

    companion object {
        fun extractClientIp(headers: Map<String, String>): String? =
            headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
                ?: headers["X-Real-IP"]
                ?: headers["Remote-Addr"]

        fun extractUserAgent(headers: Map<String, String>): String? = headers["User-Agent"]

        fun extractSessionId(headers: Map<String, String>): String? =
            headers["X-Session-Id"]
                ?: headers["Cookie"]?.let { parseCookie(it, "JSESSIONID") }

        private fun parseCookie(
            cookieHeader: String,
            name: String,
        ): String? =
            cookieHeader
                .split(";")
                .map { it.trim() }
                .firstOrNull { it.startsWith("$name=") }
                ?.substringAfter("=")
    }
}
