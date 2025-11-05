package com.erp.identity.domain.model.tenant

import java.util.UUID

/**
 * Value object representing a unique tenant identifier.
 * Immutable and self-validating.
 */
@JvmInline
value class TenantId(
    val value: UUID,
) {
    init {
        require(value.toString().isNotBlank()) { "TenantId cannot be blank" }
    }

    companion object {
        fun generate(): TenantId = TenantId(UUID.randomUUID())

        fun from(value: String): TenantId = TenantId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
