package com.erp.shared.types.cqrs

/**
 * Handles a query and returns the requested data.
 */
fun interface QueryHandler<Q : Query<R>, R> {
    fun handle(query: Q): R
}
