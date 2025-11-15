package com.erp.identity.infrastructure.adapter.input.rest.dto

import com.erp.identity.application.port.input.command.ActivateTenantCommand
import com.erp.identity.application.port.input.command.ActivateUserCommand
import com.erp.identity.domain.model.identity.Permission
import com.erp.identity.domain.model.identity.PermissionScope
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.identity.UserStatus
import com.erp.identity.domain.model.tenant.Address
import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.identity.infrastructure.validation.constraints.ValidTenantSlug
import com.erp.identity.infrastructure.validation.constraints.ValidUsername
import com.erp.identity.infrastructure.validation.sanitizeDescription
import com.erp.identity.infrastructure.validation.sanitizeEmail
import com.erp.identity.infrastructure.validation.sanitizeIndustry
import com.erp.identity.infrastructure.validation.sanitizeIpAddress
import com.erp.identity.infrastructure.validation.sanitizeName
import com.erp.identity.infrastructure.validation.sanitizePermissionIdentifier
import com.erp.identity.infrastructure.validation.sanitizePhoneNumber
import com.erp.identity.infrastructure.validation.sanitizePostalCode
import com.erp.identity.infrastructure.validation.sanitizeReason
import com.erp.identity.infrastructure.validation.sanitizeRoleName
import com.erp.identity.infrastructure.validation.sanitizeSlug
import com.erp.identity.infrastructure.validation.sanitizeTaxId
import com.erp.identity.infrastructure.validation.sanitizeUserAgent
import com.erp.identity.infrastructure.validation.sanitizeUsername
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(name = "ProvisionTenantRequest")
data class ProvisionTenantRequest
    @JsonCreator
    constructor(
        @JsonProperty("name")
        val name: String,
        @JsonProperty("slug")
        @field:ValidTenantSlug
        val slug: String,
        @JsonProperty("subscription")
        val subscription: SubscriptionPayload,
        @JsonProperty("organization")
        val organization: OrganizationPayload?,
        @JsonProperty("metadata")
        val metadata: Map<String, String>? = emptyMap(),
        @JsonProperty("requestedBy")
        val requestedBy: UUID? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand() =
            com.erp.identity.application.port.input.command.ProvisionTenantCommand(
                name = name.sanitizeName(),
                slug = slug.sanitizeSlug(),
                subscription = subscription.toDomain(),
                organization = organization?.toDomain(),
                metadata = metadata ?: emptyMap(),
                requestedBy = requestedBy,
            )
    }

@Schema(name = "Subscription")
data class SubscriptionPayload
    @JsonCreator
    constructor(
        @JsonProperty("plan")
        val plan: SubscriptionPlan,
        @JsonProperty("startDate")
        val startDate: Instant,
        @JsonProperty("endDate")
        val endDate: Instant? = null,
        @JsonProperty("maxUsers")
        val maxUsers: Int,
        @JsonProperty("maxStorage")
        val maxStorage: Long,
        @JsonProperty("features")
        val features: Set<String> = emptySet(),
    ) {
        @Schema(hidden = true)
        fun toDomain(): Subscription =
            Subscription(
                plan = plan,
                startDate = startDate,
                endDate = endDate,
                maxUsers = maxUsers,
                maxStorage = maxStorage,
                features = features,
            )
    }

@Schema(name = "Organization")
data class OrganizationPayload
    @JsonCreator
    constructor(
        @JsonProperty("legalName")
        val legalName: String,
        @JsonProperty("taxId")
        val taxId: String? = null,
        @JsonProperty("industry")
        val industry: String? = null,
        @JsonProperty("address")
        val address: AddressPayload? = null,
        @JsonProperty("contactEmail")
        val contactEmail: String,
        @JsonProperty("contactPhone")
        val contactPhone: String? = null,
    ) {
        @Schema(hidden = true)
        fun toDomain(): Organization =
            Organization(
                legalName = legalName.sanitizeName(),
                taxId = taxId?.sanitizeTaxId(),
                industry = industry?.sanitizeIndustry(),
                address = address?.toDomain(),
                contactEmail = contactEmail.sanitizeEmail(),
                contactPhone = contactPhone?.sanitizePhoneNumber(),
            )
    }

