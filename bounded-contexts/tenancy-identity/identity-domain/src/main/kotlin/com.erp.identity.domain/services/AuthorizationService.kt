package com.erp.identity.domain.services

import com.erp.identity.domain.model.identity.Permission
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.identity.User
import com.erp.shared.types.results.Result

class AuthorizationService {
    fun resolvePermissions(
        user: User,
        roles: Collection<Role>,
    ): Set<Permission> {
        val assignedRoles: Map<RoleId, Role> = roles.associateBy { it.id }
        val permissions =
            user.roleIds
                .mapNotNull { assignedRoles[it] }
                .flatMap { it.permissions }
                .toMutableSet()

        return permissions
    }

    fun hasPermission(
        user: User,
        requiredPermission: Permission,
        roles: Collection<Role>,
    ): Boolean = resolvePermissions(user, roles).contains(requiredPermission)

    fun hasAllPermissions(
        user: User,
        requiredPermissions: Set<Permission>,
        roles: Collection<Role>,
    ): Boolean = resolvePermissions(user, roles).containsAll(requiredPermissions)

    fun ensurePermissions(
        user: User,
        requiredPermissions: Set<Permission>,
        roles: Collection<Role>,
    ): Result<Unit> {
        val missing =
            requiredPermissions.filterNot { permission ->
                hasPermission(user, permission, roles)
            }

        if (missing.isEmpty()) {
            return Result.success(Unit)
        }

        return Result.failure(
            code = "INSUFFICIENT_PERMISSIONS",
            message = "User lacks required permissions",
            details =
                mapOf(
                    "userId" to user.id.toString(),
                    "missing" to missing.joinToString { it.toPermissionString() },
                ),
        )
    }
}
