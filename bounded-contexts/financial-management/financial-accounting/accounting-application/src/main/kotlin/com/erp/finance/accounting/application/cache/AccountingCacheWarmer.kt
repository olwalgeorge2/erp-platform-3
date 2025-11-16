package com.erp.finance.accounting.application.cache

import com.erp.finance.accounting.application.port.output.ChartOfAccountsRepository
import com.erp.finance.accounting.application.port.output.LedgerRepository
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@ApplicationScoped
class AccountingCacheWarmer(
    private val ledgerRepository: LedgerRepository,
    private val chartRepository: ChartOfAccountsRepository,
    private val ledgerCache: LedgerExistenceCache,
    private val chartCache: ChartOfAccountsCache,
    @ConfigProperty(name = "validation.performance.cache.warmup.enabled", defaultValue = "true")
    private val warmupEnabled: Boolean,
    @ConfigProperty(name = "validation.performance.cache.warmup.ledgers", defaultValue = "50")
    private val ledgerWarmCount: Int,
    @ConfigProperty(name = "validation.performance.cache.warmup.charts", defaultValue = "25")
    private val chartWarmCount: Int,
) {
    @PostConstruct
    fun preload() {
        if (!warmupEnabled) {
            LOGGER.debug("Validation cache warmup disabled via configuration")
            return
        }
        if (ledgerWarmCount > 0) {
            val ledgers = ledgerRepository.findRecent(ledgerWarmCount)
            ledgerCache.warmup(ledgers)
            LOGGER.infof("Preloaded %d ledgers into validation cache", ledgers.size)
        }
        if (chartWarmCount > 0) {
            val charts = chartRepository.findRecent(chartWarmCount)
            chartCache.warmup(charts)
            LOGGER.infof("Preloaded %d charts into validation cache", charts.size)
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(AccountingCacheWarmer::class.java)
    }
}
