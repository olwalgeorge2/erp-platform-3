package com.erp.identity.domain.model.identity

import java.util.UUID

/**
 * Value object representing a unique user identifier.
 * Immutable and self-validating.
 */
@JvmInline
value class UserId(
    val value: UUID,
) {
    init {
        require(value.toString().isNotBlank()) { "UserId cannot be blank" }
    }

    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())

        fun from(value: String): UserId = UserId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
