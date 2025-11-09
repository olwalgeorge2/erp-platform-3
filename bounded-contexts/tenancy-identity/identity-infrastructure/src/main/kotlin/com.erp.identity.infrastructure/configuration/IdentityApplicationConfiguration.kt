package com.erp.identity.infrastructure.configuration

import com.erp.identity.application.port.output.CredentialCryptoPort
import com.erp.identity.application.port.output.EventPublisherPort
import com.erp.identity.application.port.output.RoleRepository
import com.erp.identity.application.port.output.TenantRepository
import com.erp.identity.application.port.output.UserRepository
import com.erp.identity.application.service.command.RoleCommandHandler
import com.erp.identity.application.service.command.TenantCommandHandler
import com.erp.identity.application.service.command.UserCommandHandler
import com.erp.identity.application.service.query.RoleQueryHandler
import com.erp.identity.application.service.query.TenantQueryHandler
import com.erp.identity.application.service.query.UserQueryHandler
import com.erp.identity.domain.model.identity.PasswordPolicy
import com.erp.identity.domain.services.AuthenticationService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces

@ApplicationScoped
class IdentityApplicationConfiguration {
    @Produces
    fun passwordPolicy(): PasswordPolicy =
        PasswordPolicy(
            minLength = 12,
            maxLength = 128,
            requireUppercase = true,
            requireLowercase = true,
            requireNumber = true,
            requireSpecialCharacter = true,
        )

    @Produces
    fun authenticationService(
        credentialCryptoPort: CredentialCryptoPort,
        passwordPolicy: PasswordPolicy,
    ): AuthenticationService =
        AuthenticationService(credentialVerifier = credentialCryptoPort, passwordPolicy = passwordPolicy)

    @Produces
    fun userCommandHandler(
        tenantRepository: TenantRepository,
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        credentialCryptoPort: CredentialCryptoPort,
        authenticationService: AuthenticationService,
        eventPublisherPort: EventPublisherPort,
        passwordPolicy: PasswordPolicy,
    ): UserCommandHandler =
        UserCommandHandler(
            tenantRepository = tenantRepository,
            userRepository = userRepository,
            roleRepository = roleRepository,
            credentialCryptoPort = credentialCryptoPort,
            authenticationService = authenticationService,
            eventPublisher = eventPublisherPort,
            passwordPolicy = passwordPolicy,
        )

    @Produces
    fun tenantCommandHandler(
        tenantRepository: TenantRepository,
        eventPublisherPort: EventPublisherPort,
    ): TenantCommandHandler =
        TenantCommandHandler(
            tenantRepository = tenantRepository,
            eventPublisher = eventPublisherPort,
        )

    @Produces
    fun tenantQueryHandler(tenantRepository: TenantRepository): TenantQueryHandler =
        TenantQueryHandler(tenantRepository)

    @Produces
    fun roleCommandHandler(roleRepository: RoleRepository): RoleCommandHandler = RoleCommandHandler(roleRepository)

    @Produces
    fun roleQueryHandler(roleRepository: RoleRepository): RoleQueryHandler = RoleQueryHandler(roleRepository)

    @Produces
    fun userQueryHandler(userRepository: UserRepository): UserQueryHandler = UserQueryHandler(userRepository)
}
