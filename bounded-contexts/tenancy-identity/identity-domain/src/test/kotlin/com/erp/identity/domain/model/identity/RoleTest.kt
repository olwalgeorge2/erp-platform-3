package com.erp.identity.domain.model.identity

import com.erp.identity.domain.model.tenant.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RoleTest {
    private val tenantId = TenantId.generate()
    private val manageUsers = Permission.manage("users")
    private val readAudit = Permission.read("audit")

    @Test
    fun `create validates role name and description`() {
        assertThrows(IllegalArgumentException::class.java) {
            Role.create(
                tenantId = tenantId,
                name = "",
                description = "desc",
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            Role.create(
                tenantId = tenantId,
                name = "a",
                description = "desc",
            )
        }

        val tooLongDescription = "x".repeat(501)
        assertThrows(IllegalArgumentException::class.java) {
            Role.create(
                tenantId = tenantId,
                name = "valid-name",
                description = tooLongDescription,
            )
        }
    }

    @Test
    fun `system role requires at least one permission`() {
        assertThrows(IllegalArgumentException::class.java) {
            Role.create(
                tenantId = tenantId,
                name = "system-admin",
                description = "System role",
                permissions = emptySet(),
                isSystem = true,
            )
        }
    }

    @Test
    fun `grantPermission adds permission and updates timestamp`() {
        val role = baseRole()

        val updated = role.grantPermission(manageUsers)

        assertTrue(updated.permissions.contains(manageUsers))
        assertTrue(updated.updatedAt.isAfter(role.updatedAt))
    }

    @Test
    fun `grantPermission rejects duplicates and system roles`() {
        val roleWithPermission = baseRole().grantPermission(manageUsers)

        assertThrows(IllegalArgumentException::class.java) {
            roleWithPermission.grantPermission(manageUsers)
        }

        val systemRole =
            Role.create(
                tenantId = tenantId,
                name = "system-role",
                description = "System role",
                permissions = setOf(manageUsers),
                isSystem = true,
            )
        assertThrows(IllegalArgumentException::class.java) {
            systemRole.grantPermission(readAudit)
        }
    }

    @Test
    fun `revokePermission removes permission`() {
        val role =
            baseRole()
                .grantPermission(manageUsers)
                .grantPermission(readAudit)

        val updated = role.revokePermission(manageUsers)

        assertFalse(updated.permissions.contains(manageUsers))
        assertTrue(updated.permissions.contains(readAudit))
        assertTrue(updated.updatedAt.isAfter(role.updatedAt))
    }

    @Test
    fun `revokePermission rejects missing permission and system role`() {
        val role = baseRole()
        assertThrows(IllegalArgumentException::class.java) {
            role.revokePermission(manageUsers)
        }

        val systemRole =
            Role.create(
                tenantId = tenantId,
                name = "system-role",
                description = "System role",
                permissions = setOf(manageUsers),
                isSystem = true,
            )
        assertThrows(IllegalArgumentException::class.java) {
            systemRole.revokePermission(manageUsers)
        }
    }

    @Test
    fun `updateDescription enforces length and system immutability`() {
        val role = baseRole()
        val updated = role.updateDescription("Updated description")
        assertEquals("Updated description", updated.description)
        assertTrue(updated.updatedAt.isAfter(role.updatedAt))

        val tooLong = "x".repeat(501)
        assertThrows(IllegalArgumentException::class.java) {
            role.updateDescription(tooLong)
        }

        val systemRole =
            Role.create(
                tenantId = tenantId,
                name = "system-role",
                description = "System role",
                permissions = setOf(manageUsers),
                isSystem = true,
            )
        assertThrows(IllegalArgumentException::class.java) {
            systemRole.updateDescription("no-op")
        }
    }

    @Test
    fun `hasPermission helpers inspect permissions set`() {
        val role =
            baseRole()
                .grantPermission(manageUsers)
                .grantPermission(readAudit)

        assertTrue(role.hasPermission(manageUsers))
        assertTrue(role.hasPermission("users", "manage"))
        assertTrue(role.hasAnyPermission(setOf(Permission.read("users"), readAudit)))
        assertTrue(role.hasAllPermissions(setOf(manageUsers, readAudit)))

        assertFalse(role.hasPermission(Permission.delete("users")))
        assertFalse(role.hasPermission("audit", "delete"))
        assertFalse(role.hasAnyPermission(setOf(Permission.delete("users"))))
        assertFalse(role.hasAllPermissions(setOf(manageUsers, Permission.delete("users"))))
    }

    private fun baseRole(): Role =
        Role(
            id = RoleId.generate(),
            tenantId = tenantId,
            name = "tenant-role",
            description = "Tenant scoped role",
            permissions = emptySet(),
            createdAt = Instant.now().minusSeconds(60),
            updatedAt = Instant.now().minusSeconds(60),
        )
}
