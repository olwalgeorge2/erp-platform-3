package com.erp.apigateway.validation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.util.UUID

/**
 * Structured JSON logging for API Gateway validation failures.
 * Provides edge-level audit trail for security and compliance.
 *
 * Gateway-specific concerns:
 * - Rate limit violations
 * - Routing failures
 * - Authentication/authorization at edge
 * - Request validation before service routing
 * - DDoS detection patterns
 */
class GatewayValidationAuditLogger(
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules(),
) {
    private val logger = LoggerFactory.getLogger("GATEWAY_VALIDATION_AUDIT")

    /**
     * Logs a validation failure at the gateway boundary.
     */
    fun logValidationFailure(
        errorCode: String,
        field: String,
        rejectedValue: String?,
        tenantId: String?,
        clientIp: String?,
        requestPath: String?,
        userAgent: String?,
        httpStatus: Int = 422,
    ) {
        val auditEvent =
            createAuditEvent(
                errorCode = errorCode,
                field = field,
                rejectedValue = sanitizeForLogging(rejectedValue),
                tenantId = tenantId,
                clientIp = clientIp,
                requestPath = requestPath,
                userAgent = userAgent,
                httpStatus = httpStatus,
            )

        logger.warn(objectMapper.writeValueAsString(auditEvent))
    }

    private fun createAuditEvent(
        errorCode: String,
        field: String,
        rejectedValue: String?,
        tenantId: String?,
        clientIp: String?,
        requestPath: String?,
        userAgent: String?,
        httpStatus: Int,
    ): ObjectNode {
        val event = objectMapper.createObjectNode()

        event.put("@timestamp", Instant.now().toString())
        event.put("event_type", "gateway_validation_failure")
        event.put("service", "api-gateway")
        event.put("layer", "edge")

        // Correlation tracking (gateway is entry point, generates trace ID)
        val correlationId = MDC.get("X-Trace-Id") ?: UUID.randomUUID().toString()
        event.put("correlation_id", correlationId)

        // Gateway context
        tenantId?.let { event.put("tenant_id", it) }
        clientIp?.let { event.put("client_ip", it) }
        userAgent?.let { event.put("user_agent", it) }
        requestPath?.let { event.put("request_path", it) }

        // Error details
        event.put("error_code", errorCode)
        event.put("field", field)
        rejectedValue?.let { event.put("rejected_value", it) }
        event.put("http_status", httpStatus)

        // Security flags
        event.put("is_rate_limit_violation", isRateLimitViolation(errorCode))
        event.put("is_routing_failure", isRoutingFailure(errorCode))
        event.put("requires_security_review", requiresSecurityReview(errorCode))
        event.put("severity", calculateSeverity(errorCode))

        return event
    }

    private fun sanitizeForLogging(value: String?): String? {
        if (value == null) return null
        val truncated = if (value.length > 100) value.take(100) + "..." else value

        return truncated
            .replace(Regex("(?i)password|token|apikey|secret", RegexOption.IGNORE_CASE), "***REDACTED***")
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "***@***.***")
    }

    private fun isRateLimitViolation(errorCode: String): Boolean =
        errorCode.contains("RATE_LIMIT") || errorCode.contains("THROTTLE")

    private fun isRoutingFailure(errorCode: String): Boolean =
        errorCode.contains("ROUTE") || errorCode.contains("SERVICE_UNAVAILABLE")

    private fun requiresSecurityReview(errorCode: String): Boolean =
        isRateLimitViolation(errorCode) ||
            errorCode.contains("UNAUTHORIZED") ||
            errorCode.contains("FORBIDDEN") ||
            errorCode.contains("INVALID_TOKEN")

    private fun calculateSeverity(errorCode: String): String =
        when {
            errorCode.contains("DDOS") || errorCode.contains("ATTACK") -> "CRITICAL"
            isRateLimitViolation(errorCode) -> "HIGH"
            isRoutingFailure(errorCode) -> "MEDIUM"
            else -> "LOW"
        }

    companion object {
        fun extractClientIp(headers: Map<String, String>): String? =
            headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
                ?: headers["X-Real-IP"]
                ?: headers["Remote-Addr"]

        fun extractUserAgent(headers: Map<String, String>): String? = headers["User-Agent"]

        fun extractTenantId(headers: Map<String, String>): String? = headers["X-Tenant-Id"]
    }
}
