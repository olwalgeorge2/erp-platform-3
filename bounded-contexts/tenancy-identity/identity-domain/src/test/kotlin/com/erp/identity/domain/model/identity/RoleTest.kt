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
    private val userRead = Permission.read("user")
    private val userManage = Permission.manage("user")

    @Test
    fun `grantPermission adds permission for tenant roles`() {
        val role = Role.create(tenantId, "Viewer", "Read only")

        val updated = role.grantPermission(userRead)

        assertTrue(updated.permissions.contains(userRead))
        assertTrue(!updated.updatedAt.isBefore(role.updatedAt))
    }

    @Test
    fun `grantPermission rejects system roles`() {
        val systemRole =
            Role(
                id = RoleId.generate(),
                tenantId = tenantId,
                name = "System Admin",
                description = "Immutable role",
                permissions = setOf(userRead),
                isSystem = true,
                metadata = emptyMap(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        assertThrows(IllegalArgumentException::class.java) {
            systemRole.grantPermission(userManage)
        }
    }

    @Test
    fun `revokePermission removes permission when present`() {
        val role = Role.create(tenantId, "Editor", "Edit users", setOf(userManage))

        val updated = role.revokePermission(userManage)

        assertFalse(updated.permissions.contains(userManage))
    }

    @Test
    fun `revokePermission rejects missing permissions`() {
        val role = Role.create(tenantId, "Viewer", "Read only", setOf(userRead))

        assertThrows(IllegalArgumentException::class.java) {
            role.revokePermission(userManage)
        }
    }

    @Test
    fun `updateDescription updates text for tenant roles`() {
        val role = Role.create(tenantId, "Viewer", "Read only")

        val updated = role.updateDescription("Broader read access")

        assertEquals("Broader read access", updated.description)
        assertTrue(!updated.updatedAt.isBefore(role.updatedAt))
    }

    @Test
    fun `updateDescription rejects system roles`() {
        val systemRole =
            Role(
                id = RoleId.generate(),
                tenantId = tenantId,
                name = "System Admin",
                description = "Immutable role",
                permissions = setOf(userRead),
                isSystem = true,
                metadata = emptyMap(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        assertThrows(IllegalArgumentException::class.java) {
            systemRole.updateDescription("New description")
        }
    }
}
