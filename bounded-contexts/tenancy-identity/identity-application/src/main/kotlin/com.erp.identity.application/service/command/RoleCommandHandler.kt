package com.erp.identity.application.service.command

import com.erp.identity.application.port.input.command.CreateRoleCommand
import com.erp.identity.application.port.input.command.DeleteRoleCommand
import com.erp.identity.application.port.input.command.UpdateRoleCommand
import com.erp.identity.application.port.output.RoleRepository
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.failure
import com.erp.shared.types.results.Result.Success

class RoleCommandHandler(
    private val roleRepository: RoleRepository,
) {
    fun createRole(command: CreateRoleCommand): Result<Role> {
        val uniqueness = roleRepository.existsByName(command.tenantId, command.name)
        when (uniqueness) {
            is Result.Failure -> return uniqueness
            is Success ->
                if (uniqueness.value) {
                    return failure(
                        code = "ROLE_NAME_EXISTS",
                        message = "Role name already exists",
                        details =
                            mapOf(
                                "tenantId" to command.tenantId.toString(),
                                "name" to command.name,
                            ),
                    )
                }
        }

        val role =
            Role.create(
                tenantId = command.tenantId,
                name = command.name,
                description = command.description,
                permissions = command.permissions,
                isSystem = command.isSystem,
                metadata = command.metadata,
            )
        return roleRepository.save(role)
    }

    fun updateRole(command: UpdateRoleCommand): Result<Role> {
        val existing = roleRepository.findById(command.tenantId, command.roleId)
        val role =
            when (existing) {
                is Result.Failure -> return existing
                is Result.Success ->
                    existing.value ?: return failure(
                        code = "ROLE_NOT_FOUND",
                        message = "Role not found",
                        details = mapOf("roleId" to command.roleId.toString()),
                    )
            }

        if (role.isSystem) {
            return failure(
                code = "ROLE_IMMUTABLE",
                message = "System roles cannot be modified",
                details = mapOf("roleId" to role.id.toString()),
            )
        }

        val updated =
            role.copy(
                name = command.name,
                description = command.description,
                permissions = command.permissions,
                metadata = command.metadata,
            )
        return roleRepository.save(updated)
    }

    fun deleteRole(command: DeleteRoleCommand): Result<Unit> {
        val existing = roleRepository.findById(command.tenantId, command.roleId)
        val role =
            when (existing) {
                is Result.Failure -> return existing
                is Result.Success ->
                    existing.value ?: return failure(
                        code = "ROLE_NOT_FOUND",
                        message = "Role not found",
                        details = mapOf("roleId" to command.roleId.toString()),
                    )
            }

        if (role.isSystem) {
            return failure(
                code = "ROLE_IMMUTABLE",
                message = "System roles cannot be deleted",
                details = mapOf("roleId" to role.id.toString()),
            )
        }

        return roleRepository.delete(command.tenantId, command.roleId)
    }

    fun listRoles(
        tenantId: TenantId,
        limit: Int,
        offset: Int,
    ): Result<List<Role>> = roleRepository.list(tenantId, limit, offset)
}
