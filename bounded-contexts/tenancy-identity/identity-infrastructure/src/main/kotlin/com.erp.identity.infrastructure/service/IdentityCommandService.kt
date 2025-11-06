package com.erp.identity.infrastructure.service

import com.erp.identity.application.port.input.command.AssignRoleCommand
import com.erp.identity.application.port.input.command.AuthenticateUserCommand
import com.erp.identity.application.port.input.command.CreateUserCommand
import com.erp.identity.application.port.input.command.ProvisionTenantCommand
import com.erp.identity.application.port.input.command.UpdateCredentialsCommand
import com.erp.identity.application.service.command.TenantCommandHandler
import com.erp.identity.application.service.command.UserCommandHandler
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.shared.types.results.Result
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType

@ApplicationScoped
class IdentityCommandService(
    private val userCommandHandler: UserCommandHandler,
    private val tenantCommandHandler: TenantCommandHandler,
) {
    @Transactional(TxType.REQUIRED)
    fun createUser(command: CreateUserCommand): Result<User> = userCommandHandler.createUser(command)

    @Transactional(TxType.REQUIRED)
    fun assignRole(command: AssignRoleCommand): Result<User> = userCommandHandler.assignRole(command)

    @Transactional(TxType.REQUIRED)
    fun updateCredentials(command: UpdateCredentialsCommand): Result<User> = userCommandHandler.updateCredentials(command)

    @Transactional(TxType.REQUIRED)
    fun authenticate(command: AuthenticateUserCommand): Result<User> = userCommandHandler.authenticate(command)

    @Transactional(TxType.REQUIRED)
    fun provisionTenant(command: ProvisionTenantCommand): Result<Tenant> = tenantCommandHandler.provisionTenant(command)
}
