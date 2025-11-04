# Procurement Purchasing Service

## 1. Purpose
1.1 Manage requisitions, purchase orders, receiving, and invoice matching activities.
1.2 Enforce approval policies, budget controls, and supplier compliance requirements.
1.3 Provide visibility into spend, delivery performance, and replenishment needs.

## 2. Module Structure
2.1 `purchasing-application/` - Handles requisition workflows, PO issuance, and receiving commands.
2.2 `purchasing-domain/` - Models requisitions, POs, receipts, and matching policies.
2.3 `purchasing-infrastructure/` - Integrates with supplier networks, EDI, and receiving systems.

## 3. Domain Highlights
3.1 Supports multi-stage approvals with thresholds driven by Tenancy & Identity roles.
3.2 Aligns receipts and invoice matching with Accounts Payable and Inventory Stock modules.
3.3 Tracks supplier performance metrics and contract compliance.

## 4. Integration
4.1 Consumes requisition demand from Commerce, Manufacturing, and Operations contexts.
4.2 Publishes receiving and invoice matching events to Financial Management and Inventory.
4.3 Provides spend and supplier KPIs to Business Intelligence.
