package com.erp.finance.accounting.infrastructure.persistence.entity

import com.erp.finance.accounting.domain.model.DimensionType
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "cost_centers", schema = "financial_accounting")
class CostCenterEntity : BaseDimensionEntity() {
    override val type: DimensionType = DimensionType.COST_CENTER
}

@Entity
@Table(name = "profit_centers", schema = "financial_accounting")
class ProfitCenterEntity : BaseDimensionEntity() {
    override val type: DimensionType = DimensionType.PROFIT_CENTER
}

@Entity
@Table(name = "departments", schema = "financial_accounting")
class DepartmentEntity : BaseDimensionEntity() {
    override val type: DimensionType = DimensionType.DEPARTMENT
}

@Entity
@Table(name = "projects", schema = "financial_accounting")
class ProjectEntity : BaseDimensionEntity() {
    override val type: DimensionType = DimensionType.PROJECT
}

@Entity
@Table(name = "business_areas", schema = "financial_accounting")
class BusinessAreaEntity : BaseDimensionEntity() {
    override val type: DimensionType = DimensionType.BUSINESS_AREA
}
