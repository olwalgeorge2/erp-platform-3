package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.output.DimensionPolicyRepository
import com.erp.finance.accounting.application.port.output.DimensionRepository
import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionRequirement
import com.erp.finance.accounting.domain.model.DimensionType
import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@ApplicationScoped
class DimensionValidationService
    @Inject
    constructor(
        private val dimensionPolicyRepository: DimensionPolicyRepository,
        private val dimensionRepository: DimensionRepository,
        private val meterRegistry: MeterRegistry,
    ) : DimensionAssignmentValidator {
        fun ensurePolicies(tenantId: UUID): List<AccountDimensionPolicy> {
            val existing = dimensionPolicyRepository.findByTenant(tenantId)
            if (existing.isNotEmpty()) {
                return existing
            }
            val defaults = defaultPolicies(tenantId)
            return defaults.map(dimensionPolicyRepository::save)
        }

        override fun validateAssignments(
            tenantId: UUID,
            bookedAt: Instant,
            lines: List<DimensionValidationLine>,
        ) {
            val policyMap =
                ensurePolicies(tenantId)
                    .groupBy { it.accountType }
                    .mapValues { (_, policies) ->
                        policies.associate { it.dimensionType to it.requirement }
                    }

            val bookingDate = bookedAt.atZone(ZoneOffset.UTC).toLocalDate()
            lines.filter { it.dimensions.isEmpty() }.forEach { recordOrphanLine(it.accountType) }

            DimensionType.values().forEach { type ->
                val ids =
                    lines
                        .mapNotNull { it.dimensions[type] }
                        .toSet()
                if (ids.isEmpty()) {
                    return@forEach
                }
                val resolved = dimensionRepository.findByIds(type, tenantId, ids)
                lines.forEach { line ->
                    val dimensionId = line.dimensions[type] ?: return@forEach
                    val dimension =
                        resolved[dimensionId]
                            ?: run {
                                recordValidationFailure("NOT_FOUND", type, line.accountType)
                                error("Dimension $dimensionId for $type not found for tenant $tenantId")
                            }
                    if (!dimension.isActive(bookingDate)) {
                        recordValidationFailure("INACTIVE", type, line.accountType)
                        error("Dimension ${dimension.code} ($type) is not active on $bookingDate")
                    }
                }
            }

            lines.forEach { line ->
                val requirements = policyMap[line.accountType] ?: emptyMap()
                requirements.forEach { (dimensionType, requirement) ->
                    if (requirement == DimensionRequirement.MANDATORY && !line.dimensions.containsKey(dimensionType)) {
                        recordValidationFailure("MANDATORY_MISSING", dimensionType, line.accountType)
                        error("Dimension ${dimensionType.name} is mandatory for ${line.accountType}")
                    }
                }
            }
        }

        private fun defaultPolicies(tenantId: UUID): List<AccountDimensionPolicy> {
            val dimensions =
                listOf(
                    DimensionType.COST_CENTER,
                    DimensionType.PROFIT_CENTER,
                    DimensionType.DEPARTMENT,
                    DimensionType.PROJECT,
                    DimensionType.BUSINESS_AREA,
                )
            val accountTypes = AccountType.values()
            val policies = mutableListOf<AccountDimensionPolicy>()
            accountTypes.forEach { accountType ->
                dimensions.forEach { dimensionType ->
                    val requirement =
                        when (accountType) {
                            AccountType.EXPENSE,
                            AccountType.REVENUE,
                            ->
                                if (dimensionType ==
                                    DimensionType.COST_CENTER
                                ) {
                                    DimensionRequirement.MANDATORY
                                } else {
                                    DimensionRequirement.OPTIONAL
                                }
                            else -> DimensionRequirement.OPTIONAL
                        }
                    policies +=
                        AccountDimensionPolicy(
                            tenantId = tenantId,
                            accountType = accountType,
                            dimensionType = dimensionType,
                            requirement = requirement,
                        )
                }
            }
            return policies
        }

        data class DimensionValidationLine(
            val accountType: AccountType,
            val dimensions: Map<DimensionType, UUID>,
        )

        private fun recordValidationFailure(
            reason: String,
            dimensionType: DimensionType,
            accountType: AccountType,
        ) {
            meterRegistry
                .counter(
                    "finance.dimension.validation.failures",
                    "reason",
                    reason,
                    "dimensionType",
                    dimensionType.name,
                    "accountType",
                    accountType.name,
                ).increment()
        }

        private fun recordOrphanLine(accountType: AccountType) {
            meterRegistry.counter("finance.dimension.orphan_lines", "accountType", accountType.name).increment()
        }
    }
