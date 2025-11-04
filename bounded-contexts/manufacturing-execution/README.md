# Manufacturing Execution Context

## 1. Purpose
1.1 Manage production planning, shop-floor execution, quality assurance, and asset maintenance.
1.2 Synchronize manufacturing orders with inventory availability, procurement, and demand forecasts.
1.3 Enforce compliance, traceability, and performance monitoring for multi-tenant manufacturing operations.

## 2. Module Overview
2.1 `manufacturing-production/` - Production orders, scheduling, routing, and execution tracking.
2.2 `manufacturing-quality/` - Quality inspections, non-conformance management, and corrective actions.
2.3 `manufacturing-maintenance/` - Preventive and corrective maintenance for production assets.
2.4 `manufacturing-shared/` - Shared manufacturing policies, BOM definitions, and performance indicators.

## 3. Integration Highlights
3.1 Consumes demand and work order triggers from Commerce, Procurement, and Operations Service.
3.2 Emits production completion and quality events to Inventory, Financial, and Business Intelligence contexts.
3.3 Collaborates with Corporate Services for maintenance planning and asset utilization.

## 4. Reference
4.1 `docs/ARCHITECTURE.md` (Manufacturing Execution) details process orchestration.
4.2 Delivery milestones live in `docs/ROADMAP.md` Phases 5–7.
4.3 Related ADRs capture manufacturing-specific decisions under `docs/adr/`.
