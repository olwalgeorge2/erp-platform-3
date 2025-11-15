package com.erp.finance.accounting.application.port.output

import com.erp.finance.accounting.domain.model.AccountDimensionPolicy
import com.erp.finance.accounting.domain.model.AccountType
import com.erp.finance.accounting.domain.model.DimensionType
import java.util.UUID

interface DimensionPolicyRepository {
    fun findByTenant(tenantId: UUID): List<AccountDimensionPolicy>

    fun save(policy: AccountDimensionPolicy): AccountDimensionPolicy

    fun findByTenantAndAccountType(
        tenantId: UUID,
        accountType: AccountType,
    ): List<AccountDimensionPolicy>

    fun deleteByTenantAndDimension(
        tenantId: UUID,
        dimensionType: DimensionType,
        accountType: AccountType,
    )
}
