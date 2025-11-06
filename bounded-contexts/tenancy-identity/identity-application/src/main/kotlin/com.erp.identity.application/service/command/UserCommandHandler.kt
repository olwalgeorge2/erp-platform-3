package com.erp.identity.application.service.command

import com.erp.identity.application.port.input.command.AssignRoleCommand
import com.erp.identity.application.port.input.command.AuthenticateUserCommand
import com.erp.identity.application.port.input.command.CreateUserCommand
import com.erp.identity.application.port.input.command.UpdateCredentialsCommand
import com.erp.identity.application.port.output.CredentialCryptoPort
import com.erp.identity.application.port.output.EventPublisherPort
import com.erp.identity.application.port.output.RoleRepository
import com.erp.identity.application.port.output.TenantRepository
import com.erp.identity.application.port.output.UserRepository
import com.erp.identity.domain.events.RoleAssignedEvent
import com.erp.identity.domain.events.UserCreatedEvent
import com.erp.identity.domain.events.UserUpdatedEvent
import com.erp.identity.domain.exceptions.InvalidCredentialException
import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.services.AuthenticationResult
import com.erp.identity.domain.services.AuthenticationService
import com.erp.shared.types.results.Result
import com.erp.shared.types.events.DomainEvent

class UserCommandHandler(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val credentialCryptoPort: CredentialCryptoPort,
    private val authenticationService: AuthenticationService,
    private val eventPublisher: EventPublisherPort,
) {
    fun createUser(command: CreateUserCommand): Result<User> {
        val tenantId = command.tenantId
        if (tenantRepository.findById(tenantId) == null) {
            return Result.failure(
                code = "TENANT_NOT_FOUND",
                message = "Tenant not found for user creation",
                details = mapOf("tenantId" to tenantId.toString()),
            )
        }

        if (userRepository.existsByUsername(tenantId, command.username)) {
            return Result.failure(
                code = "USERNAME_IN_USE",
                message = "Username already in use",
                details = mapOf("username" to command.username),
            )
        }

        if (userRepository.existsByEmail(tenantId, command.email)) {
            return Result.failure(
                code = "EMAIL_IN_USE",
                message = "Email already in use",
                details = mapOf("email" to command.email),
            )
        }

        val roles = roleRepository.findByIds(tenantId, command.roleIds)
        val missingRoles = command.roleIds - roles.map { it.id }.toSet()
        if (missingRoles.isNotEmpty()) {
            return Result.failure(
                code = "ROLE_NOT_FOUND",
                message = "One or more roles do not exist",
                details = mapOf("roleIds" to missingRoles.joinToString()),
            )
        }

        val hashed =
            credentialCryptoPort.hashPassword(
                tenantId = tenantId,
                userId = null,
                rawPassword = command.password,
            )

        val credential =
            Credential(
                passwordHash = hashed.hash,
                salt = hashed.salt,
                algorithm = hashed.algorithm,
            )

        val user =
            User.create(
                tenantId = tenantId,
                username = command.username,
                email = command.email,
                fullName = command.fullName,
                credential = credential,
                roleIds = roles.map { it.id }.toSet(),
                metadata = command.metadata,
            )

        val savedUser = userRepository.save(user)
        publishEvents(
            UserCreatedEvent(
                tenantId = savedUser.tenantId,
                userId = savedUser.id,
                username = savedUser.username,
                email = savedUser.email,
                status = savedUser.status,
            ),
        )
        return Result.success(savedUser)
    }

    fun assignRole(command: AssignRoleCommand): Result<User> {
        val user =
            userRepository.findById(command.tenantId, command.userId)
                ?: return Result.failure(
                    code = "USER_NOT_FOUND",
                    message = "User not found for role assignment",
                    details = mapOf("userId" to command.userId.toString()),
                )

        if (user.hasRole(command.roleId)) {
            return Result.failure(
                code = "ROLE_ALREADY_ASSIGNED",
                message = "User already has this role",
                details = mapOf("roleId" to command.roleId.toString()),
            )
        }

        val role =
            roleRepository.findById(command.tenantId, command.roleId)
                ?: return Result.failure(
                    code = "ROLE_NOT_FOUND",
                    message = "Role not found for assignment",
                    details = mapOf("roleId" to command.roleId.toString()),
                )

        val updatedUser = user.assignRole(role.id)
        val savedUser = userRepository.save(updatedUser)

        publishEvents(
            RoleAssignedEvent(
                tenantId = command.tenantId,
                userId = savedUser.id,
                roleId = role.id,
            ),
            UserUpdatedEvent(
                tenantId = savedUser.tenantId,
                userId = savedUser.id,
                updatedFields = setOf("roleIds"),
                status = savedUser.status,
            ),
        )

        return Result.success(savedUser)
    }

    fun updateCredentials(command: UpdateCredentialsCommand): Result<User> {
        val user =
            userRepository.findById(command.tenantId, command.userId)
                ?: return Result.failure(
                    code = "USER_NOT_FOUND",
                    message = "User not found for credential update",
                    details = mapOf("userId" to command.userId.toString()),
                )

        try {
            command.currentPassword?.let { currentPassword ->
                authenticationService.requireValidCredentials(user, currentPassword)
            }
        } catch (ex: InvalidCredentialException) {
            return Result.failure(
                code = "INVALID_CREDENTIALS",
                message = ex.message ?: "Current credentials are invalid",
                details = mapOf("userId" to user.id.toString()),
            )
        }

        val hashed =
            credentialCryptoPort.hashPassword(
                tenantId = command.tenantId,
                userId = command.userId,
                rawPassword = command.newPassword,
            )

        val updateResult =
            authenticationService.updatePassword(
                user = user,
                newRawPassword = command.newPassword,
                hashedPassword = hashed.hash,
                salt = hashed.salt,
            )

        return updateResult
            .map { updated -> userRepository.save(updated) }
            .onSuccess { savedUser ->
                publishEvents(
                    UserUpdatedEvent(
                        tenantId = savedUser.tenantId,
                        userId = savedUser.id,
                        updatedFields = setOf("credential"),
                        status = savedUser.status,
                    ),
                )
            }
    }

    fun authenticate(command: AuthenticateUserCommand): Result<User> {
        val user =
            findUserByIdentifier(command.tenantId, command.usernameOrEmail)
                ?: return Result.failure(
                    code = "USER_NOT_FOUND",
                    message = "User not found for authentication",
                    details = mapOf("identifier" to command.usernameOrEmail),
                )

        return when (val result = authenticationService.authenticate(user, command.password)) {
            is AuthenticationResult.Success -> {
                val saved = userRepository.save(result.user)
                Result.success(saved)
            }
            is AuthenticationResult.Failure -> {
                userRepository.save(result.user)
                Result.Failure(result.reason)
            }
        }
    }

    private fun findUserByIdentifier(
        tenantId: TenantId,
        identifier: String,
    ): User? =
        userRepository.findByUsername(tenantId, identifier)
            ?: userRepository.findByEmail(tenantId, identifier)

    private fun publishEvents(vararg events: DomainEvent) {
        if (events.isNotEmpty()) {
            eventPublisher.publish(events.toList())
        }
    }
}
