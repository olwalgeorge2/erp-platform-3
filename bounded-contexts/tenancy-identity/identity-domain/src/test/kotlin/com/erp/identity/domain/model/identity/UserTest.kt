package com.erp.identity.domain.model.identity

import com.erp.identity.domain.model.tenant.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class UserTest {
    private val tenantId = TenantId.generate()

    @Test
    fun `recordSuccessfulLogin resets counters and updates timestamps`() {
        val credential = credential()
        val user =
            activeUser(credential).copy(
                failedLoginAttempts = 3,
                lastLoginAt = null,
            )

        val result = user.recordSuccessfulLogin()

        assertEquals(0, result.failedLoginAttempts)
        assertEquals(UserStatus.ACTIVE, result.status)
        assertEquals(null, result.lockedUntil)
        assertNotNull(result.lastLoginAt)
        assertTrue(!result.updatedAt.isBefore(user.updatedAt))
    }

    @Test
    fun `recordSuccessfulLogin throws when user locked`() {
        val locked =
            activeUser(credential())
                .copy(
                    lockedUntil = Instant.now().plusSeconds(60),
                )

        assertThrows(IllegalArgumentException::class.java) {
            locked.recordSuccessfulLogin()
        }
    }

    @Test
    fun `recordFailedLogin locks user after threshold`() {
        var user = activeUser(credential())

        repeat(5) {
            user = user.recordFailedLogin()
        }

        assertEquals(5, user.failedLoginAttempts)
        assertEquals(UserStatus.LOCKED, user.status)
        assertNotNull(user.lockedUntil)
    }

    @Test
    fun `resetPassword clears lock state and requires password change`() {
        var user = activeUser(credential())
        repeat(5) { user = user.recordFailedLogin() }
        val lockedUser = user

        val reset =
            lockedUser.resetPassword(
                newPasswordHash = "new-hash",
                newSalt = "new-salt",
            )

        assertEquals(0, reset.failedLoginAttempts)
        assertEquals(UserStatus.ACTIVE, reset.status)
        assertEquals(null, reset.lockedUntil)
        assertTrue(reset.credential.requiresChange())
        assertTrue(!reset.updatedAt.isBefore(lockedUser.updatedAt))
    }

    @Test
    fun `changePassword requires active user`() {
        val pending =
            User.create(
                tenantId = tenantId,
                username = "jane_doe",
                email = "jane.doe@example.com",
                fullName = "Jane Doe",
                credential = credential(),
            )

        assertThrows(IllegalArgumentException::class.java) {
            pending.changePassword("hash", "salt")
        }
    }

    private fun activeUser(credential: Credential): User =
        User(
            id = UserId.generate(),
            tenantId = tenantId,
            username = "john_doe",
            email = "john.doe@example.com",
            fullName = "John Doe",
            credential = credential,
            status = UserStatus.ACTIVE,
            roleIds = emptySet(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private fun credential(): Credential =
        Credential(
            passwordHash = "hash",
            salt = "salt",
            algorithm = HashAlgorithm.ARGON2,
            lastChangedAt = Instant.now(),
        )
}
