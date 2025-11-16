# Bounded Context Map

> **Last Updated**: November 5, 2025  
> **Version**: 1.0  
> **Platform**: Kotlin 2.2.0 | Quarkus 3.29.0 | Java 21

## Overview
The ERP platform separates capabilities into **bounded contexts** to keep core domains decoupled and independently deployable. Each context carries its own Gradle module set (domain, application, infrastructure, and optional shared libraries) so teams can evolve features without leaking implementation details across boundaries.

This document serves as the **strategic design map** for understanding context boundaries, integration patterns, and team ownership across the platform.

## Layering Pattern

Each bounded context follows a **three-layer hexagonal architecture** to maintain clean separation of concerns:

### Layer Responsibilities

| Layer | Module Pattern | Purpose | Dependencies |
|-------|---------------|---------|--------------|
| **Domain** | `*-domain/` | Core business logic, entities, aggregates, value objects, domain events, and repository interfaces (ports) | None (pure business logic) |
| **Application** | `*-application/` | Use case orchestration, command/query handlers (CQRS), DTO mapping, transaction boundaries, API endpoints | Domain layer only |
| **Infrastructure** | `*-infrastructure/` | Repository implementations, database adapters, external service clients, message broker implementations | Domain + Application layers |
| **Shared** | `*-shared/` | Reusable value objects, integration contracts, and common abstractions within a context | Domain primitives only |

### Design Principles
- **Domain** modules are framework-agnostic and contain zero external dependencies
- **Application** modules expose use cases through Quarkus services hosting REST APIs or message endpoints
- **Infrastructure** modules connect contexts to persistence (PostgreSQL), messaging, or external systems
- **Shared** modules capture reusable contracts that multiple subdomains inside the same context require

## Interaction Surface

### Communication Patterns

#### Synchronous Integration
- **REST APIs** via `api-gateway` for request-response patterns
- **Direct HTTP calls** between services when low latency is critical
- **API contracts** defined in application layer, versioned appropriately

#### Asynchronous Integration
- **Domain Events** published via `platform-shared/common-messaging`
- **Event-driven workflows** for eventual consistency across contexts
- **Message bus abstractions** for reliable delivery guarantees

#### Cross-Cutting Concerns
- **Observability**: Standardized through `platform-shared/common-observability`
  - OpenTelemetry integration for distributed tracing
  - Structured JSON logging
  - Metrics collection and health checks

- **Validation**: Cross-cutting layer documented in `docs/VALIDATION_ARCHITECTURE.md`
  - Shared DTO/validator library (`financial-shared/validation`)
  - Security filters (rate limiting, audit logging) applied via API Gateway + finance shared modules
  - Metrics/dashboards/runbooks for ADR-010 compliance

- **Security**: Unified through `platform-shared/common-security`
  - JWT token validation
  - Role-based access control (RBAC)
  - Multi-tenancy isolation

- **CQRS & Event Sourcing**: Enabled via `platform-infrastructure/cqrs` and `platform-infrastructure/eventing`
  - Command/query separation for complex workflows
  - Event store for audit trails and temporal queries
  - Saga pattern support for distributed transactions

### Integration Guidelines
1. **Never create direct module dependencies** between distinct bounded contexts
2. **Use shared contracts** from `*-shared` modules or `platform-shared` libraries
3. **Emit domain events** for cross-context state changes requiring eventual consistency
4. **Route HTTP APIs** through `api-gateway` to maintain explicit coupling and enable rate limiting
5. **Leverage CQRS** for long-running workflows spanning multiple contexts

## Context Catalogue

> Each bounded context represents a distinct business capability with clear ownership and autonomy.

### 1. Business Intelligence
**Location**: `bounded-contexts/business-intelligence/`

