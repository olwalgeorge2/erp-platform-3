package com.erp.finance.accounting.domain.model

import java.time.Instant
import java.util.UUID

data class ChartOfAccounts(
    val id: ChartOfAccountsId = ChartOfAccountsId(),
    val tenantId: UUID,
    val baseCurrency: String,
    val code: String = "DEFAULT",
    val name: String = "Default Chart",
    val accounts: Map<AccountId, Account> = emptyMap(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(baseCurrency.length == 3) { "Currency must be ISO-4217 3-letter code" }
        require(code.isNotBlank()) { "Chart of accounts code cannot be blank" }
        require(name.isNotBlank()) { "Chart of accounts name cannot be blank" }
    }

    fun defineAccount(
        code: String,
        name: String,
        type: AccountType,
        currency: String = baseCurrency,
        parentAccountId: AccountId? = null,
        isPosting: Boolean = true,
    ): ChartOfAccounts {
        require(code.isNotBlank()) { "Account code cannot be blank" }
        require(name.isNotBlank()) { "Account name cannot be blank" }
        require(currency.length == 3) { "Currency must be ISO-4217 3-letter code" }
        require(accounts.values.none { it.code.equals(code, ignoreCase = true) }) {
            "Account code already exists: $code"
        }

        parentAccountId?.let {
            require(accounts.containsKey(it)) { "Parent account must exist" }
            require(!accounts.getValue(it).isPosting) { "Posting account cannot be a parent" }
        }

        val account =
            Account(
                id = AccountId(),
                code = code,
                name = name,
                type = type,
                currency = currency,
                parentAccountId = parentAccountId,
                isPosting = isPosting,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        return copy(
            accounts = accounts + (account.id to account),
            updatedAt = Instant.now(),
        )
    }
}

data class Account(
    val id: AccountId,
    val code: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val parentAccountId: AccountId? = null,
    val isPosting: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(code.isNotBlank()) { "Account code cannot be blank" }
        require(name.isNotBlank()) { "Account name cannot be blank" }
        require(currency.length == 3) { "Currency must be ISO-4217 3-letter code" }
    }
}

enum class AccountType { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }
