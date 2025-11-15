package com.erp.apigateway.exception

import com.erp.apigateway.routing.RouteNotFoundException
import com.erp.apigateway.tracing.TraceContext
import com.erp.apigateway.validation.GatewayValidationAuditLogger
import com.erp.apigateway.validation.GatewayValidationException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    name = "GatewayErrorResponse",
    description = "Standard error envelope emitted by the API Gateway",
)
data class ErrorResponse(
    val code: String,
    val message: String,
    val traceId: String? = null,
    val validationErrors: List<ValidationErrorResponse> = emptyList(),
)

@Schema(
    name = "GatewayValidationError",
    description = "Details for a specific validation violation",
)
data class ValidationErrorResponse(
    val field: String,
    val code: String,
    val message: String,
    val rejectedValue: String?,
)

@Provider
class GatewayExceptionMapper : ExceptionMapper<Throwable> {
    @Inject
    var traceContext: TraceContext? = null

    @Inject
    var meterRegistry: io.micrometer.core.instrument.MeterRegistry? = null

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Context
    lateinit var requestContext: ContainerRequestContext

    private val auditLogger by lazy { GatewayValidationAuditLogger(objectMapper) }

    override fun toResponse(exception: Throwable): Response {
        if (exception is GatewayValidationException) {
            return validationErrorResponse(exception)
        }
        val (status, code, message) =
            when (exception) {
                is RouteNotFoundException ->
                    Triple(
                        Response.Status.NOT_FOUND,
                        "ROUTE_NOT_FOUND",
                        exception.message ?: "Route not found",
                    )

                else ->
                    Triple(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                    )
            }

        val body = ErrorResponse(code = code, message = message, traceId = traceContext?.traceId)

        return Response
            .status(status)
            .entity(body)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build()
    }

    private fun validationErrorResponse(exception: GatewayValidationException): Response {
        // Track validation errors for monitoring and alerting
        meterRegistry
            ?.counter(
                "gateway.validation.errors",
                "error_code",
                exception.errorCode.code,
                "field",
                exception.field,
                "http_status",
                "422",
            )?.increment()

        // Enhanced audit logging at gateway edge
        logValidationFailure(exception)

        val violation =
            ValidationErrorResponse(
                field = exception.field,
                code = exception.errorCode.code,
                message = exception.message ?: exception.errorCode.code,
                rejectedValue = exception.rejectedValue,
            )
        val body =
            ErrorResponse(
                code = exception.errorCode.code,
                message = exception.message ?: exception.errorCode.code,
                traceId = traceContext?.traceId,
                validationErrors = listOf(violation),
            )
        return Response
            .status(UNPROCESSABLE_ENTITY)
            .entity(body)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build()
    }

    private fun logValidationFailure(exception: GatewayValidationException) {
        // Skip audit logging if requestContext is not initialized (e.g., in unit tests)
        if (!this::requestContext.isInitialized) {
            return
        }

        val headers = extractHeaders()
        auditLogger.logValidationFailure(
            errorCode = exception.errorCode.code,
            field = exception.field,
            rejectedValue = exception.rejectedValue,
            tenantId = GatewayValidationAuditLogger.extractTenantId(headers),
            clientIp = GatewayValidationAuditLogger.extractClientIp(headers),
            requestPath = requestContext.uriInfo?.requestUri?.path,
            userAgent = GatewayValidationAuditLogger.extractUserAgent(headers),
            httpStatus = 422,
        )
    }

    private fun extractHeaders(): Map<String, String> =
        requestContext.headers.entries.associate { (key, values) ->
            key to values.firstOrNull().orEmpty()
        }

    companion object {
        private val UNPROCESSABLE_ENTITY =
            object : Response.StatusType {
                override fun getStatusCode(): Int = 422

                override fun getReasonPhrase(): String = "Unprocessable Entity"

                override fun getFamily(): Response.Status.Family = Response.Status.Family.CLIENT_ERROR
            }
    }
}
