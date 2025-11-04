# Commerce B2B Service

## 1. Purpose
1.1 Deliver enterprise purchasing workflows including negotiated pricing, approvals, and contract terms.
1.2 Support account-specific catalogs, punchout integrations, and bulk order management.
1.3 Provide governance for multi-level buyer hierarchies and entitlement rules.

## 2. Module Structure
2.1 `b2b-application/` - Coordinates command/query handlers for B2B ordering, approvals, and contract operations.
2.2 `b2b-domain/` - Encapsulates buyer accounts, agreements, and approval rule aggregates.
2.3 `b2b-infrastructure/` - Persists B2B entities and integrates with ERP and external procurement systems.

## 3. Domain Highlights
3.1 Supports delegated administration and role-based limits aligned with Tenancy & Identity policies.
3.2 Maintains audit trails for compliance and exports purchasing data to Financial Management.
3.3 Shares catalog and pricing dependencies through the `commerce-shared` modules.

## 4. Integration
4.1 Consumes product, pricing, and availability feeds from Inventory and Manufacturing contexts.
4.2 Publishes order and contract events to Procurement, Financial Management, and Business Intelligence.
4.3 Exposes APIs via the API gateway for partner portals and customer procurement systems.
