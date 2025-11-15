package com.erp.identity.infrastructure.adapter.input.rest.dto

import com.erp.identity.application.port.input.command.DeleteRoleCommand
import com.erp.identity.application.port.input.query.ListRolesQuery
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
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

data class GetRoleRequest(
    @field:NotNull
    @field:PathParam("tenantId")
    var tenantId: UUID? = null,
    @field:NotNull
    @field:PathParam("roleId")
    var roleId: UUID? = null,
) {
    fun toTenantId(locale: Locale): TenantId = tenantId?.let { TenantId(it) } ?: throw invalidTenantId(locale)

    fun toRoleId(locale: Locale): RoleId = roleId?.let { RoleId(it) } ?: throw invalidRoleId(locale)

    private fun invalidTenantId(locale: Locale): IdentityValidationException =
        IdentityValidationException(
            errorCode = ValidationErrorCode.TENANCY_INVALID_TENANT_ID,
            field = "tenantId",
            rejectedValue = null,
            locale = locale,
            message = ValidationMessageResolver.resolve(ValidationErrorCode.TENANCY_INVALID_TENANT_ID, locale),
        )

    private fun invalidRoleId(locale: Locale): IdentityValidationException =
        IdentityValidationException(
            errorCode = ValidationErrorCode.TENANCY_INVALID_ROLE_ID,
            field = "roleId",
            rejectedValue = null,
            locale = locale,
            message = ValidationMessageResolver.resolve(ValidationErrorCode.TENANCY_INVALID_ROLE_ID, locale),
        )
}

data class ListRolesRequest(
    @field:NotNull
    @field:PathParam("tenantId")
    var tenantId: UUID? = null,
    @field:QueryParam("limit")
    @field:Min(1)
    @field:Max(1000)
    var limit: Int? = null,
    @field:QueryParam("offset")
    @field:Min(0)
    var offset: Int? = null,
) {
    fun toQuery(locale: Locale): ListRolesQuery {
        val resolvedTenant =
            tenantId?.let { TenantId(it) } ?: throw IdentityValidationException(
                errorCode = ValidationErrorCode.TENANCY_INVALID_TENANT_ID,
                field = "tenantId",
                rejectedValue = null,
                locale = locale,
                message = ValidationMessageResolver.resolve(ValidationErrorCode.TENANCY_INVALID_TENANT_ID, locale),
            )

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

        return ListRolesQuery(
            tenantId = resolvedTenant,
            limit = resolvedLimit,
            offset = resolvedOffset,
        )
    }
}

data class DeleteRoleRequest(
    @field:NotNull
    @field:PathParam("tenantId")
    var tenantId: UUID? = null,
    @field:NotNull
    @field:PathParam("roleId")
    var roleId: UUID? = null,
) {
    fun toCommand(locale: Locale): DeleteRoleCommand =
        DeleteRoleCommand(
            tenantId = tenantId?.let { TenantId(it) } ?: throw tenantException(locale),
            roleId = roleId?.let { RoleId(it) } ?: throw roleException(locale),
        )

    private fun tenantException(locale: Locale): IdentityValidationException =
        IdentityValidationException(
            errorCode = ValidationErrorCode.TENANCY_INVALID_TENANT_ID,
            field = "tenantId",
            rejectedValue = null,
            locale = locale,
            message = ValidationMessageResolver.resolve(ValidationErrorCode.TENANCY_INVALID_TENANT_ID, locale),
        )

    private fun roleException(locale: Locale): IdentityValidationException =
        IdentityValidationException(
            errorCode = ValidationErrorCode.TENANCY_INVALID_ROLE_ID,
            field = "roleId",
            rejectedValue = null,
            locale = locale,
            message = ValidationMessageResolver.resolve(ValidationErrorCode.TENANCY_INVALID_ROLE_ID, locale),
        )
}
