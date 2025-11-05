package com.erp.shared.types.events

import java.util.UUID

/**
 * Metadata accompanying a domain event for correlation and auditing.
 */
data class EventMetadata(
    val tenantId: UUID? = null,
    val correlationId: UUID? = null,
    val causationId: UUID? = null,
    val additional: Map<String, String> = emptyMap(),
) {
    init {
        require(additional.size == additional.keys.size) { "Additional metadata keys must be unique" }
    }

    fun withAdditional(entries: Map<String, String>): EventMetadata =
        copy(additional = additional + entries)
}
