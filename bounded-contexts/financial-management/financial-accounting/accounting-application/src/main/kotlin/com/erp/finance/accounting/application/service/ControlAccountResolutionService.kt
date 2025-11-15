package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.output.ControlAccountRepository
import com.erp.finance.accounting.domain.model.ControlAccountCategory
import com.erp.finance.accounting.domain.model.ControlAccountSubLedger
import com.erp.finance.accounting.domain.model.DimensionAssignments
import jakarta.enterprise.context.ApplicationScoped
import java.util.Locale
import java.util.UUID

@ApplicationScoped
class ControlAccountResolutionService(
    private val repository: ControlAccountRepository,
) {
    fun resolvePayablesAccount(
        tenantId: UUID,
        companyCodeId: UUID,
        dimensionAssignments: DimensionAssignments,
        currency: String,
    ): UUID =
        resolveAccount(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            subLedger = ControlAccountSubLedger.AP,
            category = ControlAccountCategory.PAYABLE,
            dimensionAssignments = dimensionAssignments,
            currency = currency,
        )

    fun resolveReceivablesAccount(
        tenantId: UUID,
        companyCodeId: UUID,
        dimensionAssignments: DimensionAssignments,
        currency: String,
    ): UUID =
        resolveAccount(
            tenantId = tenantId,
            companyCodeId = companyCodeId,
            subLedger = ControlAccountSubLedger.AR,
            category = ControlAccountCategory.RECEIVABLE,
            dimensionAssignments = dimensionAssignments,
            currency = currency,
        )

    private fun resolveAccount(
        tenantId: UUID,
        companyCodeId: UUID,
        subLedger: ControlAccountSubLedger,
        category: ControlAccountCategory,
        dimensionAssignments: DimensionAssignments,
        currency: String,
    ): UUID {
        val dimensionKeys = buildDimensionKeys(dimensionAssignments)
        val currencyPreferences = buildCurrencyPreferences(currency)
        dimensionKeys.forEach { dimensionKey ->
            currencyPreferences.forEach { currencyKey ->
                val config =
                    repository.findAccount(
                        tenantId = tenantId,
                        companyCodeId = companyCodeId,
                        subLedger = subLedger,
                        category = category,
                        dimensionKey = dimensionKey,
                        currency = currencyKey,
                    )
                if (config != null) {
                    return config.glAccountId
                }
            }
        }
        error(
            "Missing ${category.name} control account for $subLedger tenant=$tenantId companyCode=$companyCodeId currency=$currency",
        )
    }

    private fun buildDimensionKeys(assignments: DimensionAssignments): List<String> {
        val segments =
            listOfNotNull(
                assignments.costCenterId?.let { "COSTCENTER:$it" },
                assignments.profitCenterId?.let { "PROFITCENTER:$it" },
                assignments.departmentId?.let { "DEPARTMENT:$it" },
                assignments.projectId?.let { "PROJECT:$it" },
                assignments.businessAreaId?.let { "BUSINESSAREA:$it" },
            )
        if (segments.isEmpty()) {
            return listOf(DEFAULT_DIMENSION_KEY)
        }
        val keys = linkedSetOf(segments.joinToString("|") { it.uppercase(Locale.ROOT) })
        keys.add(DEFAULT_DIMENSION_KEY)
        return keys.toList()
    }

    private fun buildCurrencyPreferences(currency: String): List<String> {
        val upper = currency.uppercase(Locale.ROOT)
        return listOf(upper, ANY_CURRENCY_KEY).distinct()
    }

    companion object {
        const val DEFAULT_DIMENSION_KEY = "DEFAULT"
        const val ANY_CURRENCY_KEY = "ANY"
    }
}
