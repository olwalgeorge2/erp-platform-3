package com.erp.financial.shared.validation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.util.UUID

/**
 * Structured JSON logging for validation failures.
 * Provides comprehensive audit trail for SOX/GDPR compliance.
 *
 * Log format includes:
 * - Timestamp (ISO-8601)
 * - Correlation ID (trace ID from request context)
 * - Tenant ID (multi-tenancy context)
 * - User ID (authenticated user)
 * - Client IP (source IP address)
 * - Error code (domain-specific validation error)
 * - Field (rejected field name)
 * - Rejected value (sanitized for PII)
 * - HTTP status code
 * - Request path
 * - User agent
 * - Session ID
 */
class ValidationAuditLogger(
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules(),
) {
    private val logger = LoggerFactory.getLogger("FINANCE_VALIDATION_AUDIT")

    /**
     * Logs a validation failure with full audit context.
     * Uses MDC (Mapped Diagnostic Context) for correlation ID propagation.
     */
    fun logValidationFailure(
        errorCode: FinanceValidationErrorCode,
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
                errorCode = errorCode.code,
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

        // Log as structured JSON for easy parsing by log aggregation tools (ELK, Splunk, etc.)
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

        // Timestamp in ISO-8601 format
        event.put("@timestamp", Instant.now().toString())
        event.put("event_type", "validation_failure")
        event.put("service", "financial-management")

        // Correlation tracking
        val correlationId = MDC.get("X-Trace-Id") ?: MDC.get("traceId") ?: UUID.randomUUID().toString()
        event.put("correlation_id", correlationId)
        event.put("session_id", sessionId)

        // Multi-tenancy context
        tenantId?.let { event.put("tenant_id", it.toString()) }
        userId?.let { event.put("user_id", it.toString()) }

        // Security context
        clientIp?.let { event.put("client_ip", it) }
        userAgent?.let { event.put("user_agent", it) }

        // Validation error details
        event.put("error_code", errorCode)
        event.put("field", field)
        rejectedValue?.let { event.put("rejected_value", it) }
        event.put("http_status", httpStatus)
        requestPath?.let { event.put("request_path", it) }

        // Compliance flags
        event.put("requires_sox_review", isSoxRelevant(errorCode))
        event.put("contains_pii", isPiiField(field))
        event.put("severity", calculateSeverity(errorCode))

        return event
    }

    /**
     * Sanitizes rejected values to avoid logging sensitive data (PII, credentials).
     * Truncates long values and masks sensitive patterns.
     */
    private fun sanitizeForLogging(value: String?): String? {
        if (value == null) return null

        // Truncate very long values
        val truncated = if (value.length > 100) value.take(100) + "..." else value

        // Mask patterns that look like sensitive data
        return truncated
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "***@***.***") // Email
            .replace(Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"), "***-**-****") // SSN
            .replace(Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"), "****-****-****-****") // Credit card
    }

    /**
     * Determines if validation error is relevant for SOX compliance.
     * Financial transaction errors require heightened audit scrutiny.
     */
    private fun isSoxRelevant(errorCode: String): Boolean =
        errorCode.contains("JOURNAL") ||
            errorCode.contains("ACCOUNT") ||
            errorCode.contains("LEDGER") ||
            errorCode.contains("TRANSACTION") ||
            errorCode.contains("POSTING") ||
            errorCode.contains("BALANCE")

    /**
     * Identifies fields containing Personally Identifiable Information.
     * Used for GDPR compliance and data retention policies.
     */
    private fun isPiiField(field: String): Boolean {
        val piiPatterns =
            listOf(
                "email",
                "phone",
                "address",
                "name",
                "ssn",
                "tax",
                "contact",
                "customer",
                "vendor",
                "employee",
            )
        return piiPatterns.any { field.lowercase().contains(it) }
    }

    /**
     * Calculates severity level for alerting and escalation.
     */
    private fun calculateSeverity(errorCode: String): String =
        when {
            errorCode.contains("CRITICAL") || errorCode.contains("FRAUD") -> "CRITICAL"
            errorCode.contains("DUPLICATE") || errorCode.contains("CONFLICT") -> "HIGH"
            errorCode.contains("INVALID") || errorCode.contains("MISSING") -> "MEDIUM"
            else -> "LOW"
        }

    companion object {
        /**
         * Extracts client IP from common request headers.
         * Handles proxy/load balancer headers (X-Forwarded-For, X-Real-IP).
         */
        fun extractClientIp(headers: Map<String, String>): String? =
            headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
                ?: headers["X-Real-IP"]
                ?: headers["Remote-Addr"]

        /**
         * Extracts user agent from request headers.
         */
        fun extractUserAgent(headers: Map<String, String>): String? = headers["User-Agent"]

        /**
         * Extracts session ID from request headers or cookies.
         */
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
