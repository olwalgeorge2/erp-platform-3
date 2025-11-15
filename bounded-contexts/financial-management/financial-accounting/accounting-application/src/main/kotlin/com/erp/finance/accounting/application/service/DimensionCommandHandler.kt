package com.erp.finance.accounting.application.service

import com.erp.finance.accounting.application.port.input.DimensionCommandUseCase
import com.erp.finance.accounting.application.port.input.command.dimension.AssignFiscalYearVariantCommand
import com.erp.finance.accounting.application.port.input.command.dimension.CreateCompanyCodeCommand
import com.erp.finance.accounting.application.port.input.command.dimension.CreateFiscalYearVariantCommand
import com.erp.finance.accounting.application.port.input.command.dimension.DimensionQuery
import com.erp.finance.accounting.application.port.input.command.dimension.LinkLedgerToCompanyCodeCommand
import com.erp.finance.accounting.application.port.input.command.dimension.SchedulePeriodBlackoutCommand
import com.erp.finance.accounting.application.port.input.command.dimension.UpsertDimensionCommand
import com.erp.finance.accounting.application.port.input.command.dimension.UpsertDimensionPolicyCommand
import com.erp.finance.accounting.application.port.output.CompanyCodeLedgerRepository
import com.erp.finance.accounting.application.port.output.CompanyCodeRepository
import com.erp.finance.accounting.application.port.output.DimensionPolicyRepository
import com.erp.finance.accounting.application.port.output.DimensionRepository
import com.erp.finance.accounting.application.port.output.FinanceEventPublisher
import com.erp.finance.accounting.application.port.output.FiscalYearVariantRepository
import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountingDimension
import com.erp.finance.accounting.domain.model.CompanyCode
import com.erp.finance.accounting.domain.model.CompanyCodeFiscalYearVariant
import com.erp.finance.accounting.domain.model.DimensionEventAction
import com.erp.finance.accounting.domain.model.FiscalYearVariant
import com.erp.finance.accounting.domain.model.FiscalYearVariantPeriod
import com.erp.finance.accounting.domain.model.PeriodBlackout
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.UUID

@ApplicationScoped
class DimensionCommandHandler
    @Inject
    constructor(
        private val companyCodeRepository: CompanyCodeRepository,
        private val dimensionRepository: DimensionRepository,
        private val fiscalYearVariantRepository: FiscalYearVariantRepository,
        private val dimensionPolicyRepository: DimensionPolicyRepository,
        private val eventPublisher: FinanceEventPublisher,
        private val companyCodeLedgerRepository: CompanyCodeLedgerRepository,
    ) : DimensionCommandUseCase {
        override fun createCompanyCode(command: CreateCompanyCodeCommand): CompanyCode {
            val companyCode =
                CompanyCode(
                    tenantId = command.tenantId,
                    code = command.code,
                    name = command.name,
                    legalEntityName = command.legalEntityName,
                    countryCode = command.countryCode.uppercase(),
                    baseCurrency = command.baseCurrency.uppercase(),
                    timezone = command.timezone,
                    status = command.status,
                )
            return companyCodeRepository.save(companyCode)
        }

        override fun listCompanyCodes(tenantId: UUID): List<CompanyCode> = companyCodeRepository.findByTenant(tenantId)

        override fun upsertDimension(command: UpsertDimensionCommand): AccountingDimension {
            val dimension =
                AccountingDimension(
                    id = command.dimensionId ?: UUID.randomUUID(),
                    tenantId = command.tenantId,
                    companyCodeId = command.companyCodeId,
                    type = command.type,
                    code = command.code,
                    name = command.name,
                    description = command.description,
                    parentId = command.parentId,
                    status = command.status,
                    validFrom = command.validFrom,
                    validTo = command.validTo,
                )
            val existing =
                command.dimensionId?.let {
                    dimensionRepository.findById(command.type, command.tenantId, it)
                }

            val saved = dimensionRepository.save(dimension)
            val action =
                when {
                    existing == null && saved.status.name == "ACTIVE" -> DimensionEventAction.CREATED
                    saved.status.name == "RETIRED" -> DimensionEventAction.RETIRED
                    else -> DimensionEventAction.UPDATED
                }
            eventPublisher.publishDimensionChanged(saved, action)
            return saved
        }

        override fun listDimensions(query: DimensionQuery): List<AccountingDimension> =
            dimensionRepository.findAll(
                type = query.type,
                tenantId = query.tenantId,
                companyCodeId = query.companyCodeId,
                status = query.status,
            )

        override fun createFiscalYearVariant(command: CreateFiscalYearVariantCommand): FiscalYearVariant {
            val variant =
                FiscalYearVariant(
                    tenantId = command.tenantId,
                    code = command.code,
                    name = command.name,
                    description = command.description,
                    startMonth = command.startMonth,
                    calendarPattern = command.calendarPattern,
                    periods =
                        command.periods.map {
                            FiscalYearVariantPeriod(
                                periodNumber = it.periodNumber,
                                label = it.label,
                                lengthInDays = it.lengthInDays,
                                adjustment = it.adjustment,
                            )
                        },
                )
            return fiscalYearVariantRepository.save(variant)
        }

        override fun assignFiscalYearVariant(command: AssignFiscalYearVariantCommand): CompanyCodeFiscalYearVariant {
            val assignment =
                CompanyCodeFiscalYearVariant(
                    companyCodeId = command.companyCodeId,
                    fiscalYearVariantId = command.fiscalYearVariantId,
                    effectiveFrom = command.effectiveFrom,
                    effectiveTo = command.effectiveTo,
                )
            return fiscalYearVariantRepository.assignToCompanyCode(assignment)
        }

        override fun schedulePeriodBlackout(command: SchedulePeriodBlackoutCommand): PeriodBlackout {
            val blackout =
                PeriodBlackout(
                    companyCodeId = command.companyCodeId,
                    periodCode = command.periodCode,
                    blackoutStart = command.blackoutStart,
                    blackoutEnd = command.blackoutEnd,
                    status = command.status,
                    reason = command.reason,
                )
            return fiscalYearVariantRepository.scheduleBlackout(blackout)
        }

        override fun upsertPolicy(command: UpsertDimensionPolicyCommand): AccountDimensionPolicy {
            dimensionPolicyRepository.deleteByTenantAndDimension(
                tenantId = command.tenantId,
                dimensionType = command.dimensionType,
                accountType = command.accountType,
            )
            val policy =
                AccountDimensionPolicy(
                    tenantId = command.tenantId,
                    accountType = command.accountType,
                    dimensionType = command.dimensionType,
                    requirement = command.requirement,
                )
            return dimensionPolicyRepository.save(policy)
        }

        override fun listPolicies(tenantId: UUID): List<AccountDimensionPolicy> =
            dimensionPolicyRepository.findByTenant(tenantId)

        override fun linkLedgerToCompanyCode(command: LinkLedgerToCompanyCodeCommand) {
            companyCodeLedgerRepository.linkLedger(command.companyCodeId, command.ledgerId)
        }
    }
