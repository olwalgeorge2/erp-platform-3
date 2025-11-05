package com.erp.identity.domain.exceptions

/**
 * Thrown when authentication fails due to invalid credentials.
 */
class InvalidCredentialException(
    message: String = "Invalid credentials provided",
) : RuntimeException(message)
