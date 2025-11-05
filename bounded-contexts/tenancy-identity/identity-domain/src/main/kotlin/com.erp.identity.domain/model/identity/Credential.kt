package com.erp.identity.domain.model.identity

import java.time.Instant

/**
 * Value object representing user credentials
 */
data class Credential(
    val passwordHash: String,
    val salt: String,
    val algorithm: HashAlgorithm = HashAlgorithm.BCRYPT,
    val lastChangedAt: Instant = Instant.now(),
    val expiresAt: Instant? = null,
    val mustChangeOnNextLogin: Boolean = false,
) {
    init {
        require(passwordHash.isNotBlank()) { "Password hash cannot be blank" }
        require(salt.isNotBlank()) { "Salt cannot be blank" }
    }

    fun isExpired(): Boolean = expiresAt?.isBefore(Instant.now()) ?: false

    fun requiresChange(): Boolean = mustChangeOnNextLogin || isExpired()

    fun withNewPassword(
        newPasswordHash: String,
        newSalt: String,
    ): Credential =
        copy(
            passwordHash = newPasswordHash,
            salt = newSalt,
            lastChangedAt = Instant.now(),
            mustChangeOnNextLogin = false,
        )

    fun withExpiration(expiresAt: Instant): Credential =
        copy(
            expiresAt = expiresAt,
        )

    fun requireChangeOnNextLogin(): Credential =
        copy(
            mustChangeOnNextLogin = true,
        )
}

enum class HashAlgorithm {
    BCRYPT,
    ARGON2,
    SCRYPT,
    PBKDF2,
}
