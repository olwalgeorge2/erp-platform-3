package com.erp.financial.shared.rest

import com.erp.financial.shared.api.ErrorResponse
import com.erp.financial.shared.validation.metrics.ValidationMetrics
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

@Provider
@Priority(Priorities.USER)
class ValidationAuditFilter : ContainerResponseFilter {
    private val logger = Logger.getLogger(ValidationAuditFilter::class.java)

    @Context
    private var securityContext: SecurityContext? = null

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        if (responseContext.status !in 400..499) {
            return
        }
        val body = responseContext.entity as? ErrorResponse ?: return
        if (body.validationErrors.isEmpty()) {
            return
        }

        val durationMs =
            ValidationMetrics.durationMillis(
                requestContext.getProperty(ValidationMetrics.REQUEST_START_ATTRIBUTE) as? Long,
            )

        val user = securityContext?.userPrincipal?.name ?: "anonymous"
        val path = requestContext.uriInfo.path
        logger.warnf(
            "Finance validation failure user=%s path=%s status=%d code=%s violations=%s duration_ms=%s",
            user,
            path,
            responseContext.status,
            body.code,
            body.validationErrors.joinToString { "${it.field}:${it.code}" },
            durationMs ?: "n/a",
        )

        if (durationMs != null && durationMs > SLOW_VALIDATION_THRESHOLD_MS) {
            logger.warnf(
                "Finance slow validation detected path=%s duration_ms=%d code=%s",
                path,
                durationMs,
                body.code,
            )
        }
    }

    companion object {
        private const val SLOW_VALIDATION_THRESHOLD_MS = 50L
    }
}
