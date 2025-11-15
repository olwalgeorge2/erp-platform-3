package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.validation.IdentityValidationAuditLogger
import com.erp.identity.infrastructure.validation.IdentityValidationException
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

@Provider
class IdentityValidationExceptionMapper : ExceptionMapper<IdentityValidationException> {
    @Inject
    lateinit var meterRegistry: MeterRegistry

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Context
    lateinit var requestContext: ContainerRequestContext

    private val auditLogger by lazy { IdentityValidationAuditLogger(objectMapper) }

    override fun toResponse(exception: IdentityValidationException): Response {
        // Track validation errors for monitoring and alerting
        meterRegistry
            .counter(
                "identity.validation.errors",
                "error_code",
                exception.errorCode.code,
                "field",
                exception.field,
                "http_status",
                "422",
            ).increment()

        // Enhanced audit logging with security context
        logValidationFailure(exception)

        return Response
            .status(UNPROCESSABLE_ENTITY_STATUS)
            .entity(
                ErrorResponse(
                    code = exception.errorCode.code,
                    message = exception.message ?: exception.errorCode.code,
                    validationErrors =
                        listOf(
                            ValidationErrorResponse(
                                field = exception.field,
                                code = exception.errorCode.code,
                                message = exception.message ?: exception.errorCode.code,
                                rejectedValue = exception.rejectedValue,
                            ),
                        ),
                ),
            ).type(MediaType.APPLICATION_JSON_TYPE)
            .build()
    }

    private fun logValidationFailure(exception: IdentityValidationException) {
        val headers = extractHeaders()

        auditLogger.logValidationFailure(
            errorCode = exception.errorCode.code,
            field = exception.field,
            rejectedValue = exception.rejectedValue,
            tenantId = extractTenantId(),
            userId = extractUserId(),
            clientIp = IdentityValidationAuditLogger.extractClientIp(headers),
            requestPath = requestContext.uriInfo?.requestUri?.path,
            userAgent = IdentityValidationAuditLogger.extractUserAgent(headers),
            sessionId = IdentityValidationAuditLogger.extractSessionId(headers),
            httpStatus = 422,
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
        private val UNPROCESSABLE_ENTITY_STATUS =
            Response.Status.fromStatusCode(422) ?: Response.Status.BAD_REQUEST
    }
}
