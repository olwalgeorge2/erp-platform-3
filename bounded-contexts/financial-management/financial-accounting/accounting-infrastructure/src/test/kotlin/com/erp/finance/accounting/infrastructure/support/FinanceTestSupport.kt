package com.erp.finance.accounting.infrastructure.support

import com.erp.finance.accounting.application.port.output.AccountingPeriodRepository
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.infrastructure.outbox.FinanceOutboxEventEntity
import com.erp.finance.accounting.infrastructure.outbox.FinanceOutboxRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class AccountingPeriodTestSupport(
    private val accountingPeriodRepository: AccountingPeriodRepository,
) {
    @Transactional
    fun save(period: AccountingPeriod): AccountingPeriod = accountingPeriodRepository.save(period)
}

@ApplicationScoped
class FinanceOutboxTestSupport(
    private val outboxRepository: FinanceOutboxRepository,
) {
    @Transactional
    fun fetchPending(
        limit: Int,
        maxAttempts: Int,
    ): List<FinanceOutboxEventEntity> = outboxRepository.fetchPending(limit, maxAttempts)

    @Transactional
    fun countPending(maxAttempts: Int): Long = outboxRepository.countPending(maxAttempts)
}
