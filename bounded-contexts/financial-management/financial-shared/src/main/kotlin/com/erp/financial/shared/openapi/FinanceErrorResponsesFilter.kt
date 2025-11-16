package com.erp.financial.shared.openapi

import org.eclipse.microprofile.openapi.OASFactory
import org.eclipse.microprofile.openapi.OASFilter
import org.eclipse.microprofile.openapi.models.Operation
import org.eclipse.microprofile.openapi.models.media.Content
import org.eclipse.microprofile.openapi.models.media.MediaType
import org.eclipse.microprofile.openapi.models.media.Schema
import org.eclipse.microprofile.openapi.models.responses.APIResponse

/**
 * Ensures every operation documents the shared finance validation error envelope so UI and
 * integration consumers can rely on the expanded error-code set exposed via ValidationProblemDetail.
 */
class FinanceErrorResponsesFilter : OASFilter {
    override fun filterOperation(operation: Operation?): Operation? {
        if (operation == null) {
            return null
        }

        val responses =
            operation.responses ?: OASFactory.createAPIResponses().also { operation.responses = it }

        DEFAULT_RESPONSES.forEach { (code, description) ->
            if (responses.getAPIResponse(code) == null) {
                responses.addAPIResponse(code, errorResponse(description))
            }
        }

        return operation
    }

    private fun errorResponse(description: String): APIResponse {
        val schema: Schema = OASFactory.createSchema().ref("#/components/schemas/ValidationProblemDetail")
        val media: MediaType = OASFactory.createMediaType().schema(schema)
        val content: Content = OASFactory.createContent().addMediaType("application/json", media)
        return OASFactory.createAPIResponse().description(description).content(content)
    }

    companion object {
        private val DEFAULT_RESPONSES =
            listOf(
                "400" to "Bad Request",
                "401" to "Unauthorized",
                "403" to "Forbidden",
                "404" to "Not Found",
                "409" to "Conflict",
                "422" to "Unprocessable Entity",
                "500" to "Internal Server Error",
            )
    }
}
