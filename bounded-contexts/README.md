# Bounded Contexts Guide

## 1. Purpose
This document enumerates the microservices that make up the ERP platform, organized by bounded context. Each context owns its domain model, data storage, and deployment lifecycle while aligning with shared platform conventions.

## 2. Context Catalog
2.1 Business Intelligence
- 2.1.1 `bi-application/`: Analytics use cases and orchestration.
- 2.1.2 `bi-domain/`: Core analytics entities and aggregates.
- 2.1.3 `bi-infrastructure/`: Data warehouse and reporting adapters.

2.2 Commerce
- 2.2.1 `commerce-b2b/`: Business-to-business sales workflows.
- 2.2.2 `commerce-ecommerce/`: Online retail storefront services.
- 2.2.3 `commerce-marketplace/`: Third-party marketplace integrations.
- 2.2.4 `commerce-pos/`: Point-of-sale operations.
- 2.2.5 `commerce-shared/`: Shared commerce domain contracts.

2.3 Communication Hub
- 2.3.1 `communication-application/`: Message orchestration and delivery rules.
- 2.3.2 `communication-domain/`: Templates, channels, and communication entities.
- 2.3.3 `communication-infrastructure/`: Email, SMS, and push provider adapters.

2.4 Corporate Services
- 2.4.1 `corporate-assets/`: Enterprise asset lifecycle management.
- 2.4.2 `corporate-hr/`: Human resources administration and employee records.
- 2.4.3 `corporate-shared/`: Reusable corporate domain components.

2.5 Customer Relation
- 2.5.1 `customer-campaigns/`: Marketing campaign management.
- 2.5.2 `customer-crm/`: Customer relationship and account servicing.
- 2.5.3 `customer-support/`: Case and ticket management.
- 2.5.4 `customer-shared/`: Shared customer-facing domain assets.

2.6 Financial Management
- 2.6.1 `financial-accounting/`: General ledger, journals, and consolidations.
- 2.6.2 `financial-ap/`: Accounts payable operations.
- 2.6.3 `financial-ar/`: Accounts receivable and billing.
- 2.6.4 `financial-shared/`: Common financial value objects and policies.

2.7 Inventory Management
- 2.7.1 `inventory-stock/`: Stock tracking and valuation.
- 2.7.2 `inventory-warehouse/`: Warehouse execution and movements.
- 2.7.3 `inventory-shared/`: Inventory domain types shared across modules.

2.8 Manufacturing Execution
- 2.8.1 `manufacturing-production/`: Production orders and scheduling.
- 2.8.2 `manufacturing-quality/`: Quality assurance workflows.
- 2.8.3 `manufacturing-maintenance/`: Equipment maintenance management.
- 2.8.4 `manufacturing-shared/`: Shared manufacturing policies and types.

2.9 Operations Service
- 2.9.1 `operations-field-service/`: Field service work orders and dispatch.
- 2.9.2 `operations-shared/`: Shared operations models and utilities.

2.10 Procurement
- 2.10.1 `procurement-purchasing/`: Purchase orders, receiving, and invoicing.
- 2.10.2 `procurement-sourcing/`: Supplier sourcing and RFQ management.
- 2.10.3 `procurement-shared/`: Common procurement domain elements.

2.11 Tenancy and Identity
- 2.11.1 `identity-application/`: Authentication and authorization services.
- 2.11.2 `identity-domain/`: Tenant, user, and role aggregates.
- 2.11.3 `identity-infrastructure/`: Identity provider and directory adapters.

## 3. Integration Principles
3.1 Contexts communicate through asynchronous domain events and well-defined APIs.
3.2 Shared-kernel modules (`*-shared/`) expose versioned contracts to minimize coupling.
3.3 Each context maintains autonomous CI/CD pipelines but conforms to platform governance.
3.4 Cross-cutting concerns (observability, security, messaging) leverage `platform-infrastructure/` packages.
