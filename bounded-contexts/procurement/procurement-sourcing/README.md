# Procurement Sourcing Service

## 1. Purpose
1.1 Manage supplier sourcing events, RFQs, bidding, and contract lifecycle management.
1.2 Qualify suppliers, assess risk, and maintain compliant vendor master data.
1.3 Provide insights into sourcing performance, savings, and supplier diversity goals.

## 2. Module Structure
2.1 `sourcing-application/` - Orchestrates RFQs, bid evaluations, and contract workflows.
2.2 `sourcing-domain/` - Encapsulates supplier profiles, sourcing events, scorecards, and contracts.
2.3 `sourcing-infrastructure/` - Integrates with supplier portals, compliance services, and document management.

## 3. Domain Highlights
3.1 Applies supplier risk scoring and compliance checks leveraging Tenancy & Identity policies.
3.2 Shares supplier qualifications and performance metrics with Purchasing and Financial Management.
3.3 Supports collaborative sourcing with audit trails and approval checkpoints.

## 4. Integration
4.1 Publishes awarded supplier events to Purchasing and Inventory for execution.
4.2 Consumes demand signals from Manufacturing, Operations, and Commerce contexts.
4.3 Provides sourcing analytics to Business Intelligence and executive dashboards.
