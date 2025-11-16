package com.erp.financial.shared.rest

import com.erp.financial.shared.api.ErrorResponse
import com.erp.financial.shared.validation.metrics.ValidationMetrics
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.USER - 10)
class ValidationMetricsFilter : ContainerRequestFilter, ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        requestContext.setProperty(ValidationMetrics.REQUEST_START_ATTRIBUTE, System.nanoTime())
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val start = requestContext.getProperty(ValidationMetrics.REQUEST_START_ATTRIBUTE) as? Long ?: return
        val duration = System.nanoTime() - start
        val pathTemplate = buildPathTemplate(requestContext)
        val validationFailure =
            responseContext.status in 400..499 && responseContext.entity is ErrorResponse
        ValidationMetrics.recordRequest(
            method = requestContext.method,
            pathTemplate = pathTemplate,
            status = responseContext.status,
            durationNanos = duration,
            validationFailure = validationFailure,
        )
    }

    private fun buildPathTemplate(requestContext: ContainerRequestContext): String {
        val templates = requestContext.uriInfo.matchedTemplates
        if (templates.isEmpty()) {
            return "/" + requestContext.uriInfo.path.trimStart('/')
        }
        return templates
            .reversed()
            .joinToString(prefix = "/", separator = "") { tmpl -> tmpl.template.trim('/') }
            .replace("//", "/")
    }
}
