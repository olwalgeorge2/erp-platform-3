package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.validation.IdentityValidationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class IdentityValidationExceptionMapper : ExceptionMapper<IdentityValidationException> {
    override fun toResponse(exception: IdentityValidationException): Response =
        Response
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

    companion object {
        private val UNPROCESSABLE_ENTITY_STATUS =
            Response.Status.fromStatusCode(422) ?: Response.Status.BAD_REQUEST
    }
}
