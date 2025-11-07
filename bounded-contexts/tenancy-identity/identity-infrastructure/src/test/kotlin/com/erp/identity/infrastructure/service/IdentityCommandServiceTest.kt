package com.erp.identity.infrastructure.service

import com.erp.identity.application.port.input.command.ActivateTenantCommand
import com.erp.identity.application.port.input.command.ActivateUserCommand
import com.erp.identity.application.port.input.command.AssignRoleCommand
import com.erp.identity.application.port.input.command.CreateRoleCommand
import com.erp.identity.application.port.input.command.CreateUserCommand
import com.erp.identity.application.port.input.command.DeleteRoleCommand
import com.erp.identity.application.port.input.command.ProvisionTenantCommand
import com.erp.identity.application.port.input.command.ResumeTenantCommand
import com.erp.identity.application.port.input.command.SuspendTenantCommand
import com.erp.identity.application.port.input.command.UpdateRoleCommand
import com.erp.identity.application.service.command.RoleCommandHandler
import com.erp.identity.application.service.command.TenantCommandHandler
import com.erp.identity.application.service.command.UserCommandHandler
import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.Permission
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.identity.UserStatus
import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.results.Result
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import java.time.Instant

class IdentityCommandServiceTest {
    private val userHandler: UserCommandHandler = mock()
    private val tenantHandler: TenantCommandHandler = mock()
    private val roleHandler: RoleCommandHandler = mock()
    private val service = IdentityCommandService(userHandler, tenantHandler, roleHandler)

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `createUser delegates to handler and sets tenant MDC`() {
        val command = sampleCreateUserCommand()
        val user = sampleUser(command.tenantId)
        whenever(userHandler.createUser(eq(command))).thenReturn(Result.success(user))

        val result = service.createUser(command)

        assertTrue(result is Result.Success<User>)
        verify(userHandler).createUser(eq(command))
        val traceId = MDC.get("traceId")
        if (traceId == null) {
            val adapterClass = MDC.getMDCAdapter()::class.java.name
            assertTrue(
                adapterClass.contains("NOP"),
                "Expected MDC traceId to be populated, adapter was $adapterClass",
            )
        } else {
            assertTrue(traceId.isNotBlank())
        }
    }

    @Test
    fun `createUser propagates failure without mutating success context`() {
        val command = sampleCreateUserCommand()
        val failure: Result<User> =
            Result.failure(
                code = "TENANT_NOT_FOUND",
                message = "Missing tenant",
            )
        whenever(userHandler.createUser(eq(command))).thenReturn(failure)

        val result = service.createUser(command)

        assertTrue(result is Result.Failure)
        verify(userHandler).createUser(eq(command))
    }

    @Test
    fun `assignRole passes through handler response`() {
        val command =
            AssignRoleCommand(
                tenantId = TenantId.generate(),
                userId = UserId.generate(),
                roleId = RoleId.generate(),
                assignedBy = "audit-user",
            )
        val user = sampleUser(command.tenantId)
        whenever(userHandler.assignRole(eq(command))).thenReturn(Result.success(user))

        val result = service.assignRole(command)

        assertTrue(result is Result.Success<User>)
        verify(userHandler).assignRole(eq(command))
    }

    @Test
    fun `activateUser delegates to handler`() {
        val command =
            ActivateUserCommand(
                tenantId = TenantId.generate(),
                userId = UserId.generate(),
                requestedBy = "admin",
            )
        val user = sampleUser(command.tenantId)
        whenever(userHandler.activateUser(eq(command))).thenReturn(Result.success(user))

        val result = service.activateUser(command)

        assertTrue(result is Result.Success<User>)
        verify(userHandler).activateUser(eq(command))
    }

    @Test
    fun `provisionTenant delegates to tenant handler`() {
        val subscription =
            Subscription(
                plan = SubscriptionPlan.STARTER,
                startDate = Instant.now(),
                endDate = null,
                maxUsers = 25,
                maxStorage = 10_000,
                features = setOf("rbac"),
            )
        val command =
            ProvisionTenantCommand(
                name = "Acme Corp",
                slug = "acme-corp",
                subscription = subscription,
                organization =
                    Organization(
                        legalName = "Acme Corp",
                        taxId = null,
                        industry = "Software",
                        address = null,
                        contactEmail = "ops@acme.test",
                        contactPhone = null,
                    ),
                metadata = mapOf("region" to "us-east-1"),
                requestedBy = null,
            )
        val tenant =
            Tenant.provision(
                name = "Acme Corp",
                slug = "acme-corp",
                subscription = subscription,
                organization = null,
            )
        whenever(tenantHandler.provisionTenant(eq(command))).thenReturn(Result.success(tenant))

        val result = service.provisionTenant(command)

        assertTrue(result is Result.Success<Tenant>)
        verify(tenantHandler).provisionTenant(eq(command))
    }