@Schema(name = "Address")
data class AddressPayload
    @JsonCreator
    constructor(
        @JsonProperty("street")
        val street: String,
        @JsonProperty("city")
        val city: String,
        @JsonProperty("state")
        val state: String? = null,
        @JsonProperty("postalCode")
        val postalCode: String,
        @JsonProperty("country")
        val country: String,
    ) {
        @Schema(hidden = true)
        fun toDomain(): Address =
            Address(
                street = street.sanitizeName(),
                city = city.sanitizeName(),
                state = state?.sanitizeName(),
                postalCode = postalCode.sanitizePostalCode(),
                country = country.sanitizeName(),
            )
    }

data class TenantResponse(
    val id: String,
    val name: String,
    val slug: String,
    val status: TenantStatus,
    val subscription: SubscriptionResponse,
    val organization: OrganizationResponse?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String>,
)

@Schema(name = "Permission")
data class PermissionPayload
    @JsonCreator
    constructor(
        @JsonProperty("resource")
        val resource: String,
        @JsonProperty("action")
        val action: String,
        @JsonProperty("scope")
        @field:Schema(description = "Scope", implementation = String::class)
        val scope: PermissionScope = PermissionScope.TENANT,
    ) {
        @Schema(hidden = true)
        fun toDomain(): Permission =
            Permission(
                resource = resource.sanitizePermissionIdentifier(),
                action = action.sanitizePermissionIdentifier(),
                scope = scope,
            )
    }

@Schema(name = "CreateRoleRequest")
data class CreateRoleRequest
    @JsonCreator
    constructor(
        @JsonProperty("name")
        val name: String,
        @JsonProperty("description")
        val description: String,
        @JsonProperty("permissions")
        @param:JsonSetter(nulls = Nulls.AS_EMPTY)
        val permissions: Set<PermissionPayload> = emptySet(),
        @JsonProperty("isSystem")
        val isSystem: Boolean = false,
        @JsonProperty("metadata")
        @param:JsonSetter(nulls = Nulls.AS_EMPTY)
        val metadata: Map<String, String> = emptyMap(),
        @JsonProperty("createdBy")
        val createdBy: String? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand(tenantId: TenantId) =
            com.erp.identity.application.port.input.command.CreateRoleCommand(
                tenantId = tenantId,
                name = name.sanitizeRoleName(),
                description = description.sanitizeDescription(),
                permissions = permissions.map { it.toDomain() }.toSet(),
                isSystem = isSystem,
                metadata = metadata,
                createdBy = createdBy,
            )
    }

@Schema(name = "UpdateRoleRequest")
data class UpdateRoleRequest
    @JsonCreator
    constructor(
        @JsonProperty("name")
        val name: String,
        @JsonProperty("description")
        val description: String,
        @JsonProperty("permissions")
        @param:JsonSetter(nulls = Nulls.AS_EMPTY)
        val permissions: Set<PermissionPayload> = emptySet(),
        @JsonProperty("metadata")
        @param:JsonSetter(nulls = Nulls.AS_EMPTY)
        val metadata: Map<String, String> = emptyMap(),
        @JsonProperty("updatedBy")
        val updatedBy: String? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand(
            tenantId: TenantId,
            roleId: RoleId,
        ) = com.erp.identity.application.port.input.command.UpdateRoleCommand(
            tenantId = tenantId,
            roleId = roleId,
            name = name.sanitizeRoleName(),
            description = description.sanitizeDescription(),
            permissions = permissions.map { it.toDomain() }.toSet(),
            metadata = metadata,
            updatedBy = updatedBy,
        )
    }

