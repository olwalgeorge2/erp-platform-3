package com.erp.identity.domain.model.identity

/**
 * Enum representing the lifecycle status of a user.
 */
enum class UserStatus {
    /**
     * User account is pending activation (email verification)
     */
    PENDING,

    /**
     * User account is active and can authenticate
     */
    ACTIVE,

    /**
     * User account is temporarily suspended
     */
    SUSPENDED,

    /**
     * User account is locked (too many failed login attempts)
     */
    LOCKED,

    /**
     * User account is disabled by administrator
     */
    DISABLED,

    /**
     * User account is deleted
     */
    DELETED,
}
