package com.erp.financial.shared.rest

import com.erp.financial.shared.api.ErrorResponse
import com.erp.financial.shared.api.ValidationError
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.FinanceValidationException
import com.erp.financial.shared.validation.ValidationAuditLogger
import com.erp.financial.shared.validation.ValidationMessageResolver
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import java.util.UUID

/**
 * Maps FinanceValidationException to HTTP 422 responses with structured error payload.
 * Tracks validation error metrics for observability.
 */
@Provider
class FinanceValidationExceptionMapper : ExceptionMapper<FinanceValidationException> {
    @Inject
    lateinit var meterRegistry: MeterRegistry

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Context
    lateinit var requestContext: ContainerRequestContext

    private val auditLogger by lazy { ValidationAuditLogger(objectMapper) }

    override fun toResponse(exception: FinanceValidationException): Response {
        // Track validation error metrics
        meterRegistry
            .counter(
                "finance.validation.errors",
                "error_code",
                exception.errorCode.code,
                "field",
                exception.field,
                "http_status",
                UNPROCESSABLE_ENTITY.toString(),
            ).increment()

        // Enhanced audit logging with full context
        logValidationFailure(exception)

        val safeMessage =
            if (SECURE_ERROR_CODES.contains(exception.errorCode)) {
                ValidationMessageResolver.resolve(
                    exception.errorCode,
                    exception.locale,
                    exception.field,
                )
            } else {
                exception.message
                    ?: ValidationMessageResolver.resolve(exception.errorCode, exception.locale, exception.field)
            }

        return Response
            .status(UNPROCESSABLE_ENTITY)
            .entity(
                ErrorResponse(
                    code = exception.errorCode.code,
                    message = safeMessage,
                    details = emptyMap(),
                    validationErrors =
                        listOf(
                            ValidationError(
                                field = exception.field,
                                code = exception.errorCode.code,
                                message = safeMessage,
                                rejectedValue = exception.rejectedValue,
                            ),
                        ),
                ),
            ).type(MediaType.APPLICATION_JSON_TYPE)
            .build()
    }

    private fun logValidationFailure(exception: FinanceValidationException) {
        val headers = extractHeaders()

        auditLogger.logValidationFailure(
            errorCode = exception.errorCode,
            field = exception.field,
            rejectedValue = exception.rejectedValue,
            tenantId = extractTenantId(),
            userId = extractUserId(),
            clientIp = ValidationAuditLogger.extractClientIp(headers),
            requestPath = requestContext.uriInfo?.requestUri?.path,
            userAgent = ValidationAuditLogger.extractUserAgent(headers),
            sessionId = ValidationAuditLogger.extractSessionId(headers),
            httpStatus = UNPROCESSABLE_ENTITY,
        )
    }

    private fun extractHeaders(): Map<String, String> =
        requestContext.headers.entries.associate { (key, values) ->
            key to values.firstOrNull().orEmpty()
        }

    private fun extractTenantId(): UUID? =
        requestContext.getHeaderString("X-Tenant-Id")?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

    private fun extractUserId(): UUID? =
        requestContext.getHeaderString("X-User-Id")?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

    companion object {
        private const val UNPROCESSABLE_ENTITY = 422
        private val SECURE_ERROR_CODES =
            setOf(
                FinanceValidationErrorCode.FINANCE_RATE_LIMIT_EXCEEDED,
                FinanceValidationErrorCode.FINANCE_DEPENDENCY_UNAVAILABLE,
            )
    }
}