@Schema(name = "Role")
data class RoleResponse(
    val id: String,
    val tenantId: String,
    val name: String,
    val description: String,
    val permissions: Set<PermissionPayload>,
    val isSystem: Boolean,
    val metadata: Map<String, String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Schema(name = "ActivateTenantRequest")
data class ActivateTenantRequest
    @JsonCreator
    constructor(
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand(tenantId: UUID) =
            com.erp.identity.application.port.input.command.ActivateTenantCommand(
                tenantId = TenantId(tenantId),
                requestedBy = requestedBy,
            )
    }

@Schema(name = "SuspendTenantRequest")
data class SuspendTenantRequest
    @JsonCreator
    constructor(
        @JsonProperty("reason")
        val reason: String,
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
    ) {
        fun toCommand(tenantId: UUID) =
            com.erp.identity.application.port.input.command.SuspendTenantCommand(
                tenantId = TenantId(tenantId),
                reason = reason.sanitizeReason(),
                requestedBy = requestedBy,
            )
    }

@Schema(name = "ResumeTenantRequest")
data class ResumeTenantRequest
    @JsonCreator
    constructor(
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
    ) {
        fun toCommand(tenantId: UUID) =
            com.erp.identity.application.port.input.command.ResumeTenantCommand(
                tenantId = TenantId(tenantId),
                requestedBy = requestedBy,
            )
    }

data class SubscriptionResponse(
    val plan: SubscriptionPlan,
    val startDate: Instant,
    val endDate: Instant?,
    val maxUsers: Int,
    val maxStorage: Long,
    val features: Set<String>,
)

data class OrganizationResponse(
    val legalName: String,
    val taxId: String?,
    val industry: String?,
    val address: AddressResponse?,
    val contactEmail: String,
    val contactPhone: String?,
)

data class AddressResponse(
    val street: String,
    val city: String,
    val state: String?,
    val postalCode: String,
    val country: String,
)

@Schema(name = "CreateUserRequest")
data class CreateUserRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("username")
        @field:ValidUsername
        val username: String,
        @JsonProperty("email")
        val email: String,
        @JsonProperty("fullName")
        val fullName: String,
        @JsonProperty("password")
        val password: String,
        @JsonProperty("roleIds")
        @param:JsonSetter(nulls = Nulls.AS_EMPTY)
        val roleIds: Set<UUID> = emptySet(),
        @JsonProperty("metadata")
        @param:JsonSetter(nulls = Nulls.AS_EMPTY)
        val metadata: Map<String, String> = emptyMap(),
        @JsonProperty("createdBy")
        val createdBy: String? = null,
    ) {
        fun toCommand() =
            com.erp.identity.application.port.input.command.CreateUserCommand(
                tenantId = TenantId(tenantId),
                username = username.sanitizeUsername(),
                email = email.sanitizeEmail(),
                fullName = fullName.sanitizeName(),
                password = password,
                roleIds = roleIds.map { RoleId(it) }.toSet(),
                metadata = metadata,
                createdBy = createdBy,
            )
    }

@Schema(name = "AssignRoleRequest")
data class AssignRoleRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("roleId")
        val roleId: UUID,
        @JsonProperty("assignedBy")
        val assignedBy: String? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand(userId: UUID) =
            com.erp.identity.application.port.input.command.AssignRoleCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                roleId = RoleId(roleId),
                assignedBy = assignedBy,
            )
    }

@Schema(name = "ActivateUserRequest")
data class ActivateUserRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
        @JsonProperty("requirePasswordReset")
        val requirePasswordReset: Boolean? = true,
    ) {
        @Schema(hidden = true)
        fun toCommand(userId: UUID) =
            ActivateUserCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                requestedBy = requestedBy,
                requirePasswordReset = requirePasswordReset ?: true,
            )
    }

@Schema(name = "UpdateCredentialsRequest")
data class UpdateCredentialsRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("currentPassword")
        val currentPassword: String? = null,
        @JsonProperty("newPassword")
        val newPassword: String,
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand(userId: UUID) =
            com.erp.identity.application.port.input.command.UpdateCredentialsCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                currentPassword = currentPassword,
                newPassword = newPassword,
                requestedBy = requestedBy,
            )
    }

