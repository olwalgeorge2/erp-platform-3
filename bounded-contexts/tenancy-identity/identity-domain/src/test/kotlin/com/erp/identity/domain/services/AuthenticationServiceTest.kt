package com.erp.identity.domain.services

import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.PasswordPolicy
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.identity.UserStatus
import com.erp.identity.domain.model.tenant.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthenticationServiceTest {
    private val passwordPolicy = PasswordPolicy()

    @Test
    fun `successful authentication resets failed attempts`() {
        val verifier = StubCredentialVerifier(shouldSucceed = true)
        val service = AuthenticationService(verifier, passwordPolicy)
        val user = activeUser().copy(failedLoginAttempts = 2)

        val result = service.authenticate(user, "secret")

        assertTrue(result is AuthenticationResult.Success)
        val authenticatedUser = (result as AuthenticationResult.Success).user
        assertEquals(0, authenticatedUser.failedLoginAttempts)
    }

    @Test
    fun `failed authentication increments attempts`() {
        val verifier = StubCredentialVerifier(shouldSucceed = false)
        val service = AuthenticationService(verifier, passwordPolicy)
        val user = activeUser()

        val result = service.authenticate(user, "wrong")

        assertTrue(result is AuthenticationResult.Failure)
        val failedUser = (result as AuthenticationResult.Failure).user
        assertEquals(1, failedUser.failedLoginAttempts)
    }

    private fun activeUser(): User =
        User(
            id = UserId.generate(),
            tenantId = TenantId.generate(),
            username = "john_doe",
            email = "john.doe@example.com",
            fullName = "John Doe",
            credential =
                Credential(
                    passwordHash = "hash",
                    salt = "salt",
                    algorithm = HashAlgorithm.ARGON2,
                    lastChangedAt = Instant.now(),
                ),
            status = UserStatus.ACTIVE,
            roleIds = emptySet(),
            metadata = emptyMap(),
            lastLoginAt = null,
            failedLoginAttempts = 0,
            lockedUntil = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private class StubCredentialVerifier(
        private val shouldSucceed: Boolean,
    ) : CredentialVerifier {
        override fun verify(
            rawPassword: String,
            storedCredential: Credential,
        ): Boolean = shouldSucceed
    }
}
