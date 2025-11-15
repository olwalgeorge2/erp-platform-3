package com.erp.finance.accounting.infrastructure.adapter.input.rest

import com.erp.finance.accounting.application.port.input.command.CreateLedgerCommand
import com.erp.finance.accounting.application.port.input.command.DefineAccountCommand
import com.erp.finance.accounting.application.port.input.command.JournalEntryLineCommand
import com.erp.finance.accounting.application.port.input.command.PostJournalEntryCommand
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.AccountingPeriod
import com.erp.finance.accounting.domain.model.AccountingPeriodStatus
import com.erp.finance.accounting.domain.model.DimensionAssignments
import com.erp.finance.accounting.domain.model.EntryDirection
import com.erp.finance.accounting.domain.model.Money
import com.erp.finance.accounting.infrastructure.FinanceKafkaTestResource
import com.erp.finance.accounting.infrastructure.FinancePostgresTestResource
import com.erp.finance.accounting.infrastructure.outbox.FinanceOutboxEventScheduler
import com.erp.finance.accounting.infrastructure.service.FinanceCommandService
import com.erp.finance.accounting.infrastructure.support.AccountingPeriodTestSupport
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
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
class FinanceDimensionIntegrationTest {
    @Test
    fun `dimension lifecycle emits kafka payload`() {
        val tenantId = UUID.randomUUID()
        val companyCodeId = createCompanyCode(tenantId)
        val costCenterId = createCostCenter(tenantId, companyCodeId)

        RestAssured
            .given()
            .queryParam("tenantId", tenantId)
            .queryParam("companyCodeId", companyCodeId)
            .queryParam("status", "ACTIVE")
            .get("/api/v1/finance/dimensions/cost-centers")
            .then()
            .statusCode(200)
            .body("$", hasSize<Any>(1))
            .body("[0].id", equalTo(costCenterId.toString()))

        val dimensionMessages = readKafkaMessages("finance.dimensions.events.v1")
        assertTrue(
            dimensionMessages.any { it.contains("\"dimensionId\":\"$costCenterId\"") },
            "Expected dimension event to reference cost center $costCenterId",
        )
    }

    @Test
    fun `trial balance endpoint aggregates journal entries`() {
        val tenantId = UUID.randomUUID()
        val companyCodeId = createCompanyCode(tenantId)
        val costCenterId = createCostCenter(tenantId, companyCodeId)

        val chartId = UUID.randomUUID()
        val ledger =
            financeCommandService.createLedger(
                CreateLedgerCommand(
                    tenantId = tenantId,
                    chartOfAccountsId = chartId,
                    baseCurrency = "USD",
                    chartCode = "TRIAL-${chartId.toString().take(6)}",
                    chartName = "Trial Balance Chart",
                ),
            )

        val assetChart =
            financeCommandService.defineAccount(
                DefineAccountCommand(
                    tenantId = tenantId,
                    chartOfAccountsId = chartId,
                    code = "1000",
                    name = "Cash",
                    type = AccountType.ASSET,
                    currency = "USD",
                ),
            )
        val revenueChart =
            financeCommandService.defineAccount(
                DefineAccountCommand(
                    tenantId = tenantId,
                    chartOfAccountsId = chartId,
                    code = "4000",
                    name = "Services Revenue",
                    type = AccountType.REVENUE,
                    currency = "USD",
                ),
            )

        val assetAccount =
            assetChart.accounts.values
                .first { it.code == "1000" }
                .id
        val revenueAccount =
            revenueChart.accounts.values
                .first { it.code == "4000" }
                .id

        val period =
            AccountingPeriod(
                ledgerId = ledger.id,
                tenantId = tenantId,
                code = "2025-01",
                startDate = LocalDate.parse("2025-01-01"),
                endDate = LocalDate.parse("2025-01-31"),
                status = AccountingPeriodStatus.OPEN,
            )
        periodSeeder.save(period)

        financeCommandService.postJournalEntry(
            PostJournalEntryCommand(
                tenantId = tenantId,
                ledgerId = ledger.id.value,
                accountingPeriodId = period.id.value,
                reference = "TB-001",
                description = "Trial balance entry",
                lines =
                    listOf(
                        JournalEntryLineCommand(
                            accountId = assetAccount,
                            direction = EntryDirection.DEBIT,
                            amount = Money(15_000),
                            currency = "USD",
                            dimensions = DimensionAssignments(costCenterId = costCenterId),
                        ),
                        JournalEntryLineCommand(
                            accountId = revenueAccount,
                            direction = EntryDirection.CREDIT,
                            amount = Money(15_000),
                            currency = "USD",
                            dimensions = DimensionAssignments(costCenterId = costCenterId),
                        ),
                    ),
            ),
        )

        RestAssured
            .given()
            .queryParam("tenantId", tenantId)
            .queryParam("accountingPeriodId", period.id.value)
            .queryParam("dimensionFilter", "COST_CENTER:$costCenterId")
            .get("/api/v1/finance/ledgers/${ledger.id.value}/trial-balance")
            .then()
            .statusCode(200)
            .body("currency", equalTo("USD"))
            .body("lines.size()", equalTo(2))
            .body("lines.find { it.accountCode == '1000' }.debitTotalMinor", equalTo(15_000))
            .body("lines.find { it.accountCode == '4000' }.creditTotalMinor", equalTo(15_000))
    }

    private fun createCompanyCode(tenantId: UUID): UUID =
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "code": "CC-${tenantId.toString().take(6)}",
                  "name": "Integration Co",
                  "legalEntityName": "Integration Company LLC",
                  "countryCode": "US",
                  "baseCurrency": "USD",
                  "timezone": "America/Chicago",
                  "status": "ACTIVE"
                }
                """.trimIndent(),
            ).post("/api/v1/finance/company-codes")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .jsonPath()
            .getString("id")
            .let(UUID::fromString)

    private fun createCostCenter(
        tenantId: UUID,
        companyCodeId: UUID,
    ): UUID =
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "tenantId": "$tenantId",
                  "companyCodeId": "$companyCodeId",
                  "code": "6000",
                  "name": "Integration Cost Center",
                  "status": "ACTIVE",
                  "validFrom": "2025-01-01"
                }
                """.trimIndent(),
            ).post("/api/v1/finance/dimensions/cost-centers")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract()
            .jsonPath()
            .getString("id")
            .let(UUID::fromString)

    private fun readKafkaMessages(topic: String): List<String> {
        val bootstrap = System.getProperty("KAFKA_BOOTSTRAP_SERVERS") ?: return emptyList()
        val props =
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.GROUP_ID_CONFIG, "finance-dimension-integration-${System.nanoTime()}")
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            }
        KafkaConsumer<String, String>(props).use { consumer ->
            consumer.subscribe(listOf(topic))
            scheduler.publishPendingEvents()
            val records = consumer.poll(Duration.ofSeconds(5))
            return records.map { it.value() }
        }
    }

    companion object {
        init {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }

    @jakarta.inject.Inject
    lateinit var financeCommandService: FinanceCommandService

    @jakarta.inject.Inject
    lateinit var periodSeeder: AccountingPeriodTestSupport

    @jakarta.inject.Inject
    lateinit var scheduler: FinanceOutboxEventScheduler
}