| Aspect | Details |
|--------|---------|
| **Modules** | `bi-domain`, `bi-application`, `bi-infrastructure` |
| **Focus** | Analytics, reporting, data visualization, and business intelligence pipelines |
| **Team Ownership** | BI & Analytics Team |
| **Integration** | Consumes events from all contexts for data ingestion; exposes curated insights via REST APIs and dashboards |
| **Key Dependencies** | `platform-shared/common-messaging` for event consumption |
| **Upstream Contexts** | All (data consumer) |
| **Downstream Contexts** | Portal (visualization) |

### 2. Commerce
**Location**: `bounded-contexts/commerce/`

| Aspect | Details |
|--------|---------|
| **Modules** | `commerce-b2b`, `commerce-ecommerce`, `commerce-marketplace`, `commerce-pos` |
| **Shared Modules** | `commerce-shared/` → `catalog-shared`, `order-shared`, `pricing-shared` |
| **Focus** | Multi-channel sales operations including B2B portals, e-commerce storefronts, marketplace integrations, and point-of-sale systems |
| **Team Ownership** | Commerce & Sales Team |
| **Integration** | Publishes order events to Inventory and Financial contexts; consumes customer data from Customer Relation; integrates with Payment gateways |
| **Key Dependencies** | `customer-shared`, `inventory-shared`, `financial-shared` for contract alignment |
| **Upstream Contexts** | Customer Relation, Inventory Management |
| **Downstream Contexts** | Financial Management, Communication Hub |

### 3. Communication Hub
**Location**: `bounded-contexts/communication-hub/`

| Aspect | Details |
|--------|---------|
| **Modules** | `communication-domain`, `communication-application`, `communication-infrastructure` |
| **Focus** | Omni-channel communication orchestration including emails, SMS, push notifications, and in-app messaging |
| **Team Ownership** | Platform Services Team |
| **Integration** | Subscribes to events from Customer Relation, Commerce, and Operations for notification triggers; integrates with email/SMS providers (SendGrid, Twilio) |
| **Key Dependencies** | External communication providers, `platform-shared/common-messaging` |
| **Upstream Contexts** | Customer Relation, Commerce, Operations Service |
| **Downstream Contexts** | None (leaf context) |

### 4. Corporate Services
**Location**: `bounded-contexts/corporate-services/`

| Aspect | Details |
|--------|---------|
| **Modules** | `corporate-hr`, `corporate-assets` |
| **Shared Modules** | `corporate-shared` |
| **Focus** | Internal enterprise capabilities including human resources management, workforce planning, and asset tracking/management |
| **Team Ownership** | Corporate Systems Team |
| **Integration** | Publishes employee events to Tenancy & Identity for access provisioning; shares asset data with Operations Service |
| **Key Dependencies** | `identity-domain` for user lifecycle management |
| **Upstream Contexts** | Tenancy & Identity |
| **Downstream Contexts** | Operations Service, Financial Management |

### 5. Customer Relation
**Location**: `bounded-contexts/customer-relation/`

| Aspect | Details |
|--------|---------|
| **Modules** | `customer-crm`, `customer-support`, `customer-campaigns` |
| **Shared Modules** | `customer-shared` |
| **Focus** | Customer lifecycle management including CRM operations, support ticketing, case management, and marketing campaigns |
| **Team Ownership** | Customer Success Team |
| **Integration** | Provides canonical customer profiles to Commerce, Communication Hub; subscribes to order events for customer insights |
| **Key Dependencies** | `commerce-shared` for order context, `communication-domain` for campaign delivery |
| **Upstream Contexts** | Commerce, Communication Hub |
| **Downstream Contexts** | Business Intelligence, Communication Hub |

### 6. Financial Management
**Location**: `bounded-contexts/financial-management/`

| Aspect | Details |
|--------|---------|
| **Modules** | `financial-accounting` (GL), `financial-ap`, `financial-ar` |
| **Shared Modules** | `financial-shared` |
| **Focus** | Complete financial accounting system including general ledger, journal entries, AP/AR operations, and financial reporting |
| **Team Ownership** | Finance & Accounting Team |
| **Integration** | REST via `/api/v1/finance/**` (scopes: financial-admin/user/auditor); consumes `commerce.order.events.v1`, `procurement.payables.events.v1`, `identity.domain.events.v1`; publishes `finance.journal.events.v1`, `finance.period.events.v1`, `finance.reconciliation.events.v1`; optional adapters for external ERPs |
| **Key Dependencies** | `financial_accounting` schema + Flyway migrations, `financial-shared` value objects, API Gateway routing, platform outbox/Kafka stack |
| **Upstream Contexts** | Commerce, Procurement, Identity |
| **Downstream Contexts** | Business Intelligence, Communication Hub, Reporting |

### 7. Inventory Management
**Location**: `bounded-contexts/inventory-management/`

| Aspect | Details |
|--------|---------|
| **Modules** | `inventory-stock`, `inventory-warehouse` |
| **Shared Modules** | `inventory-shared` |
| **Focus** | Stock control, warehouse operations, inventory movements, and fulfillment processes |
| **Team Ownership** | Supply Chain & Logistics Team |
| **Integration** | Subscribes to order events from Commerce for stock reservation; publishes inventory events to Manufacturing and Procurement; canonical source for SKU definitions |
| **Key Dependencies** | `commerce-shared` for product catalog, `manufacturing-shared` for production requirements |
| **Upstream Contexts** | Commerce, Manufacturing Execution, Procurement |
| **Downstream Contexts** | Commerce, Manufacturing Execution |

### 8. Manufacturing Execution
**Location**: `bounded-contexts/manufacturing-execution/`

| Aspect | Details |
|--------|---------|
| **Modules** | `manufacturing-production`, `manufacturing-quality`, `manufacturing-maintenance` |
| **Shared Modules** | `manufacturing-shared` |
| **Focus** | Shop-floor execution systems including production scheduling, work orders, quality control, equipment maintenance, and MES operations |
| **Team Ownership** | Manufacturing & Operations Team |
| **Integration** | Consumes inventory data for bill of materials; publishes production completion events to Inventory; coordinates with Operations Service for maintenance scheduling |
| **Key Dependencies** | `inventory-shared` for material requirements, `operations-shared` for field service |
| **Upstream Contexts** | Inventory Management, Operations Service |
| **Downstream Contexts** | Inventory Management, Business Intelligence |

### 9. Operations Service
**Location**: `bounded-contexts/operations-service/`

| Aspect | Details |
|--------|---------|
| **Modules** | `operations-field-service` |
| **Shared Modules** | `operations-shared` |
| **Focus** | Field service management including service tickets, technician scheduling, mobile workforce management, and operational scheduling |
| **Team Ownership** | Field Operations Team |
| **Integration** | Coordinates with Customer Support for ticket routing; integrates with Manufacturing for maintenance requests; shares schedules with Corporate Services for resource planning |
| **Key Dependencies** | `customer-shared` for customer context, `manufacturing-shared` for equipment data |
| **Upstream Contexts** | Customer Relation, Manufacturing Execution |
| **Downstream Contexts** | Communication Hub, Business Intelligence |

### 10. Procurement
**Location**: `bounded-contexts/procurement/`

| Aspect | Details |
|--------|---------|
| **Modules** | `procurement-purchasing`, `procurement-sourcing` |
| **Shared Modules** | `procurement-shared` |
| **Focus** | Purchase requisitions, purchase orders, supplier management, vendor sourcing, RFQs, and contract management |
| **Team Ownership** | Procurement & Sourcing Team |
| **Integration** | Publishes purchase orders to Inventory for receiving; integrates with Financial Management for payables; coordinates with Manufacturing for material requirements planning |
| **Key Dependencies** | `financial-shared` for payment terms, `inventory-shared` for material catalog |
| **Upstream Contexts** | Inventory Management, Manufacturing Execution |
| **Downstream Contexts** | Financial Management, Inventory Management |

