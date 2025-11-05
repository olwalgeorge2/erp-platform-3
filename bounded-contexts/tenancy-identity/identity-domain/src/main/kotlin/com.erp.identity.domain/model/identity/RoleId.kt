package com.erp.identity.domain.model.identity

import java.util.UUID

/**
 * Value object representing a unique role identifier
 */
@JvmInline
value class RoleId(
    val value: UUID,
) {
    companion object {
        fun generate(): RoleId = RoleId(UUID.randomUUID())

        fun from(value: String): RoleId = RoleId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
