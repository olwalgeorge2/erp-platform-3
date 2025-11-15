package com.erp.identity.infrastructure.adapter.input.rest

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

        val entity = responseContext.entity
        val errorResponse = entity as? ErrorResponse ?: return

        val principal = securityContext?.userPrincipal?.name ?: "anonymous"
        val path = requestContext.uriInfo.path

        logger.warnf(
            "Validation failure - user=%s path=%s status=%d code=%s violations=%s",
            principal,
            path,
            responseContext.status,
            errorResponse.code,
            errorResponse.validationErrors.joinToString { "${it.field}:${it.code}" },
        )
    }
}
