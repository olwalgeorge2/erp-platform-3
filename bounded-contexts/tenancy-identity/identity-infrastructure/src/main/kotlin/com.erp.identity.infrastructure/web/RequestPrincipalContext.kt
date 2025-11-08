package com.erp.identity.infrastructure.web

/**
 * Stores the authenticated principal (if present) for the duration of the request.
 */
data class RequestPrincipal(
    val userId: String?,
    val tenantId: String?,
    val roles: Set<String>,
    val permissions: Set<String>,
) {
    fun hasRole(role: String): Boolean = roles.any { it.equals(role, ignoreCase = true) }

    fun hasPermission(
        resource: String,
        action: String,
    ): Boolean =
        permissions.any { perm ->
            val normalized = perm.lowercase()
            normalized == "${resource.lowercase()}:${action.lowercase()}" || normalized == "${resource.lowercase()}:*"
        }
}

object RequestPrincipalContext {
    private val holder = ThreadLocal<RequestPrincipal?>()

    fun set(principal: RequestPrincipal?) {
        holder.set(principal)
    }

    fun get(): RequestPrincipal? = holder.get()

    fun clear() {
        holder.remove()
    }
}