    @Test
    fun `activateTenant delegates to tenant handler`() {
        val command =
            ActivateTenantCommand(
                tenantId = TenantId.generate(),
                requestedBy = null,
            )
        val tenant = sampleTenant()
        whenever(tenantHandler.activateTenant(eq(command))).thenReturn(Result.success(tenant))

        val result = service.activateTenant(command)

        assertTrue(result is Result.Success<Tenant>)
        verify(tenantHandler).activateTenant(eq(command))
    }

    @Test
    fun `suspendTenant delegates to tenant handler`() {
        val command =
            SuspendTenantCommand(
                tenantId = TenantId.generate(),
                reason = "Billing",
                requestedBy = null,
            )
        val tenant = sampleTenant()
        whenever(tenantHandler.suspendTenant(eq(command))).thenReturn(Result.success(tenant))

        val result = service.suspendTenant(command)

        assertTrue(result is Result.Success<Tenant>)
        verify(tenantHandler).suspendTenant(eq(command))
    }

    @Test
    fun `resumeTenant delegates to tenant handler`() {
        val command =
            ResumeTenantCommand(
                tenantId = TenantId.generate(),
                requestedBy = null,
            )
        val tenant = sampleTenant()
        whenever(tenantHandler.resumeTenant(eq(command))).thenReturn(Result.success(tenant))

        val result = service.resumeTenant(command)

        assertTrue(result is Result.Success<Tenant>)
        verify(tenantHandler).resumeTenant(eq(command))
    }

    @Test
    fun `createRole delegates to handler`() {
        val command =
            CreateRoleCommand(
                tenantId = TenantId.generate(),
                name = "admin",
                description = "Admin role",
                permissions = setOf(Permission.create("user")),
            )
        val role = sampleRole(command.tenantId)
        whenever(roleHandler.createRole(eq(command))).thenReturn(Result.success(role))

        val result = service.createRole(command)

        assertTrue(result is Result.Success<Role>)
        verify(roleHandler).createRole(eq(command))
    }

    @Test
    fun `updateRole delegates to handler`() {
        val command =
            UpdateRoleCommand(
                tenantId = TenantId.generate(),
                roleId = RoleId.generate(),
                name = "admin",
                description = "updated",
                permissions = emptySet(),
            )
        val role = sampleRole(command.tenantId)
        whenever(roleHandler.updateRole(eq(command))).thenReturn(Result.success(role))

        val result = service.updateRole(command)

        assertTrue(result is Result.Success<Role>)
        verify(roleHandler).updateRole(eq(command))
    }

    @Test
    fun `deleteRole delegates to handler`() {
        val command =
            DeleteRoleCommand(
                tenantId = TenantId.generate(),
                roleId = RoleId.generate(),
            )
        whenever(roleHandler.deleteRole(eq(command))).thenReturn(Result.success(Unit))

        val result = service.deleteRole(command)

        assertTrue(result is Result.Success<Unit>)
        verify(roleHandler).deleteRole(eq(command))
    }

    private fun sampleCreateUserCommand(): CreateUserCommand =
        CreateUserCommand(
            tenantId = TenantId.generate(),
            username = "service-user",
            email = "service.user@example.com",
            fullName = "Service User",
            password = "Password123!",
            roleIds = emptySet(),
            metadata = mapOf("source" to "unit-test"),
            createdBy = "tester",
        )

    private fun sampleUser(tenantId: TenantId): User =
        User(
            id = UserId.generate(),
            tenantId = tenantId,
            username = "service-user",
            email = "service.user@example.com",
            fullName = "Service User",
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
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private fun sampleTenant(): Tenant {
        val subscription =
            Subscription(
                plan = SubscriptionPlan.STARTER,
                startDate = Instant.now(),
                endDate = null,
                maxUsers = 25,
                maxStorage = 10_000,
                features = setOf("rbac"),
            )
        return Tenant.provision(
            name = "Acme Corp",
            slug = "acme-corp",
            subscription = subscription,
            organization = null,
        )
    }

    private fun sampleRole(tenantId: TenantId): Role =
        Role.create(
            tenantId = tenantId,
            name = "admin",
            description = "Administrator",
            permissions = setOf(Permission.create("user")),
        )
}