@Schema(name = "SuspendUserRequest")
data class SuspendUserRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("reason")
        val reason: String,
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
    ) {
        fun toCommand(userId: UUID) =
            com.erp.identity.application.port.input.command.SuspendUserCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                reason = reason.sanitizeReason(),
                requestedBy = requestedBy,
            )
    }

@Schema(name = "ReactivateUserRequest")
data class ReactivateUserRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
    ) {
        fun toCommand(userId: UUID) =
            com.erp.identity.application.port.input.command.ReactivateUserCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                requestedBy = requestedBy,
            )
    }

@Schema(name = "ResetPasswordRequest")
data class ResetPasswordRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("newPassword")
        val newPassword: String,
        @JsonProperty("requirePasswordChange")
        val requirePasswordChange: Boolean = true,
        @JsonProperty("requestedBy")
        val requestedBy: String? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand(userId: UUID) =
            com.erp.identity.application.port.input.command.ResetPasswordCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                newPassword = newPassword,
                requirePasswordChange = requirePasswordChange,
                requestedBy = requestedBy,
            )
    }

@Schema(name = "AuthenticateRequest")
data class AuthenticateRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("usernameOrEmail")
        val usernameOrEmail: String,
        @JsonProperty("password")
        val password: String,
        @JsonProperty("ipAddress")
        val ipAddress: String? = null,
        @JsonProperty("userAgent")
        val userAgent: String? = null,
    ) {
        @Schema(hidden = true)
        fun toCommand() =
            com.erp.identity.application.port.input.command.AuthenticateUserCommand(
                tenantId = TenantId(tenantId),
                usernameOrEmail = usernameOrEmail.sanitizeUsername(),
                password = password,
                ipAddress = ipAddress?.sanitizeIpAddress(),
                userAgent = userAgent?.sanitizeUserAgent(),
            )
    }

@Schema(name = "User")
data class UserResponse(
    val id: String,
    val tenantId: String,
    val username: String,
    val email: String,
    val fullName: String,
    val status: UserStatus,
    val roleIds: Set<String>,
    val lastLoginAt: Instant?,
    val failedLoginAttempts: Int,
    val lockedUntil: Instant?,
    val metadata: Map<String, String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Tenant.toResponse(): TenantResponse =
    TenantResponse(
        id = id.toString(),
        name = name,
        slug = slug,
        status = status,
        subscription =
            SubscriptionResponse(
                plan = subscription.plan,
                startDate = subscription.startDate,
                endDate = subscription.endDate,
                maxUsers = subscription.maxUsers,
                maxStorage = subscription.maxStorage,
                features = subscription.features,
            ),
        organization = organization?.toResponse(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        metadata = metadata,
    )

private fun Organization.toResponse(): OrganizationResponse =
    OrganizationResponse(
        legalName = legalName,
        taxId = taxId,
        industry = industry,
        address = address?.toResponse(),
        contactEmail = contactEmail,
        contactPhone = contactPhone,
    )

private fun Address.toResponse(): AddressResponse =
    AddressResponse(
        street = street,
        city = city,
        state = state,
        postalCode = postalCode,
        country = country,
    )

fun User.toResponse(): UserResponse =
    UserResponse(
        id = id.toString(),
        tenantId = tenantId.toString(),
        username = username,
        email = email,
        fullName = fullName,
        status = status,
        roleIds = roleIds.map(RoleId::toString).toSet(),
        lastLoginAt = lastLoginAt,
        failedLoginAttempts = failedLoginAttempts,
        lockedUntil = lockedUntil,
        metadata = metadata,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Role.toResponse(): RoleResponse =
    RoleResponse(
        id = id.toString(),
        tenantId = tenantId.toString(),
        name = name,
        description = description,
        permissions = permissions.map { it.toPayload() }.toSet(),
        isSystem = isSystem,
        metadata = metadata,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun Permission.toPayload(): PermissionPayload =
    PermissionPayload(
        resource = resource,
        action = action,
        scope = scope,
    )
