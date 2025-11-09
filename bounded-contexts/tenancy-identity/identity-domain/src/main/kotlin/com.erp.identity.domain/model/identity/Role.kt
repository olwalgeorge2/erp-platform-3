package com.erp.identity.domain.model.identity

import com.erp.identity.domain.model.tenant.TenantId
import java.time.Instant

/**
 * Role aggregate root - represents a collection of permissions for RBAC
 */
data class Role(
    val id: RoleId,
    val tenantId: TenantId,
    val name: String,
    val description: String,
    val permissions: Set<Permission>,
    val isSystem: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(name.isNotBlank()) { "Role name cannot be blank" }
        require(name.length in 2..100) { "Role name must be between 2 and 100 characters" }
        require(description.length <= 500) { "Description must be at most 500 characters" }
        if (isSystem) {
            require(permissions.isNotEmpty()) { "System roles must have at least one permission" }
        }
    }

    companion object {
        fun create(
            tenantId: TenantId,
            name: String,
            description: String,
            permissions: Set<Permission> = emptySet(),
            isSystem: Boolean = false,
            metadata: Map<String, String> = emptyMap(),
        ): Role =
            Role(
                id = RoleId.generate(),
                tenantId = tenantId,
                name = name,
                description = description,
                permissions = permissions,
                isSystem = isSystem,
                metadata = metadata,
            )
    }

    // Permission management
    fun grantPermission(permission: Permission): Role {
        require(!isSystem) { "Cannot modify system roles" }
        require(!permissions.contains(permission)) { "Role already has this permission" }
        return copy(
            permissions = permissions + permission,
            updatedAt = Instant.now(),
        )
    }

    fun revokePermission(permission: Permission): Role {
        require(!isSystem) { "Cannot modify system roles" }
        require(permissions.contains(permission)) { "Role does not have this permission" }
        return copy(
            permissions = permissions - permission,
            updatedAt = Instant.now(),
        )
    }

    fun updateDescription(newDescription: String): Role {
        require(!isSystem) { "Cannot modify system roles" }
        require(newDescription.length <= 500) { "Description must be at most 500 characters" }
        return copy(
            description = newDescription,
            updatedAt = Instant.now(),
        )
    }

    // Query methods
    fun hasPermission(permission: Permission): Boolean = permissions.contains(permission)

    fun hasPermission(
        resource: String,
        action: String,
    ): Boolean = permissions.any { it.resource == resource && it.action == action }

    fun hasAnyPermission(requiredPermissions: Set<Permission>): Boolean =
        permissions.intersect(requiredPermissions).isNotEmpty()

    fun hasAllPermissions(requiredPermissions: Set<Permission>): Boolean = permissions.containsAll(requiredPermissions)
}
