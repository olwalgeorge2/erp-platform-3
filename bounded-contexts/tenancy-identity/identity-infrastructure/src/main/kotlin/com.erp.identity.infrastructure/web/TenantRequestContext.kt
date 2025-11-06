package com.erp.identity.infrastructure.web

/**
 * Stores tenant identifier for the duration of a request.
 */
object TenantRequestContext {
    private val holder = ThreadLocal<String?>()

    fun set(tenantId: String?) {
        holder.set(tenantId)
    }

    fun get(): String? = holder.get()

    fun clear() {
        holder.remove()
    }
}
