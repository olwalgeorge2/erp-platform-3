package com.erp.identity.domain.model.identity

/**
 * Value object representing a permission in the RBAC system
 * Permissions are resource-action pairs (e.g., "user:create", "invoice:read")
 */
data class Permission(
    val resource: String,
    val action: String,
    val scope: PermissionScope = PermissionScope.TENANT,
) {
    init {
        require(resource.isNotBlank()) { "Resource cannot be blank" }
        require(resource.matches(RESOURCE_REGEX)) { "Resource must be lowercase alphanumeric with hyphens" }
        require(action.isNotBlank()) { "Action cannot be blank" }
        require(action in VALID_ACTIONS) { "Action must be one of: ${VALID_ACTIONS.joinToString()}" }
    }

    companion object {
        private val RESOURCE_REGEX = "^[a-z0-9-]+$".toRegex()
        private val VALID_ACTIONS =
            setOf(
                "create",
                "read",
                "update",
                "delete",
                "list",
                "execute",
                "manage",
            )

        fun of(
            resource: String,
            action: String,
            scope: PermissionScope = PermissionScope.TENANT,
        ): Permission = Permission(resource, action, scope)

        fun create(resource: String): Permission = Permission(resource, "create")

        fun read(resource: String): Permission = Permission(resource, "read")

        fun update(resource: String): Permission = Permission(resource, "update")

        fun delete(resource: String): Permission = Permission(resource, "delete")

        fun list(resource: String): Permission = Permission(resource, "list")

        fun manage(resource: String): Permission = Permission(resource, "manage")
    }

    /**
     * Returns permission in string format: "resource:action"
     */
    fun toPermissionString(): String = "$resource:$action"

    override fun toString(): String = "$resource:$action:$scope"
}

enum class PermissionScope {
    SYSTEM,
    TENANT,
    USER,
}
