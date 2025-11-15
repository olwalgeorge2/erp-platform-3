package com.erp.identity.infrastructure.adapter.input.rest.dto

import com.erp.identity.application.port.input.query.GetTenantQuery
import com.erp.identity.application.port.input.query.ListTenantsQuery
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.identity.infrastructure.validation.IdentityValidationException
import com.erp.identity.infrastructure.validation.ValidationErrorCode
import com.erp.identity.infrastructure.validation.ValidationMessageResolver
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.util.Locale
import java.util.UUID

data class GetTenantRequest(
    @field:NotNull
    @field:PathParam("tenantId")
    var tenantId: UUID? = null,
) {
    fun toQuery(locale: Locale): GetTenantQuery =
        GetTenantQuery(
            tenantId = tenantId?.let { TenantId(it) } ?: throw invalidTenantId(locale),
        )

    private fun invalidTenantId(locale: Locale): IdentityValidationException =
        IdentityValidationException(
            errorCode = ValidationErrorCode.TENANCY_INVALID_TENANT_ID,
            field = "tenantId",
            rejectedValue = null,
            locale = locale,
            message = ValidationMessageResolver.resolve(ValidationErrorCode.TENANCY_INVALID_TENANT_ID, locale),
        )
}

data class ListTenantsRequest(
    @field:QueryParam("status")
    var status: String? = null,
    @field:QueryParam("limit")
    @field:Min(1)
    @field:Max(1000)
    var limit: Int? = null,
    @field:QueryParam("offset")
    @field:Min(0)
    var offset: Int? = null,
) {
    fun toQuery(locale: Locale): ListTenantsQuery {
        val statusEnum =
            status?.let { raw ->
                try {
                    TenantStatus.valueOf(raw.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw IdentityValidationException(
                        errorCode = ValidationErrorCode.TENANCY_INVALID_STATUS,
                        field = "status",
                        rejectedValue = raw,
                        locale = locale,
                        message =
                            ValidationMessageResolver.resolve(
                                ValidationErrorCode.TENANCY_INVALID_STATUS,
                                locale,
                                raw,
                                TenantStatus.entries.joinToString(),
                            ),
                    )
                }
            }

        val resolvedLimit = limit ?: 50
        if (resolvedLimit !in 1..1000) {
            throw IdentityValidationException(
                errorCode = ValidationErrorCode.TENANCY_INVALID_PAGE_LIMIT,
                field = "limit",
                rejectedValue = resolvedLimit.toString(),
                locale = locale,
                message =
                    ValidationMessageResolver.resolve(
                        ValidationErrorCode.TENANCY_INVALID_PAGE_LIMIT,
                        locale,
                        1,
                        1000,
                    ),
            )
        }

        val resolvedOffset = offset ?: 0
        if (resolvedOffset < 0) {
            throw IdentityValidationException(
                errorCode = ValidationErrorCode.TENANCY_INVALID_PAGE_OFFSET,
                field = "offset",
                rejectedValue = resolvedOffset.toString(),
                locale = locale,
                message =
                    ValidationMessageResolver.resolve(
                        ValidationErrorCode.TENANCY_INVALID_PAGE_OFFSET,
                        locale,
                        0,
                    ),
            )
        }

        return ListTenantsQuery(
            status = statusEnum,
            limit = resolvedLimit,
            offset = resolvedOffset,
        )
    }
}
