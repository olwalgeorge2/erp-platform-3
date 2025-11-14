package com.erp.finance.accounting.infrastructure.outbox

import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Instant
import java.time.temporal.ChronoUnit

@ApplicationScoped
class FinanceOutboxCleanupScheduler(
    private val outboxRepository: FinanceOutboxRepository,
) {
    @Inject
    @ConfigProperty(name = "finance.outbox.retention-days", defaultValue = "7")
    lateinit var retentionDays: String

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(TxType.REQUIRES_NEW)
    fun purgeExpiredEvents() {
        val cutoff = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS)
        val deleted =
            outboxRepository.deleteOlderThan(
                statuses =
                    setOf(
                        FinanceOutboxEventStatus.PUBLISHED,
                        FinanceOutboxEventStatus.FAILED,
                    ),
                cutoff = cutoff,
            )

        if (deleted > 0) {
            LOGGER.infof("Deleted %d finance outbox events older than %s", deleted, cutoff)
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(FinanceOutboxCleanupScheduler::class.java)
    }
}
