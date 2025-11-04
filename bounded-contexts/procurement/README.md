# Procurement Context

## 1. Purpose
1.1 Manage sourcing, supplier relationships, and purchasing across the supply chain.
1.2 Enable procurement teams to coordinate requisitions, approvals, and contracts governed by tenant policies.
1.3 Integrate purchasing flows with inventory, financial, and manufacturing processes to maintain continuity.

## 2. Module Overview
2.1 `procurement-purchasing/` - Requisitions, purchase orders, receiving, and invoice matching.
2.2 `procurement-sourcing/` - Supplier onboarding, RFQs, bidding, and contract administration.
2.3 `procurement-shared/` - Shared procurement attributes, supplier classifications, and compliance rules.

## 3. Integration Highlights
3.1 Interfaces with Inventory Management for replenishment signals and receiving confirmations.
3.2 Publishes financial events to Accounts Payable and General Ledger.
3.3 Shares supplier performance metrics with Business Intelligence for spend analytics.

## 4. Reference
4.1 `docs/ARCHITECTURE.md` (Procurement) provides interaction diagrams and domain boundaries.
4.2 Implementation phases are listed in `docs/ROADMAP.md` Phases 5–6.
4.3 Supplier compliance ADRs are cataloged under `docs/adr/`.
