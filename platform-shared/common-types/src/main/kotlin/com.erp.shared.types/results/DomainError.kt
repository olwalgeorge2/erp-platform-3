package com.erp.shared.types.results

/**
 * Represents a domain-level error.
 */
data class DomainError(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val cause: Throwable? = null,
) {
    init {
        require(code.isNotBlank()) { "Error code cannot be blank" }
        require(message.isNotBlank()) { "Error message cannot be blank" }
    }

    fun withDetails(vararg pairs: Pair<String, String>): DomainError = copy(details = details + pairs.toMap())
}