### 11. Tenancy & Identity
**Location**: `bounded-contexts/tenancy-identity/`

| Aspect | Details |
|--------|---------|
| **Modules** | `identity-domain`, `identity-application`, `identity-infrastructure` |
| **Focus** | Multi-tenancy support, tenant onboarding, authentication (OAuth 2.0, OIDC), authorization (RBAC), user management, and identity federation |
| **Team Ownership** | Platform Security Team |
| **Integration** | Foundational context providing identity and access management for entire platform; integrates with all contexts for authentication; uses `platform-shared/common-security` |
| **Key Dependencies** | External identity providers (LDAP, Azure AD, Okta), `platform-shared/common-security` |
| **Upstream Contexts** | None (foundational) |
| **Downstream Contexts** | All contexts (identity provider) |

---

## Platform Components

### API Gateway
**Location**: `api-gateway/`

The API Gateway serves as the single entry point for all external client requests, providing:
- **Request Routing**: Directs traffic to appropriate bounded contexts
- **Rate Limiting**: Configured via `config/rate-limits.yml`
- **Authentication**: JWT validation and token verification
- **API Composition**: Aggregates responses from multiple contexts
- **Cross-Cutting Concerns**: Centralized logging, tracing, and metrics

### Platform Infrastructure
**Location**: `platform-infrastructure/`

Provides reusable infrastructure patterns and components:

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **CQRS** (`cqrs/`) | Command-Query Responsibility Segregation | Command/query handlers, command bus, query bus abstractions |
| **Eventing** (`eventing/`) | Event-driven architecture support | Event publishers, subscribers, event store, saga orchestration |
| **Monitoring** (`monitoring/`) | Observability infrastructure | Health checks, metrics collection, distributed tracing, performance monitoring |

### Platform Shared
**Location**: `platform-shared/`

Cross-cutting concerns and utilities available to all bounded contexts:

| Module | Purpose | Usage |
|--------|---------|-------|
| **common-messaging** | Message bus abstractions | Domain event publishing, reliable messaging, pub/sub patterns |
| **common-observability** | OpenTelemetry integration | Structured logging, distributed tracing, metrics collection |
| **common-security** | Security utilities | JWT handling, encryption, RBAC utilities, audit logging |
| **common-types** | Shared primitives | Base entities, value objects, domain primitives, result types |

### Portal
**Location**: `portal/`

TypeScript-based web UI providing unified interface for all bounded contexts with modern frontend framework integration.

---

## Context Relationships

### Context Map Visualization

```
                           ┌─────────────────────┐
                           │  Tenancy & Identity │
                           │  (Foundational)     │
                           └──────────┬──────────┘
                                      │ provides auth
                   ┌──────────────────┼──────────────────┐
                   │                  │                  │
         ┌─────────▼──────┐  ┌────────▼────────┐  ┌────▼──────────┐
         │  API Gateway    │  │  Communication  │  │   Portal      │
         │  (Entry Point)  │  │      Hub        │  │   (UI)        │
         └─────────┬───────┘  └─────────────────┘  └───────────────┘
                   │ routes
    ┌──────────────┼──────────────┬──────────────┬─────────────┐
    │              │              │              │             │
┌───▼───────┐ ┌───▼────────┐ ┌──▼─────────┐ ┌──▼──────┐  ┌──▼────────┐
│ Commerce  │ │ Customer   │ │ Financial  │ │ Procure │  │ Inventory │
│           │ │ Relation   │ │ Management │ │  -ment  │  │ Managemt  │
└─────┬─────┘ └──────┬─────┘ └─────┬──────┘ └────┬────┘  └─────┬─────┘
      │              │              │             │             │
      └──────┬───────┴──────┬───────┴─────────────┴─────────────┘
             │              │                     │
      ┌──────▼──────┐ ┌─────▼──────────┐  ┌──────▼──────────┐
      │Manufacturing│ │   Operations   │  │   Corporate     │
      │  Execution  │ │    Service     │  │   Services      │
      └──────┬──────┘ └────────────────┘  └─────────────────┘
             │
      ┌──────▼────────────┐
      │    Business       │
      │  Intelligence     │
      │  (Analytics)      │
      └───────────────────┘
```

