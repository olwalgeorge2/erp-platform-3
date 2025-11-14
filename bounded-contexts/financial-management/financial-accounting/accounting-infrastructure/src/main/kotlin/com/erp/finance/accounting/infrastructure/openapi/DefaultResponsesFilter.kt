package com.erp.finance.accounting.infrastructure.openapi

import org.eclipse.microprofile.openapi.OASFactory
import org.eclipse.microprofile.openapi.OASFilter
import org.eclipse.microprofile.openapi.models.Operation
import org.eclipse.microprofile.openapi.models.media.Content
import org.eclipse.microprofile.openapi.models.media.MediaType
import org.eclipse.microprofile.openapi.models.media.Schema
import org.eclipse.microprofile.openapi.models.responses.APIResponse

class DefaultResponsesFilter : OASFilter {
    override fun filterOperation(operation: Operation?): Operation? {
        if (operation == null) return null

        val responses =
            operation.responses ?: OASFactory.createAPIResponses().also { operation.responses = it }

        listOf(
            "400" to "Bad Request",
            "401" to "Unauthorized",
            "403" to "Forbidden",
            "404" to "Not Found",
            "409" to "Conflict",
            "422" to "Unprocessable Entity",
            "500" to "Internal Server Error",
        ).forEach { (code, description) ->
            if (responses.getAPIResponse(code) == null) {
                responses.addAPIResponse(code, defaultErrorResponse(description))
            }
        }

        return operation
    }

    private fun defaultErrorResponse(description: String): APIResponse {
        val schema: Schema = OASFactory.createSchema().ref("#/components/schemas/ErrorResponse")
        val media: MediaType = OASFactory.createMediaType().schema(schema)
        val content: Content = OASFactory.createContent().addMediaType("application/json", media)
        return OASFactory.createAPIResponse().description(description).content(content)
    }
}
