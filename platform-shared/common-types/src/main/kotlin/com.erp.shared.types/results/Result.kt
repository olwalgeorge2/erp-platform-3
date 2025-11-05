package com.erp.shared.types.results

sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()

    data class Failure(
        val error: DomainError,
        val validationErrors: List<ValidationError> = emptyList(),
    ) : Result<Nothing>() {
        init {
            val duplicates = validationErrors.groupBy { it.field }.filterValues { it.size > 1 }
            require(duplicates.isEmpty()) { "Duplicate validation errors for fields: ${duplicates.keys.joinToString()}" }
        }
    }

    fun isSuccess(): Boolean = this is Success

    fun isFailure(): Boolean = this is Failure

    inline fun <R> map(transform: (T) -> R): Result<R> =
        when (this) {
            is Success -> Success(transform(value))
            is Failure -> this
        }

    inline fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) {
            block(value)
        }
        return this
    }

    inline fun onFailure(block: (Failure) -> Unit): Result<T> {
        if (this is Failure) {
            block(this)
        }
        return this
    }

    companion object {
        fun <T> success(value: T): Result<T> = Success(value)

        fun failure(
            code: String,
            message: String,
            details: Map<String, String> = emptyMap(),
            cause: Throwable? = null,
            validationErrors: List<ValidationError> = emptyList(),
        ): Failure =
            Failure(
                DomainError(
                    code = code,
                    message = message,
                    details = details,
                    cause = cause,
                ),
                validationErrors,
            )
    }
}
