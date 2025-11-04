# Inventory Management Context

## 1. Purpose
1.1 Oversee stock levels, reservations, and warehouse execution across the supply chain.
1.2 Provide accurate inventory visibility, replenishment signals, and costing policies per tenant.
1.3 Coordinate inbound, outbound, and internal movements with procurement, manufacturing, and operations teams.

## 2. Module Overview
2.1 `inventory-stock/` - Stock ledger, reservations, and inventory valuation services.
2.2 `inventory-warehouse/` - Warehouse execution, picking, packing, and logistics orchestration.
2.3 `inventory-shared/` - Common inventory identifiers, measurement units, and business rules.

## 3. Integration Highlights
3.1 Subscribes to procurement receipts, production outputs, and returns to update stock.
3.2 Publishes availability and allocation events to Commerce, Procurement, and Operations Service.
3.3 Supplies inventory metrics and KPIs to Business Intelligence contexts.

## 4. Reference
4.1 Consult `docs/ARCHITECTURE.md` (Inventory Management) for process diagrams and data models.
4.2 `docs/ROADMAP.md` Phases 5–6 outline rollout sequencing and dependencies.
4.3 Shared warehouses and logistics policies are documented via ADRs in `docs/adr/`.
