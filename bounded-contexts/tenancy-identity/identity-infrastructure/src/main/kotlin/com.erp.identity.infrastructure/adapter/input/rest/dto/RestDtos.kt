package com.erp.identity.infrastructure.adapter.input.rest.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.erp.identity.application.port.input.command.ActivateUserCommand
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
import java.time.Instant
import java.util.UUID

data class ProvisionTenantRequest
    @JsonCreator
    constructor(
        @JsonProperty("name")
        val name: String,
        @JsonProperty("slug")
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
        fun toCommand() =
            com.erp.identity.application.port.input.command.ProvisionTenantCommand(
                name = name,
                slug = slug,
                subscription = subscription.toDomain(),
                organization = organization?.toDomain(),
                metadata = metadata ?: emptyMap(),
                requestedBy = requestedBy,
            )
    }

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
        fun toDomain(): Organization =
            Organization(
                legalName = legalName,
                taxId = taxId,
                industry = industry,
                address = address?.toDomain(),
                contactEmail = contactEmail,
                contactPhone = contactPhone,
            )
    }

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
        fun toDomain(): Address =
            Address(
                street = street,
                city = city,
                state = state,
                postalCode = postalCode,
                country = country,
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

data class CreateUserRequest
    @JsonCreator
    constructor(
        @JsonProperty("tenantId")
        val tenantId: UUID,
        @JsonProperty("username")
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
                username = username,
                email = email,
                fullName = fullName,
                password = password,
                roleIds = roleIds.map { RoleId(it) }.toSet(),
                metadata = metadata,
                createdBy = createdBy,
            )
    }

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
        fun toCommand(userId: UUID) =
            com.erp.identity.application.port.input.command.AssignRoleCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                roleId = RoleId(roleId),
                assignedBy = assignedBy,
            )
    }

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
        fun toCommand(userId: UUID) =
            ActivateUserCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                requestedBy = requestedBy,
                requirePasswordReset = requirePasswordReset ?: true,
            )
    }

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
        fun toCommand(userId: UUID) =
            com.erp.identity.application.port.input.command.UpdateCredentialsCommand(
                tenantId = TenantId(tenantId),
                userId = UserId(userId),
                currentPassword = currentPassword,
                newPassword = newPassword,
                requestedBy = requestedBy,
            )
    }

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
        fun toCommand() =
            com.erp.identity.application.port.input.command.AuthenticateUserCommand(
                tenantId = TenantId(tenantId),
                usernameOrEmail = usernameOrEmail,
                password = password,
                ipAddress = ipAddress,
                userAgent = userAgent,
            )
    }

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
