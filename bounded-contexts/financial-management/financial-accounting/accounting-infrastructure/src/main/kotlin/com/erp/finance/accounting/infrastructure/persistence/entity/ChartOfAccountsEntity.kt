package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.Account
import com.erp.finance.accounting.domain.model.AccountId
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.ChartOfAccounts
import com.erp.finance.accounting.domain.model.ChartOfAccountsId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "chart_of_accounts", schema = "financial_accounting")
class ChartOfAccountsEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "code", nullable = false, length = 64)
    var code: String,
    @Column(name = "name", nullable = false, length = 255)
    var name: String,
    @Column(name = "base_currency", nullable = false, length = 3)
    var baseCurrency: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(name = "created_by", nullable = false, length = 128)
    var createdBy: String = DEFAULT_ACTOR,
    @Column(name = "updated_by", nullable = false, length = 128)
    var updatedBy: String = DEFAULT_ACTOR,
    @Column(name = "source_system", nullable = false, length = 64)
    var sourceSystem: String = DEFAULT_SOURCE,
    @Version
    @Column(name = "version")
    var version: Int? = null,
) {
    @OneToMany(
        mappedBy = "chartOfAccounts",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    var accounts: MutableSet<AccountEntity> = linkedSetOf()

    @PrePersist
    fun prePersist() {
        createdBy = createdBy.ifBlank { DEFAULT_ACTOR }
        updatedBy = updatedBy.ifBlank { createdBy }
        sourceSystem = sourceSystem.ifBlank { DEFAULT_SOURCE }
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedBy = updatedBy.ifBlank { DEFAULT_ACTOR }
        sourceSystem = sourceSystem.ifBlank { DEFAULT_SOURCE }
        updatedAt = Instant.now()
    }

    fun toDomain(): ChartOfAccounts =
        ChartOfAccounts(
            id = ChartOfAccountsId(id),
            tenantId = tenantId,
            baseCurrency = baseCurrency,
            code = code,
            name = name,
            accounts =
                accounts
                    .map { entity -> AccountId(entity.id) to entity.toDomain() }
                    .toMap(),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun updateFrom(domain: ChartOfAccounts) {
        tenantId = domain.tenantId
        code = domain.code
        name = domain.name
        baseCurrency = domain.baseCurrency
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt

        val existingById = accounts.associateBy { it.id }
        val desiredIds =
            domain.accounts.keys
                .map { it.value }
                .toSet()

        accounts.removeIf { entity -> entity.id !in desiredIds }

        val entityById =
            mutableMapOf<UUID, AccountEntity>()

        domain.accounts.values.forEach { account ->
            val entity =
                existingById[account.id.value]?.apply {
                    code = account.code
                    name = account.name
                    accountType = account.type
                    currency = account.currency
                    isPosting = account.isPosting
                    createdAt = account.createdAt
                    updatedAt = account.updatedAt
                } ?: AccountEntity(
                    id = account.id.value,
                    tenantId = domain.tenantId,
                    chartOfAccounts = this,
                    code = account.code,
                    name = account.name,
                    accountType = account.type,
                    currency = account.currency,
                    isPosting = account.isPosting,
                    createdAt = account.createdAt,
                    updatedAt = account.updatedAt,
                ).also {
                    accounts.add(it)
                }
            entityById[account.id.value] = entity
        }

        domain.accounts.values.forEach { account ->
            val entity = entityById.getValue(account.id.value)
            entity.parentAccount = account.parentAccountId?.let { parentId -> entityById[parentId.value] }
        }
    }

    companion object {
        fun from(domain: ChartOfAccounts): ChartOfAccountsEntity =
            ChartOfAccountsEntity(
                id = domain.id.value,
                tenantId = domain.tenantId,
                code = domain.code,
                name = domain.name,
                baseCurrency = domain.baseCurrency,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            ).also { entity ->
                entity.updateFrom(domain)
            }

        private const val DEFAULT_ACTOR = "system"
        private const val DEFAULT_SOURCE = "erp-platform"
    }
}

@Entity
@Table(name = "accounts", schema = "financial_accounting")
class AccountEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chart_of_accounts_id", nullable = false)
    var chartOfAccounts: ChartOfAccountsEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    var parentAccount: AccountEntity? = null,
    @Column(name = "code", nullable = false, length = 64)
    var code: String,
    @Column(name = "name", nullable = false, length = 255)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    var accountType: AccountType,
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,
    @Column(name = "is_posting", nullable = false)
    var isPosting: Boolean = true,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "version")
    var version: Int? = null,
) {
    fun toDomain(): Account =
        Account(
            id = AccountId(id),
            code = code,
            name = name,
            type = accountType,
            currency = currency,
            parentAccountId = parentAccount?.let { AccountId(it.id) },
            isPosting = isPosting,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
