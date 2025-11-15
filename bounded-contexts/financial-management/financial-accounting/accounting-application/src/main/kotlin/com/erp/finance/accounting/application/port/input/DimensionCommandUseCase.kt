package com.erp.finance.accounting.application.port.input

import com.erp.finance.accounting.application.port.input.command.dimension.AssignFiscalYearVariantCommand
import com.erp.finance.accounting.application.port.input.command.dimension.CreateCompanyCodeCommand
import com.erp.finance.accounting.application.port.input.command.dimension.CreateFiscalYearVariantCommand
import com.erp.finance.accounting.application.port.input.command.dimension.DimensionQuery
import com.erp.finance.accounting.application.port.input.command.dimension.LinkLedgerToCompanyCodeCommand
import com.erp.finance.accounting.application.port.input.command.dimension.SchedulePeriodBlackoutCommand
import com.erp.finance.accounting.application.port.input.command.dimension.UpsertDimensionCommand
import com.erp.finance.accounting.application.port.input.command.dimension.UpsertDimensionPolicyCommand
import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.CompanyCode
import com.erp.finance.accounting.domain.model.CompanyCodeFiscalYearVariant
import com.erp.finance.accounting.domain.model.FiscalYearVariant
import com.erp.finance.accounting.domain.model.PeriodBlackout
import java.util.UUID

interface DimensionCommandUseCase {
    fun createCompanyCode(command: CreateCompanyCodeCommand): CompanyCode

    fun listCompanyCodes(tenantId: UUID): List<CompanyCode>

    fun upsertDimension(command: UpsertDimensionCommand): AccountingDimension

    fun listDimensions(query: DimensionQuery): List<AccountingDimension>

    fun createFiscalYearVariant(command: CreateFiscalYearVariantCommand): FiscalYearVariant

    fun assignFiscalYearVariant(command: AssignFiscalYearVariantCommand): CompanyCodeFiscalYearVariant

    fun schedulePeriodBlackout(command: SchedulePeriodBlackoutCommand): PeriodBlackout

    fun upsertPolicy(command: UpsertDimensionPolicyCommand): AccountDimensionPolicy

    fun listPolicies(tenantId: UUID): List<AccountDimensionPolicy>

    fun linkLedgerToCompanyCode(command: LinkLedgerToCompanyCodeCommand)
}
