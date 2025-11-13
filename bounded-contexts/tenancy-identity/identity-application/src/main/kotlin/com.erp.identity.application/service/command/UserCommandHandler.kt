package com.erp.identity.application.service.command

import com.erp.identity.application.port.input.command.ActivateUserCommand
import com.erp.identity.application.port.input.command.AssignRoleCommand
import com.erp.identity.application.port.input.command.AuthenticateUserCommand
import com.erp.identity.application.port.input.command.CreateUserCommand
import com.erp.identity.application.port.input.command.ReactivateUserCommand
import com.erp.identity.application.port.input.command.ResetPasswordCommand
import com.erp.identity.application.port.input.command.SuspendUserCommand
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
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.PasswordPolicy
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.services.AuthenticationResult
import com.erp.identity.domain.services.AuthenticationService
import com.erp.shared.types.events.DomainEvent
import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.failure
import com.erp.shared.types.results.Result.Companion.success

class UserCommandHandler(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val credentialCryptoPort: CredentialCryptoPort,
    private val authenticationService: AuthenticationService,
    private val eventPublisher: EventPublisherPort,
    private val passwordPolicy: PasswordPolicy,
) {
    fun createUser(command: CreateUserCommand): Result<User> {
        val tenantCheck = ensureTenantExists(command.tenantId)
        if (tenantCheck is Result.Failure) {
            return tenantCheck
        }

        ensureUsernameAvailable(command.tenantId, command.username).let { result ->
            if (result is Result.Failure) return result
        }

        ensureEmailAvailable(command.tenantId, command.email).let { result ->
            if (result is Result.Failure) return result
        }

        validatePassword(command.password).let { result ->
            if (result is Result.Failure) return result
        }

        val rolesResult = roleRepository.findByIds(command.tenantId, command.roleIds)
        val roles =
            when (rolesResult) {
                is Result.Success -> {
                    val resolvedRoles = rolesResult.value
                    val missingRoles = command.roleIds - resolvedRoles.map { it.id }.toSet()
                    if (missingRoles.isNotEmpty()) {
                        return failure(
                            code = "ROLE_NOT_FOUND",
                            message = "One or more roles do not exist",
                            details = mapOf("roleIds" to missingRoles.joinToString()),
                        )
                    }
                    resolvedRoles
                }
                is Result.Failure -> return rolesResult
            }

        val hashed =
            credentialCryptoPort.hashPassword(
                tenantId = command.tenantId,
                userId = null,
                rawPassword = command.password,
                algorithm = HashAlgorithm.ARGON2,
            )

        val credential =
            Credential(
                passwordHash = hashed.hash,
                salt = hashed.salt,
                algorithm = hashed.algorithm,
            )

        val user =
            User.create(
                tenantId = command.tenantId,
                username = command.username,
                email = command.email,
                fullName = command.fullName,
                credential = credential,
                roleIds = roles.map { it.id }.toSet(),
                metadata = command.metadata,
            )

        return userRepository
            .save(user)
            .onSuccess { savedUser ->
                publishEvents(
                    UserCreatedEvent(
                        tenantId = savedUser.tenantId,
                        userId = savedUser.id,
                        username = savedUser.username,
                        email = savedUser.email,
                        status = savedUser.status,
                    ),
                )
            }
    }

    fun assignRole(command: AssignRoleCommand): Result<User> {
        val user =
            when (val result = userRepository.findById(command.tenantId, command.userId)) {
                is Result.Success -> result.value
                is Result.Failure -> return result
            } ?: return failure(
                code = "USER_NOT_FOUND",
                message = "User not found for role assignment",
                details = mapOf("userId" to command.userId.toString()),
            )

        if (user.hasRole(command.roleId)) {
            return failure(
                code = "ROLE_ALREADY_ASSIGNED",
                message = "User already has this role",
                details = mapOf("roleId" to command.roleId.toString()),
            )
        }

        val role =
            when (val result = roleRepository.findById(command.tenantId, command.roleId)) {
                is Result.Success -> result.value
                is Result.Failure -> return result
            } ?: return failure(
                code = "ROLE_NOT_FOUND",
                message = "Role not found for assignment",
                details = mapOf("roleId" to command.roleId.toString()),
            )

        val updatedUser = user.assignRole(role.id)
        return userRepository
            .save(updatedUser)
            .onSuccess { savedUser ->
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
            }
    }

    fun activateUser(command: ActivateUserCommand): Result<User> {
        val user =
            when (val result = userRepository.findById(command.tenantId, command.userId)) {
                is Result.Success -> result.value
                is Result.Failure -> return result
            } ?: return failure(
                code = "USER_NOT_FOUND",
                message = "User not found for activation",
                details = mapOf("userId" to command.userId.toString()),
            )

        val activatedUser =
            try {
                user.activate()
            } catch (ex: IllegalArgumentException) {
                return failure(
                    code = "USER_STATE_INVALID",
                    message = ex.message ?: "User cannot be activated in current state",
                    details =
                        mapOf(
                            "userId" to user.id.toString(),
                            "status" to user.status.name,
                        ),
                )
            }

        val finalUser =
            if (command.requirePasswordReset) {
                activatedUser
            } else {
                activatedUser.clearPasswordChangeRequirement()
            }

        return userRepository
            .save(finalUser)
            .onSuccess { savedUser ->
                publishEvents(
                    UserUpdatedEvent(
                        tenantId = savedUser.tenantId,
                        userId = savedUser.id,
                        updatedFields = setOf("status"),
                        status = savedUser.status,
                    ),
                )
            }
    }

    fun updateCredentials(command: UpdateCredentialsCommand): Result<User> {
        val user =
            when (val result = userRepository.findById(command.tenantId, command.userId)) {
                is Result.Success -> result.value
                is Result.Failure -> return result
            } ?: return failure(
                code = "USER_NOT_FOUND",
                message = "User not found for credential update",
                details = mapOf("userId" to command.userId.toString()),
            )

        try {
            command.currentPassword?.let { currentPassword ->
                authenticationService.requireValidCredentials(user, currentPassword)
            }
        } catch (ex: InvalidCredentialException) {
            return failure(
                code = "INVALID_CREDENTIALS",
                message = ex.message ?: "Current credentials are invalid",
                details = mapOf("userId" to user.id.toString()),
            )
        }

        validatePassword(command.newPassword).let { result ->
            if (result is Result.Failure) {
                return result
            }
        }

        val hashed =
            credentialCryptoPort.hashPassword(
                tenantId = command.tenantId,
                userId = command.userId,
                rawPassword = command.newPassword,
                algorithm = HashAlgorithm.ARGON2,
            )

        val updateResult =
            authenticationService.updatePassword(
                user = user,
                newRawPassword = command.newPassword,
                hashedPassword = hashed.hash,
                salt = hashed.salt,
            )

        return updateResult
            .flatMap { updated -> userRepository.save(updated) }
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

    fun suspendUser(command: SuspendUserCommand): Result<User> =
        when (val userResult = userRepository.findById(command.tenantId, command.userId)) {
            is Result.Failure -> userResult
            is Result.Success -> {
                val user =
                    userResult.value
                        ?: return failure(
                            code = "USER_NOT_FOUND",
                            message = "User not found",
                            details =
                                mapOf(
                                    "tenantId" to command.tenantId.toString(),
                                    "userId" to command.userId.toString(),
                                ),
                        )
                val updated = user.suspend(command.reason)
                userRepository
                    .save(updated)
                    .onSuccess { savedUser ->
                        publishEvents(
                            UserUpdatedEvent(
                                tenantId = savedUser.tenantId,
                                userId = savedUser.id,
                                updatedFields = setOf("status"),
                                status = savedUser.status,
                            ),
                        )
                    }
            }
        }

    fun reactivateUser(command: ReactivateUserCommand): Result<User> =
        when (val userResult = userRepository.findById(command.tenantId, command.userId)) {
            is Result.Failure -> userResult
            is Result.Success -> {
                val user =
                    userResult.value
                        ?: return failure(
                            code = "USER_NOT_FOUND",
                            message = "User not found",
                            details =
                                mapOf(
                                    "tenantId" to command.tenantId.toString(),
                                    "userId" to command.userId.toString(),
                                ),
                        )
                val updated = user.reactivate()
                userRepository
                    .save(updated)
                    .onSuccess { savedUser ->
                        publishEvents(
                            UserUpdatedEvent(
                                tenantId = savedUser.tenantId,
                                userId = savedUser.id,
                                updatedFields = setOf("status"),
                                status = savedUser.status,
                            ),
                        )
                    }
            }
        }

    fun resetPassword(command: ResetPasswordCommand): Result<User> =
        when (val userResult = userRepository.findById(command.tenantId, command.userId)) {
            is Result.Failure -> userResult
            is Result.Success -> {
                val user =
                    userResult.value
                        ?: return failure(
                            code = "USER_NOT_FOUND",
                            message = "User not found",
                            details =
                                mapOf(
                                    "tenantId" to command.tenantId.toString(),
                                    "userId" to command.userId.toString(),
                                ),
                        )

                validatePassword(command.newPassword).let { validation ->
                    if (validation is Result.Failure) {
                        return validation
                    }
                }

                val hashed =
                    credentialCryptoPort.hashPassword(
                        tenantId = command.tenantId,
                        userId = command.userId,
                        rawPassword = command.newPassword,
                        algorithm = HashAlgorithm.ARGON2,
                    )

                var updated = user.resetPassword(hashed.hash, hashed.salt)
                if (!command.requirePasswordChange) {
                    updated = updated.clearPasswordChangeRequirement()
                }

                userRepository
                    .save(updated)
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
        }

    fun authenticate(command: AuthenticateUserCommand): Result<User> {
        val userResult = findUserByIdentifier(command.tenantId, command.usernameOrEmail)
        val user =
            when (userResult) {
                is Result.Success -> userResult.value
                is Result.Failure -> return userResult
            } ?: run {
                // Anti-enumeration: keep response generic and add a small constant-time guard
                try {
                    Thread.sleep(100)
                } catch (_: InterruptedException) {
                }
                return failure(
                    code = "AUTHENTICATION_FAILED",
                    message = "Authentication failed",
                )
            }

        return when (val result = authenticationService.authenticate(user, command.password)) {
            is AuthenticationResult.Success ->
                userRepository
                    .save(result.user)
            is AuthenticationResult.Failure -> {
                userRepository.save(result.user)
                Result.Failure(result.reason)
            }
        }
    }

    private fun findUserByIdentifier(
        tenantId: TenantId,
        identifier: String,
    ): Result<User?> {
        val byUsername = userRepository.findByUsername(tenantId, identifier)
        when (byUsername) {
            is Result.Failure -> return byUsername
            is Result.Success ->
                if (byUsername.value != null) {
                    return byUsername
                }
        }
        return userRepository.findByEmail(tenantId, identifier)
    }

    private fun publishEvents(vararg events: DomainEvent) {
        if (events.isNotEmpty()) {
            eventPublisher.publish(events.toList())
        }
    }

    private fun ensureTenantExists(tenantId: TenantId): Result<Unit> =
        when (val result = tenantRepository.findById(tenantId)) {
            is Result.Success ->
                if (result.value != null) {
                    success(Unit)
                } else {
                    failure(
                        code = "TENANT_NOT_FOUND",
                        message = "Tenant not found",
                        details = mapOf("tenantId" to tenantId.toString()),
                    )
                }
            is Result.Failure -> result
        }

    private fun ensureUsernameAvailable(
        tenantId: TenantId,
        username: String,
    ): Result<Unit> =
        when (val result = userRepository.existsByUsername(tenantId, username)) {
            is Result.Success ->
                if (result.value) {
                    failure(
                        code = "USERNAME_IN_USE",
                        message = "Username already in use",
                        details = mapOf("username" to username),
                    )
                } else {
                    success(Unit)
                }
            is Result.Failure -> result
        }

    private fun ensureEmailAvailable(
        tenantId: TenantId,
        email: String,
    ): Result<Unit> =
        when (val result = userRepository.existsByEmail(tenantId, email)) {
            is Result.Success ->
                if (result.value) {
                    failure(
                        code = "EMAIL_IN_USE",
                        message = "Email already in use",
                        details = mapOf("email" to email),
                    )
                } else {
                    success(Unit)
                }
            is Result.Failure -> result
        }

    private fun validatePassword(password: String): Result<Unit> {
        val validationErrors = passwordPolicy.validate(password)
        return if (validationErrors.isEmpty()) {
            success(Unit)
        } else {
            failure(
                code = "WEAK_PASSWORD",
                message = "Password does not meet requirements",
                validationErrors = validationErrors,
            )
        }
    }
}
