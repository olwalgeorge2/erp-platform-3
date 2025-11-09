package com.erp.identity.domain.model.identity

import com.erp.identity.domain.model.tenant.TenantId
import java.time.Instant

/**
 * User aggregate root - represents an authenticated user within a tenant
 */
data class User(
    val id: UserId,
    val tenantId: TenantId,
    val username: String,
    val email: String,
    val fullName: String,
    val credential: Credential,
    val status: UserStatus,
    val roleIds: Set<RoleId> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val lastLoginAt: Instant? = null,
    val failedLoginAttempts: Int = 0,
    val lockedUntil: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(
            username.matches(USERNAME_REGEX),
        ) { "Username must be alphanumeric with underscores/hyphens (3-50 chars)" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(email.matches(EMAIL_REGEX)) { "Email must be valid" }
        require(fullName.isNotBlank()) { "Full name cannot be blank" }
        require(fullName.length in 2..200) { "Full name must be between 2 and 200 characters" }
        require(failedLoginAttempts >= 0) { "Failed login attempts cannot be negative" }
    }

    companion object {
        private val USERNAME_REGEX = "^[a-zA-Z0-9_-]{3,50}$".toRegex()
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION_MINUTES = 30L

        fun create(
            tenantId: TenantId,
            username: String,
            email: String,
            fullName: String,
            credential: Credential,
            roleIds: Set<RoleId> = emptySet(),
            metadata: Map<String, String> = emptyMap(),
        ): User =
            User(
                id = UserId.generate(),
                tenantId = tenantId,
                username = username,
                email = email,
                fullName = fullName,
                credential = credential.copy(mustChangeOnNextLogin = true),
                status = UserStatus.PENDING,
                roleIds = roleIds,
                metadata = metadata,
            )
    }

    // Lifecycle methods
    fun activate(): User {
        require(status == UserStatus.PENDING) { "Can only activate pending users" }
        return copy(
            status = UserStatus.ACTIVE,
            updatedAt = Instant.now(),
        )
    }

    fun suspend(reason: String): User {
        require(status == UserStatus.ACTIVE) { "Can only suspend active users" }
        return copy(
            status = UserStatus.SUSPENDED,
            metadata = metadata + ("suspensionReason" to reason),
            updatedAt = Instant.now(),
        )
    }

    fun reactivate(): User {
        require(status == UserStatus.SUSPENDED) { "Can only reactivate suspended users" }
        return copy(
            status = UserStatus.ACTIVE,
            metadata = metadata - "suspensionReason",
            failedLoginAttempts = 0,
            lockedUntil = null,
            updatedAt = Instant.now(),
        )
    }

    fun disable(): User {
        require(
            status in setOf(UserStatus.ACTIVE, UserStatus.SUSPENDED),
        ) { "Can only disable active or suspended users" }
        return copy(
            status = UserStatus.DISABLED,
            updatedAt = Instant.now(),
        )
    }

    fun delete(): User {
        require(status != UserStatus.DELETED) { "User is already deleted" }
        return copy(
            status = UserStatus.DELETED,
            updatedAt = Instant.now(),
        )
    }

    // Authentication methods
    fun recordSuccessfulLogin(): User {
        require(status == UserStatus.ACTIVE) { "Only active users can login" }
        require(!isLocked()) { "User account is locked" }
        return copy(
            lastLoginAt = Instant.now(),
            failedLoginAttempts = 0,
            lockedUntil = null,
            updatedAt = Instant.now(),
        )
    }

    fun recordFailedLogin(): User {
        val newAttempts = failedLoginAttempts + 1
        val shouldLock = newAttempts >= MAX_FAILED_ATTEMPTS

        return copy(
            failedLoginAttempts = newAttempts,
            lockedUntil = if (shouldLock) Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60) else lockedUntil,
            status = if (shouldLock) UserStatus.LOCKED else status,
            updatedAt = Instant.now(),
        )
    }

    fun changePassword(
        newPasswordHash: String,
        newSalt: String,
    ): User {
        require(status == UserStatus.ACTIVE) { "Can only change password for active users" }
        return copy(
            credential = credential.withNewPassword(newPasswordHash, newSalt),
            updatedAt = Instant.now(),
        )
    }

    fun resetPassword(
        newPasswordHash: String,
        newSalt: String,
    ): User =
        copy(
            credential = credential.withNewPassword(newPasswordHash, newSalt).requireChangeOnNextLogin(),
            failedLoginAttempts = 0,
            lockedUntil = null,
            status = if (status == UserStatus.LOCKED) UserStatus.ACTIVE else status,
            updatedAt = Instant.now(),
        )

    // Role management
    fun assignRole(roleId: RoleId): User {
        require(!roleIds.contains(roleId)) { "User already has this role" }
        return copy(
            roleIds = roleIds + roleId,
            updatedAt = Instant.now(),
        )
    }

    fun revokeRole(roleId: RoleId): User {
        require(roleIds.contains(roleId)) { "User does not have this role" }
        return copy(
            roleIds = roleIds - roleId,
            updatedAt = Instant.now(),
        )
    }

    // Query methods
    fun isLocked(): Boolean = lockedUntil?.isAfter(Instant.now()) ?: false

    fun canLogin(): Boolean = status == UserStatus.ACTIVE && !isLocked() && !credential.requiresChange()

    fun requiresPasswordChange(): Boolean = credential.requiresChange()

    fun hasRole(roleId: RoleId): Boolean = roleIds.contains(roleId)

    fun clearPasswordChangeRequirement(): User =
        copy(
            credential = credential.copy(mustChangeOnNextLogin = false),
            updatedAt = Instant.now(),
        )
}
