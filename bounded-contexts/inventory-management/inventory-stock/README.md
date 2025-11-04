# Inventory Stock Service

## 1. Purpose
1.1 Maintain the authoritative stock ledger, reservations, and valuation logic.
1.2 Provide real-time availability and allocation capabilities across channels and warehouses.
1.3 Support replenishment policies and safety stock analytics for supply planning.

## 2. Module Structure
2.1 `stock-application/` - Processes stock adjustments, reservation commands, and availability queries.
2.2 `stock-domain/` - Defines stock aggregates, location balances, and allocation policies.
2.3 `stock-infrastructure/` - Persists stock data and integrates with event streams and external WMS/OMS systems.

## 3. Domain Highlights
3.1 Handles multi-location, serialized, and lot-tracked inventory scenarios.
3.2 Enforces reservation expiration and backorder logic shared with Commerce modules.
3.3 Provides valuation snapshots for Financial Management and Business Intelligence.

## 4. Integration
4.1 Consumes receipts from Procurement, production outputs from Manufacturing, and returns from Operations.
4.2 Publishes inventory availability and adjustment events to Commerce and Procurement contexts.
4.3 Exposes stock inquiry APIs through the API gateway for upstream consumers.
