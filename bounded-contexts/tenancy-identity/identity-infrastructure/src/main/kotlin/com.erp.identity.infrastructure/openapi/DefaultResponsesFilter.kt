package com.erp.identity.infrastructure.openapi

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
            operation.responses ?: run {
                val r = OASFactory.createAPIResponses()
                operation.responses = r
                r
            }

        val defaults =
            listOf(
                "400" to "Bad Request",
                "401" to "Unauthorized",
                "403" to "Forbidden",
                "404" to "Not Found",
                "409" to "Conflict",
                "422" to "Unprocessable Entity",
                "500" to "Internal Server Error",
            )

        defaults.forEach { (code, desc) ->
            if (responses.getAPIResponse(code) == null) {
                responses.addAPIResponse(code, errorResponse(desc))
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
}
