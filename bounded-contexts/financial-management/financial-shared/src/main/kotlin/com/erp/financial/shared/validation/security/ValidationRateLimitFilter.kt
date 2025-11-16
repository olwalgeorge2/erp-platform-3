package com.erp.financial.shared.validation.security

import com.erp.financial.shared.api.ErrorResponse
import com.erp.financial.shared.validation.FinanceValidationErrorCode
import com.erp.financial.shared.validation.ValidationMessageResolver
import com.erp.financial.shared.validation.metrics.ValidationMetrics
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.ext.Provider
import java.util.Locale
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@Provider
@Priority(Priorities.AUTHENTICATION - 10)
class ValidationRateLimitFilter
    @Inject
    constructor(
        private val rateLimiter: ValidationRateLimiter,
        @ConfigProperty(name = "validation.security.rate-limit.enabled", defaultValue = "true")
        private val enabled: Boolean,
        @ConfigProperty(name = "validation.security.abuse.threshold", defaultValue = "25")
        private val abuseThreshold: Int,
    ) : ContainerRequestFilter {
        private val logger = Logger.getLogger(ValidationRateLimitFilter::class.java)

        @Context
        private var securityContext: SecurityContext? = null

        override fun filter(requestContext: ContainerRequestContext) {
            if (!enabled) {
                return
            }
            val path = "/" + requestContext.uriInfo.path.trimStart('/')
            val clientIp = extractClientIp(requestContext)
            val ipDecision = rateLimiter.checkIpLimit(clientIp, path)
            if (!ipDecision.allowed) {
                handleViolation(requestContext, ipDecision, path)
                return
            }

            val userDecision = rateLimiter.checkUserLimit(securityContext?.userPrincipal?.name)
            if (userDecision != null && !userDecision.allowed) {
                handleViolation(requestContext, userDecision, path)
            }
        }

    private fun handleViolation(
        requestContext: ContainerRequestContext,
        decision: RateLimitDecision,
        path: String,
    ) {
        ValidationMetrics.recordRateLimitViolation(decision.bucket, decision.keyType)
        val message =
            ValidationMessageResolver.resolve(
                FinanceValidationErrorCode.FINANCE_RATE_LIMIT_EXCEEDED,
                localeFromHeaders(requestContext),
                path,
            )
            val response =
                ErrorResponse(
                    code = FinanceValidationErrorCode.FINANCE_RATE_LIMIT_EXCEEDED.code,
                    message = message,
                    validationErrors = emptyList(),
                )
            logger.warnf(
                "Finance validation rate limit exceeded bucket=%s keyType=%s key=%s path=%s remaining=%d reset=%d",
                decision.bucket,
                decision.keyType,
                decision.key,
                path,
                decision.remaining,
                decision.resetEpochSeconds,
            )
        requestContext.abortWith(
                Response
                    .status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(response)
                    .header("X-RateLimit-Limit", rateLimiter.bucketLimit(decision.bucket))
                    .header("X-RateLimit-Remaining", decision.remaining)
                    .header("X-RateLimit-Reset", decision.resetEpochSeconds)
                    .build(),
            )
    }

    private fun extractClientIp(requestContext: ContainerRequestContext): String {
        val forwarded = requestContext.getHeaderString("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(',').first().trim()
        }
        val realIp = requestContext.getHeaderString("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }
        return requestContext.headers["remote-ip"]?.firstOrNull() ?: "unknown"
    }

    private fun localeFromHeaders(requestContext: ContainerRequestContext): Locale {
        val header = requestContext.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE)
        return if (header.isNullOrBlank()) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(header)
        }
    }
}
