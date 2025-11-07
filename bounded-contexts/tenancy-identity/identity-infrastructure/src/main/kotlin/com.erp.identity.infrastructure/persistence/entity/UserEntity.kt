package com.erp.identity.infrastructure.persistence.entity

import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.identity.UserStatus
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_users_username", columnNames = ["tenant_id", "username"]),
        UniqueConstraint(name = "uk_identity_users_email", columnNames = ["tenant_id", "email"]),
    ],
    indexes = [
        Index(name = "idx_identity_users_tenant", columnList = "tenant_id"),
    ],
)
class UserEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID = UUID.randomUUID(),
    @Column(name = "username", nullable = false, length = 50)
    var username: String = "",
    @Column(name = "email", nullable = false, length = 200)
    var email: String = "",
    @Column(name = "full_name", nullable = false, length = 200)
    var fullName: String = "",
    @Column(name = "password_hash", nullable = false, length = 512)
    var passwordHash: String = "",
    @Column(name = "password_salt", nullable = false, length = 512)
    var passwordSalt: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "hash_algorithm", nullable = false, length = 16)
    var hashAlgorithm: HashAlgorithm = HashAlgorithm.PBKDF2,
    @Column(name = "password_last_changed_at", nullable = false)
    var passwordLastChangedAt: Instant = Instant.now(),
    @Column(name = "password_expires_at")
    var passwordExpiresAt: Instant? = null,
    @Column(name = "must_change_on_next_login", nullable = false)
    var mustChangeOnNextLogin: Boolean = false,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: UserStatus = UserStatus.PENDING,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "identity_user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
    )
    @Column(name = "role_id", nullable = false)
    var roleIds: MutableSet<UUID> = mutableSetOf(),
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "identity_user_metadata",
        joinColumns = [JoinColumn(name = "user_id")],
    )
    @MapKeyColumn(name = "metadata_key", length = 100)
    @Column(name = "metadata_value", length = 500)
    var metadata: MutableMap<String, String> = mutableMapOf(),
    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,
    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,
    @Column(name = "locked_until")
    var lockedUntil: Instant? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): User =
        User(
            id = UserId(id),
            tenantId = TenantId(tenantId),
            username = username,
            email = email,
            fullName = fullName,
            credential =
                Credential(
                    passwordHash = passwordHash,
                    salt = passwordSalt,
                    algorithm = hashAlgorithm,
                    lastChangedAt = passwordLastChangedAt,
                    expiresAt = passwordExpiresAt,
                    mustChangeOnNextLogin = mustChangeOnNextLogin,
                ),
            status = status,
            roleIds = roleIds.map { RoleId(it) }.toSet(),
            metadata = metadata.toMap(),
            lastLoginAt = lastLoginAt,
            failedLoginAttempts = failedLoginAttempts,
            lockedUntil = lockedUntil,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: User) {
        tenantId = domain.tenantId.value
        username = domain.username
        email = domain.email
        fullName = domain.fullName
        passwordHash = domain.credential.passwordHash
        passwordSalt = domain.credential.salt
        hashAlgorithm = domain.credential.algorithm
        passwordLastChangedAt = domain.credential.lastChangedAt
        passwordExpiresAt = domain.credential.expiresAt
        mustChangeOnNextLogin = domain.credential.mustChangeOnNextLogin
        status = domain.status
        roleIds = domain.roleIds.map { it.value }.toMutableSet()
        metadata = domain.metadata.toMutableMap()
        lastLoginAt = domain.lastLoginAt
        failedLoginAttempts = domain.failedLoginAttempts
        lockedUntil = domain.lockedUntil
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }

    companion object {
        fun from(domain: User): UserEntity =
            UserEntity(
                id = domain.id.value,
                tenantId = domain.tenantId.value,
                username = domain.username,
                email = domain.email,
                fullName = domain.fullName,
                passwordHash = domain.credential.passwordHash,
                passwordSalt = domain.credential.salt,
                hashAlgorithm = domain.credential.algorithm,
                passwordLastChangedAt = domain.credential.lastChangedAt,
                passwordExpiresAt = domain.credential.expiresAt,
                mustChangeOnNextLogin = domain.credential.mustChangeOnNextLogin,
                status = domain.status,
                roleIds = domain.roleIds.map { it.value }.toMutableSet(),
                metadata = domain.metadata.toMutableMap(),
                lastLoginAt = domain.lastLoginAt,
                failedLoginAttempts = domain.failedLoginAttempts,
                lockedUntil = domain.lockedUntil,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}
