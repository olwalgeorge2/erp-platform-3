# Corporate Assets Service

## 1. Purpose
1.1 Manage the lifecycle of fixed assets, equipment, and capital investments.
1.2 Track acquisition, depreciation, assignment, and disposal events across tenants.
1.3 Provide visibility into asset utilization for finance, operations, and maintenance teams.

## 2. Module Structure
2.1 `assets-application/` - Coordinates asset workflows, depreciation schedules, and approvals.
2.2 `assets-domain/` - Defines asset aggregates, lifecycle states, and valuation policies.
2.3 `assets-infrastructure/` - Connects to asset registries, financial systems, and inventory references.

## 3. Domain Highlights
3.1 Supports multi-ledger depreciation methods aligned with Financial Management.
3.2 Links assets to maintenance plans and work orders from Operations Service and Manufacturing.
3.3 Maintains compliance audit trails for regulatory and tax reporting.

## 4. Integration
4.1 Publishes asset capitalization and disposal events to Financial Management.
4.2 Consumes procurement data for acquisitions and inventory updates for spare parts usage.
4.3 Shares asset metadata with Business Intelligence for utilization analytics.
