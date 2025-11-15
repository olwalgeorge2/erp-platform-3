package com.erp.finance.accounting.infrastructure.adapter.output.persistence

import com.erp.finance.accounting.application.port.output.DimensionSummaryRow
import com.erp.finance.accounting.application.port.output.TrialBalanceRepository
import com.erp.finance.accounting.application.port.output.TrialBalanceRow
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionType
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaTrialBalanceRepository(
    private val entityManager: EntityManager,
) : TrialBalanceRepository {
    override fun fetchTrialBalance(
        tenantId: UUID,
        ledgerId: UUID,
        accountingPeriodId: UUID,
        dimensionFilters: Map<DimensionType, UUID>,
    ): List<TrialBalanceRow> {
        val sql =
            StringBuilder(
                """
                SELECT jel.account_id, acc.code, acc.name, acc.account_type,
                       SUM(jel.debit_amount) AS debit_total,
                       SUM(jel.credit_amount) AS credit_total
                FROM financial_accounting.journal_entry_lines jel
                JOIN financial_accounting.journal_entries je ON je.id = jel.journal_entry_id
                JOIN financial_accounting.accounts acc ON acc.id = jel.account_id
                WHERE je.tenant_id = :tenantId
                  AND je.ledger_id = :ledgerId
                  AND je.accounting_period_id = :periodId
                  AND je.status = 'POSTED'
                """.trimIndent(),
            )

        dimensionFilters.forEach { (type, _) ->
            when (type) {
                DimensionType.COST_CENTER -> sql.append(" AND jel.cost_center_id = :costCenterId")
                DimensionType.PROFIT_CENTER -> sql.append(" AND jel.profit_center_id = :profitCenterId")
                DimensionType.DEPARTMENT -> sql.append(" AND jel.department_id = :departmentId")
                DimensionType.PROJECT -> sql.append(" AND jel.project_id = :projectId")
                DimensionType.BUSINESS_AREA -> sql.append(" AND jel.business_area_id = :businessAreaId")
            }
        }

        sql.append(
            """
            GROUP BY jel.account_id, acc.code, acc.name, acc.account_type
            ORDER BY acc.code
            """.trimIndent(),
        )

        val query = entityManager.createNativeQuery(sql.toString())
        query.setParameter("tenantId", tenantId)
        query.setParameter("ledgerId", ledgerId)
        query.setParameter("periodId", accountingPeriodId)
        dimensionFilters.forEach { (type, id) ->
            when (type) {
                DimensionType.COST_CENTER -> query.setParameter("costCenterId", id)
                DimensionType.PROFIT_CENTER -> query.setParameter("profitCenterId", id)
                DimensionType.DEPARTMENT -> query.setParameter("departmentId", id)
                DimensionType.PROJECT -> query.setParameter("projectId", id)
                DimensionType.BUSINESS_AREA -> query.setParameter("businessAreaId", id)
            }
        }

        @Suppress("UNCHECKED_CAST")
        val rows = query.resultList as List<Array<Any?>>
        return rows.map { columns ->
            TrialBalanceRow(
                accountId = columns[0] as UUID,
                accountCode = columns[1] as String,
                accountName = columns[2] as String,
                accountType = AccountType.valueOf(columns[3] as String),
                debitTotalMinor = (columns[4] as Number).toLong(),
                creditTotalMinor = (columns[5] as Number).toLong(),
            )
        }
    }

    override fun fetchSummaryByDimension(
        tenantId: UUID,
        ledgerId: UUID,
        accountingPeriodId: UUID,
        dimensionType: DimensionType,
    ): List<DimensionSummaryRow> {
        val column =
            when (dimensionType) {
                DimensionType.COST_CENTER -> "cost_center_id"
                DimensionType.PROFIT_CENTER -> "profit_center_id"
                DimensionType.DEPARTMENT -> "department_id"
                DimensionType.PROJECT -> "project_id"
                DimensionType.BUSINESS_AREA -> "business_area_id"
            }

        val table =
            when (dimensionType) {
                DimensionType.COST_CENTER -> "cost_centers"
                DimensionType.PROFIT_CENTER -> "profit_centers"
                DimensionType.DEPARTMENT -> "departments"
                DimensionType.PROJECT -> "projects"
                DimensionType.BUSINESS_AREA -> "business_areas"
            }

        val sql =
            """
            SELECT jel.$column,
                   dim.code,
                   dim.name,
                   SUM(jel.debit_amount) AS debit_total,
                   SUM(jel.credit_amount) AS credit_total
            FROM financial_accounting.journal_entry_lines jel
            JOIN financial_accounting.journal_entries je ON je.id = jel.journal_entry_id
            JOIN financial_accounting.$table dim ON dim.id = jel.$column
            WHERE je.tenant_id = :tenantId
              AND je.ledger_id = :ledgerId
              AND je.accounting_period_id = :periodId
              AND je.status = 'POSTED'
            GROUP BY jel.$column, dim.code, dim.name
            ORDER BY dim.code
            """.trimIndent()

        val query = entityManager.createNativeQuery(sql)
        query.setParameter("tenantId", tenantId)
        query.setParameter("ledgerId", ledgerId)
        query.setParameter("periodId", accountingPeriodId)

        @Suppress("UNCHECKED_CAST")
        val rows = query.resultList as List<Array<Any?>>
        return rows.map { columns ->
            DimensionSummaryRow(
                dimensionId = columns[0] as UUID,
                dimensionCode = columns[1] as String,
                dimensionName = columns[2] as String,
                debitTotalMinor = (columns[3] as Number).toLong(),
                creditTotalMinor = (columns[4] as Number).toLong(),
            )
        }
    }
}
