package com.erp.shared.types.results

/**
 * Field-level validation error details.
 */
data class ValidationError(
    val field: String,
    val code: String,
    val message: String,
    val rejectedValue: String? = null,
) {
    init {
        require(field.isNotBlank()) { "Validation field cannot be blank" }
        require(code.isNotBlank()) { "Validation code cannot be blank" }
        require(message.isNotBlank()) { "Validation message cannot be blank" }
    }
}
