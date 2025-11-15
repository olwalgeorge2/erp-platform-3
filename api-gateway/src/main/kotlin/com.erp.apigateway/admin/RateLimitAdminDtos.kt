package com.erp.apigateway.admin

import com.erp.apigateway.validation.GatewayValidationErrorCode
import com.erp.apigateway.validation.GatewayValidationException
import com.erp.apigateway.validation.ValidationMessageResolver
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.PathParam
import java.util.Locale

data class TenantOverridePathRequest(
    @field:NotBlank(message = "tenant must not be blank")
    @field:PathParam("tenant")
    var tenant: String? = null,
) {
    fun tenantId(locale: Locale): String {
        val value = tenant?.trim()
        if (value.isNullOrEmpty()) {
            throw GatewayValidationException(
                errorCode = GatewayValidationErrorCode.GATEWAY_INVALID_TENANT_ID,
                field = "tenant",
                rejectedValue = tenant,
                locale = locale,
                message =
                    ValidationMessageResolver.resolve(
                        GatewayValidationErrorCode.GATEWAY_INVALID_TENANT_ID,
                        locale,
                        tenant ?: "",
                    ),
            )
        }
        return value
    }
}

data class EndpointOverridePathRequest(
    @field:NotBlank(message = "pattern must not be blank")
    @field:PathParam("pattern")
    var pattern: String? = null,
) {
    fun pattern(locale: Locale): String {
        val value = pattern?.trim()
        if (value.isNullOrEmpty()) {
            throw GatewayValidationException(
                errorCode = GatewayValidationErrorCode.GATEWAY_INVALID_PATTERN,
                field = "pattern",
                rejectedValue = pattern,
                locale = locale,
                message =
                    ValidationMessageResolver.resolve(
                        GatewayValidationErrorCode.GATEWAY_INVALID_PATTERN,
                        locale,
                        pattern ?: "",
                    ),
            )
        }
        return value
    }
}

data class OverrideRequest(
    @field:Min(1, message = "requestsPerMinute must be at least 1")
    val requestsPerMinute: Int,
    @field:Min(1, message = "windowSeconds must be at least 1")
    val windowSeconds: Int,
) {
    fun validate(locale: Locale) {
        if (requestsPerMinute < 1) {
            throw GatewayValidationException(
                errorCode = GatewayValidationErrorCode.GATEWAY_INVALID_REQUESTS_PER_MINUTE,
                field = "requestsPerMinute",
                rejectedValue = requestsPerMinute.toString(),
                locale = locale,
                message =
                    ValidationMessageResolver.resolve(
                        GatewayValidationErrorCode.GATEWAY_INVALID_REQUESTS_PER_MINUTE,
                        locale,
                        1,
                    ),
            )
        }
        if (windowSeconds < 1) {
            throw GatewayValidationException(
                errorCode = GatewayValidationErrorCode.GATEWAY_INVALID_WINDOW_SECONDS,
                field = "windowSeconds",
                rejectedValue = windowSeconds.toString(),
                locale = locale,
                message =
                    ValidationMessageResolver.resolve(
                        GatewayValidationErrorCode.GATEWAY_INVALID_WINDOW_SECONDS,
                        locale,
                        1,
                    ),
            )
        }
    }
}
