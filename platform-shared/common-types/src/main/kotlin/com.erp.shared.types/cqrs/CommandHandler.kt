package com.erp.shared.types.cqrs

/**
 * Handles a command and returns a result.
 */
fun interface CommandHandler<C : Command<R>, R> {
    fun handle(command: C): R
}
