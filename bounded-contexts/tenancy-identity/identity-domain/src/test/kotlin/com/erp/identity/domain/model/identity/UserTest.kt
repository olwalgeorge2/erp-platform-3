package com.erp.identity.domain.model.identity

import com.erp.identity.domain.model.tenant.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class UserTest {
    private val tenantId = TenantId.generate()

    @Test
    fun `assignRole adds role and reject duplicates`() {
        val baseUser = activeUser(credential())
        val roleId = RoleId.generate()

        val updated = baseUser.assignRole(roleId)
        assertTrue(updated.roleIds.contains(roleId))

        assertThrows(IllegalArgumentException::class.java) {
            updated.assignRole(roleId)
        }
    }

    @Test
    fun `revokeRole removes existing role`() {
        val roleId = RoleId.generate()
        val baseUser = activeUser(credential()).assignRole(roleId)

        val updated = baseUser.revokeRole(roleId)

        assertTrue(!updated.roleIds.contains(roleId))
        assertThrows(IllegalArgumentException::class.java) {
            updated.revokeRole(roleId)
        }
    }

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

    @Test
    fun `disable transitions active user to disabled`() {
        val user = activeUser(credential())

        val disabled = user.disable()

        assertEquals(UserStatus.DISABLED, disabled.status)
    }

    @Test
    fun `delete moves user to deleted state`() {
        val user = activeUser(credential())

        val deleted = user.delete()

        assertEquals(UserStatus.DELETED, deleted.status)
    }

    @Test
    fun `create user sets must change password flag`() {
        val newUser =
            User.create(
                tenantId = tenantId,
                username = "new_user",
                email = "new.user@example.com",
                fullName = "New User",
                credential = credential(),
            )

        assertTrue(newUser.credential.mustChangeOnNextLogin)
        assertEquals(UserStatus.PENDING, newUser.status)
    }

    @Test
    fun `activate transitions pending user to active`() {
        val pending =
            User.create(
                tenantId = tenantId,
                username = "pending_user",
                email = "pending.user@example.com",
                fullName = "Pending User",
                credential = credential(),
            )

        val activated = pending.activate()

        assertEquals(UserStatus.ACTIVE, activated.status)
    }

    @Test
    fun `suspend adds suspension reason metadata`() {
        val active =
            User
                .create(
                    tenantId,
                    "active_user",
                    "active.user@example.com",
                    "Active User",
                    credential(),
                ).activate()

        val suspended = active.suspend("policy review")

        assertEquals(UserStatus.SUSPENDED, suspended.status)
        assertEquals("policy review", suspended.metadata["suspensionReason"])
    }

    @Test
    fun `reactivate clears suspension reason and resets counters`() {
        val suspended =
            User
                .create(tenantId, "suspended", "suspended@example.com", "Suspended User", credential())
                .activate()
                .suspend("audit")
                .copy(
                    failedLoginAttempts = 3,
                    lockedUntil = Instant.now().plusSeconds(120),
                )

        val reactivated = suspended.reactivate()

        assertEquals(UserStatus.ACTIVE, reactivated.status)
        assertEquals(0, reactivated.failedLoginAttempts)
        assertEquals(null, reactivated.lockedUntil)
        assertFalse(reactivated.metadata.containsKey("suspensionReason"))
    }

    @Test
    fun `canLogin honours credential requirements and lock state`() {
        val readyUser = activeUser(credential())
        assertTrue(readyUser.canLogin())

        val mustChange =
            readyUser.copy(
                credential = readyUser.credential.requireChangeOnNextLogin(),
            )
        assertFalse(mustChange.canLogin())

        val locked =
            readyUser.copy(
                lockedUntil = Instant.now().plusSeconds(60),
            )
        assertFalse(locked.canLogin())
    }

    @Test
    fun `isLocked reflects future lock timestamp`() {
        val user =
            activeUser(credential()).copy(
                lockedUntil = Instant.now().plusSeconds(60),
            )
        assertTrue(user.isLocked())

        val unlocked =
            user.copy(
                lockedUntil = Instant.now().minusSeconds(10),
            )
        assertFalse(unlocked.isLocked())
    }

    @Test
    fun `hasRole returns membership state`() {
        val roleId = RoleId.generate()
        val user = activeUser(credential()).assignRole(roleId)

        assertTrue(user.hasRole(roleId))
        assertFalse(user.hasRole(RoleId.generate()))
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
