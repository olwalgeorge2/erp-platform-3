package com.erp.identity.application.service.command

import com.erp.identity.application.port.input.command.CreateUserCommand
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateUserCommandValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `valid command passes bean validation`() {
        val command =
            CreateUserCommand(
                tenantId = TenantId(UUID.randomUUID()),
                username = "integration_user",
                email = "integration.user@example.com",
                fullName = "Integration User",
                password = "Password123!",
                roleIds = setOf(RoleId(UUID.randomUUID())),
                metadata = mapOf("source" to "test"),
                createdBy = "integration-test",
            )

        val violations = validator.validate(command)

        assertTrue(violations.isEmpty()) { "Expected no validation violations but found: $violations" }
    }
}
