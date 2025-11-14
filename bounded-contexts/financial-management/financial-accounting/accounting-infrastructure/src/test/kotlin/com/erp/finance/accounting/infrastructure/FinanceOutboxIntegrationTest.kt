package com.erp.finance.accounting.infrastructure

import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.JournalEntryLineCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.Money
import com.erp.finance.accounting.infrastructure.outbox.FinanceOutboxEventScheduler
import com.erp.finance.accounting.infrastructure.service.FinanceCommandService
import com.erp.finance.accounting.infrastructure.support.AccountingPeriodTestSupport
import com.erp.finance.accounting.infrastructure.support.FinanceOutboxTestSupport
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.time.Duration
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@QuarkusTest
@EnabledIfSystemProperty(named = "withContainers", matches = "true")
@QuarkusTestResource(FinancePostgresTestResource::class)
@QuarkusTestResource(FinanceKafkaTestResource::class)
class FinanceOutboxIntegrationTest {
    @Inject
    lateinit var financeCommandService: FinanceCommandService

    @Inject
    lateinit var periodSeeder: AccountingPeriodTestSupport

    @Inject
    lateinit var outboxTester: FinanceOutboxTestSupport

    @Inject
    lateinit var scheduler: FinanceOutboxEventScheduler

    @Test
    fun `journal entry traverses outbox to kafka`() {
        val tenantId = UUID.randomUUID()
        val chartId = UUID.randomUUID()

        val ledger =
            financeCommandService.createLedger(
                CreateLedgerCommand(
                    tenantId = tenantId,
                    chartOfAccountsId = chartId,
                    baseCurrency = "USD",
                    chartCode = "FIN-${chartId.toString().take(6)}",
                    chartName = "Finance Integration Chart",
                ),
            )

        val period =
            AccountingPeriod(
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-01",
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-31"),
            )
        periodSeeder.save(period)

        val cashAccount =
            financeCommandService
                .defineAccount(
                    DefineAccountCommand(
                        tenantId = tenantId,
                        chartOfAccountsId = chartId,
                        code = "1000",
                        name = "Cash",
                        type = AccountType.ASSET,
                        currency = "USD",
                        isPosting = true,
                    ),
                ).accounts.values
                .first { it.code == "1000" }
                .id

        val revenueAccount =
            financeCommandService
                .defineAccount(
                    DefineAccountCommand(
                        tenantId = tenantId,
                        chartOfAccountsId = chartId,
                        code = "4000",
                        name = "Revenue",
                        type = AccountType.REVENUE,
                        currency = "USD",
                        isPosting = true,
                    ),
                ).accounts.values
                .first { it.code == "4000" }
                .id

        val command =
            PostJournalEntryCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = period.id.value,
                reference = "JE-IT",
                description = "Integration Test Entry",
                lines =
                    listOf(
                        JournalEntryLineCommand(
                            accountId = cashAccount,
                            direction = EntryDirection.DEBIT,
                            amount = Money(1_000),
                            currency = "USD",
                            description = "Cash",
                        ),
                        JournalEntryLineCommand(
                            accountId = revenueAccount,
                            direction = EntryDirection.CREDIT,
                            amount = Money(1_000),
                            currency = "USD",
                            description = "Revenue",
                        ),
                    ),
            )

        financeCommandService.postJournalEntry(command)

        val pending = outboxTester.fetchPending(10, 5)
        assertTrue(pending.isNotEmpty(), "Expected outbox events after posting journal entry")

        scheduler.publishPendingEvents()

        kafkaConsumer().use { kafka ->
            kafka.subscribe(listOf("finance.journal.events.v1"))
            val records = kafka.poll(Duration.ofSeconds(5))
            assertTrue(records.count() > 0, "Expected Kafka event for journal posting")
            val payload = records.first().value()
            val json = ObjectMapper().readTree(payload)
            assertEquals("finance.journal.posted", json["eventType"].asText())
        }

        assertEquals(0, outboxTester.countPending(5))
    }

    private fun kafkaConsumer(): KafkaConsumer<String, String> {
        val bootstrap =
            System.getProperty("KAFKA_BOOTSTRAP_SERVERS")
                ?: System.getenv("KAFKA_BOOTSTRAP_SERVERS")
                ?: error("KAFKA_BOOTSTRAP_SERVERS not configured for integration test")

        val props =
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.GROUP_ID_CONFIG, "finance-outbox-it-${UUID.randomUUID()}")
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            }
        return KafkaConsumer(props)
    }
}
