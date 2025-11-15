package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.output.DimensionPolicyRepository
import com.erp.finance.accounting.application.port.output.DimensionRepository
import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.DimensionRequirement
import com.erp.finance.accounting.domain.model.DimensionStatus
import com.erp.finance.accounting.domain.model.DimensionType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DimensionValidationServiceTest {
    private lateinit var policyRepository: FakeDimensionPolicyRepository
    private lateinit var dimensionRepository: FakeDimensionRepository
    private lateinit var service: DimensionValidationService
    private lateinit var meterRegistry: SimpleMeterRegistry
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        policyRepository = FakeDimensionPolicyRepository()
        dimensionRepository = FakeDimensionRepository()
        meterRegistry = SimpleMeterRegistry()
        service = DimensionValidationService(policyRepository, dimensionRepository, meterRegistry)
    }

    @Test
    fun `throws when mandatory dimension is missing`() {
        policyRepository.seed(
            AccountDimensionPolicy(
                tenantId = tenantId,
                accountType = AccountType.EXPENSE,
                dimensionType = DimensionType.COST_CENTER,
                requirement = DimensionRequirement.MANDATORY,
            ),
        )

        val lines =
            listOf(
                DimensionValidationService.DimensionValidationLine(
                    accountType = AccountType.EXPENSE,
                    dimensions = emptyMap(),
                ),
            )

        assertThrows(DimensionValidationException::class.java) {
            service.validateAssignments(tenantId, Instant.parse("2025-01-01T00:00:00Z"), lines)
        }
        assertEquals(
            1.0,
            meterRegistry
                .get("finance.dimension.validation.failures")
                .tags("reason", "MANDATORY_MISSING", "dimensionType", "COST_CENTER", "accountType", "EXPENSE")
                .counter()
                .count(),
        )
    }

    @Test
    fun `throws when dimension is not active on booking date`() {
        val dimensionId = UUID.randomUUID()
        policyRepository.seed(
            AccountDimensionPolicy(
                tenantId = tenantId,
                accountType = AccountType.REVENUE,
                dimensionType = DimensionType.COST_CENTER,
                requirement = DimensionRequirement.MANDATORY,
            ),
        )
        dimensionRepository.save(
            AccountingDimension(
                id = dimensionId,
                tenantId = tenantId,
                companyCodeId = UUID.randomUUID(),
                type = DimensionType.COST_CENTER,
                code = "6000",
                name = "Inactive",
                status = DimensionStatus.ACTIVE,
                validFrom = LocalDate.parse("2025-01-01"),
                validTo = LocalDate.parse("2025-12-31"),
            ),
        )

        val lines =
            listOf(
                DimensionValidationService.DimensionValidationLine(
                    accountType = AccountType.REVENUE,
                    dimensions = mapOf(DimensionType.COST_CENTER to dimensionId),
                ),
            )

        assertThrows(DimensionValidationException::class.java) {
            service.validateAssignments(tenantId, Instant.parse("2024-12-31T23:59:59Z"), lines)
        }
        assertEquals(
            1.0,
            meterRegistry
                .get("finance.dimension.validation.failures")
                .tags("reason", "INACTIVE", "dimensionType", "COST_CENTER", "accountType", "REVENUE")
                .counter()
                .count(),
        )
    }

    @Test
    fun `passes when optional dimension is omitted`() {
        policyRepository.seed(
            AccountDimensionPolicy(
                tenantId = tenantId,
                accountType = AccountType.ASSET,
                dimensionType = DimensionType.PROJECT,
                requirement = DimensionRequirement.OPTIONAL,
            ),
        )

        val lines =
            listOf(
                DimensionValidationService.DimensionValidationLine(
                    accountType = AccountType.ASSET,
                    dimensions = emptyMap(),
                ),
            )

        assertDoesNotThrow {
            service.validateAssignments(tenantId, Instant.parse("2025-01-10T00:00:00Z"), lines)
        }
        assertEquals(
            1.0,
            meterRegistry
                .get("finance.dimension.orphan_lines")
                .tags("accountType", "ASSET")
                .counter()
                .count(),
        )
    }

    private class FakeDimensionPolicyRepository : DimensionPolicyRepository {
        private val policies = mutableListOf<AccountDimensionPolicy>()

        fun seed(policy: AccountDimensionPolicy) {
            save(policy)
        }

        override fun findByTenant(tenantId: UUID): List<AccountDimensionPolicy> =
            policies.filter { it.tenantId == tenantId }

        override fun save(policy: AccountDimensionPolicy): AccountDimensionPolicy {
            policies.removeIf { it.id == policy.id }
            policies += policy
            return policy
        }

        override fun findByTenantAndAccountType(
            tenantId: UUID,
            accountType: AccountType,
        ): List<AccountDimensionPolicy> = policies.filter { it.tenantId == tenantId && it.accountType == accountType }

        override fun deleteByTenantAndDimension(
            tenantId: UUID,
            dimensionType: DimensionType,
            accountType: AccountType,
        ) {
            policies.removeIf {
                it.tenantId == tenantId && it.dimensionType == dimensionType && it.accountType == accountType
            }
        }
    }

    private class FakeDimensionRepository : DimensionRepository {
        private val store = mutableMapOf<Pair<DimensionType, UUID>, AccountingDimension>()

        override fun save(dimension: AccountingDimension): AccountingDimension {
            store[dimension.type to dimension.id] = dimension
            return dimension
        }

        override fun findById(
            type: DimensionType,
            tenantId: UUID,
            id: UUID,
        ): AccountingDimension? = store[type to id]?.takeIf { it.tenantId == tenantId }

        override fun findAll(
            type: DimensionType,
            tenantId: UUID,
            companyCodeId: UUID?,
            status: DimensionStatus?,
        ): List<AccountingDimension> =
            store.values.filter {
                it.type == type &&
                    it.tenantId == tenantId &&
                    (companyCodeId == null || it.companyCodeId == companyCodeId) &&
                    (status == null || it.status == status)
            }

        override fun findByIds(
            type: DimensionType,
            tenantId: UUID,
            ids: Set<UUID>,
        ): Map<UUID, AccountingDimension> =
            store
                .filter { it.key.first == type && ids.contains(it.key.second) && it.value.tenantId == tenantId }
                .map { entry -> entry.key.second to entry.value }
                .toMap()
    }
}
