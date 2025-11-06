package com.erp.identity.domain.services

import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.Permission
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.identity.UserStatus
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.results.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthorizationServiceTest {
    private val tenantId = TenantId.generate()
    private val credential =
        Credential(
            passwordHash = "hash",
            salt = "salt",
            algorithm = HashAlgorithm.ARGON2,
            lastChangedAt = Instant.now(),
        )

    private val service = AuthorizationService()

    @Test
    fun `resolvePermissions aggregates unique permissions from assigned roles`() {
        val readUser = Permission.read("user")
        val manageTenant = Permission.manage("tenant")

        val adminRole =
            Role.create(
                tenantId = tenantId,
                name = "Admin",
                description = "Administrative role",
                permissions = setOf(readUser, manageTenant),
            )
        val auditorRole =
            Role.create(
                tenantId = tenantId,
                name = "Auditor",
                description = "Auditing role",
                permissions = setOf(readUser),
            )
        val user =
            sampleUser(
                roleIds = setOf(adminRole.id, auditorRole.id),
            )

        val permissions = service.resolvePermissions(user, listOf(adminRole, auditorRole))

        assertEquals(setOf(readUser, manageTenant), permissions)
    }

    @Test
    fun `hasPermission returns false when role definitions are missing`() {
        val readUser = Permission.read("user")
        val adminRole =
            Role.create(
                tenantId = tenantId,
                name = "Admin",
                description = "Administrative role",
                permissions = setOf(readUser),
            )
        val user =
            sampleUser(
                roleIds = setOf(adminRole.id),
            )

        val hasPermission = service.hasPermission(user, readUser, emptyList())

        assertFalse(hasPermission)
    }

    @Test
    fun `ensurePermissions returns failure when user lacks requirements`() {
        val readUser = Permission.read("user")
        val updateUser = Permission.update("user")
        val viewerRole =
            Role.create(
                tenantId = tenantId,
                name = "Viewer",
                description = "Read-only role",
                permissions = setOf(readUser),
            )
        val user =
            sampleUser(
                roleIds = setOf(viewerRole.id),
            )

        val result = service.ensurePermissions(user, setOf(updateUser), listOf(viewerRole))

        assertTrue(result is Result.Failure)
        val failure = result as Result.Failure
        assertEquals("INSUFFICIENT_PERMISSIONS", failure.error.code)
        assertTrue(failure.error.details["missing"]!!.contains("user:update"))
    }

    private fun sampleUser(roleIds: Set<RoleId>): User =
        User(
            id = UserId.generate(),
            tenantId = tenantId,
            username = "jane-doe",
            email = "jane.doe@example.com",
            fullName = "Jane Doe",
            credential = credential,
            status = UserStatus.ACTIVE,
            roleIds = roleIds,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
}