### Relationship Types

| Type | Symbol | Description | Example |
|------|--------|-------------|---------|
| **Upstream-Downstream** | U→D | Provider-Consumer relationship | Commerce → Financial Management |
| **Published Language** | PL | Shared contract/API | `*-shared` modules |
| **Conformist** | CF | Consumer adopts provider's model | All contexts → Tenancy & Identity |
| **Anti-Corruption Layer** | ACL | Translation layer for external systems | Infrastructure adapters |
| **Shared Kernel** | SK | Common domain primitives | `platform-shared/common-types` |

---

## Working With Bounded Contexts

### Design Guidelines

1. **Context Selection**
   - Place new functionality in the context that owns the relevant domain language
   - Introduce a new context only when vocabulary diverges materially
   - Avoid splitting contexts prematurely—prefer starting cohesive and refactoring later

2. **Dependency Management**
   - **NEVER** create direct module dependencies between distinct bounded contexts
   - Share contracts through `*-shared` modules within a context
   - Use `platform-shared` libraries for cross-cutting concerns
   - Define integration points explicitly via events or APIs

3. **Cross-Context Communication**
   - **Synchronous**: REST APIs via `api-gateway` for request-response patterns
   - **Asynchronous**: Domain events via `platform-shared/common-messaging` for eventual consistency
   - **Long-Running Workflows**: CQRS sagas via `platform-infrastructure/cqrs`
   - **Data Synchronization**: Event sourcing via `platform-infrastructure/eventing`

4. **Testing Strategy**
   - Unit tests within each module for domain logic
   - Integration tests for cross-module interactions within a context
   - Contract tests for inter-context API/event contracts
   - End-to-end tests for critical business workflows

5. **Team Ownership**
   - Each context should have a dedicated team responsible for its evolution
   - Teams own the complete vertical slice (domain, application, infrastructure)
   - Cross-team coordination via published language and event contracts
   - Architecture Decision Records (ADRs) document significant context changes

### Common Patterns

#### Event-Driven Integration
```kotlin
// Publishing domain events
class OrderPlacedEvent(
    val orderId: OrderId,
    val customerId: CustomerId,
    val amount: Money
) : DomainEvent

// In application service
eventPublisher.publish(OrderPlacedEvent(...))
```

#### Shared Contracts
```kotlin
// In commerce-shared/order-shared
data class OrderReference(
    val orderId: UUID,
    val orderNumber: String
)

// Used by multiple contexts without direct coupling
```

#### Anti-Corruption Layer
```kotlin
// In infrastructure layer
class ExternalPaymentGatewayAdapter(
    private val paymentGateway: ExternalPaymentService
) : PaymentPort {
    override fun processPayment(payment: Payment): Result<PaymentConfirmation> {
        // Translate domain model to external API format
        val externalRequest = payment.toExternalFormat()
        val response = paymentGateway.charge(externalRequest)
        // Translate back to domain model
        return response.toDomainResult()
    }
}
```

---

## References

- **Architecture Documentation**: `docs/ARCHITECTURE.md`
- **Build System Guide**: `docs/BUILD_SYSTEM_UPDATE.md`
- **Architecture Decision Records**: `docs/adr/`
- **Deployment Guide**: `deployment/README.md`
- **Testing Strategy**: `tests/README.md`

---

> **Note**: This context map is a living document and should be updated as contexts evolve or new contexts are introduced. All changes should be accompanied by an ADR documenting the rationale.
