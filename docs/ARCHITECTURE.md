# ERP Platform Architecture

## Overview

This is a modern, cloud-native Enterprise Resource Planning (ERP) platform built using Domain-Driven Design (DDD) principles and microservices architecture. The system is designed to be modular, scalable, and maintainable, with clear separation of concerns and bounded contexts.

## Technology Stack

### Core Technologies
- **Language**: Kotlin 2.2.0
- **JDK**: Java 21
- **Framework**: Quarkus 3.29.0 (Reactive, Cloud-Native)
- **Build System**: Gradle 8.x with Kotlin DSL
- **Database**: PostgreSQL
- **API Style**: RESTful with Jackson serialization

### Key Libraries & Frameworks
- **Dependency Injection**: Quarkus Arc (CDI)
- **ORM**: Hibernate ORM with Panache
- **Validation**: Hibernate Validator
- **Security**: Elytron Security
- **Observability**: OpenTelemetry, JSON Logging
- **Caching**: Quarkus Cache
- **Testing**: JUnit 5, MockK, Kotest

## Architectural Style

### Domain-Driven Design (DDD)

The platform is organized into **bounded contexts**, each representing a distinct business domain with its own:
- Ubiquitous language
- Domain model
- Data ownership
- Team ownership

### Hexagonal Architecture (Ports & Adapters)

Each bounded context follows a **three-layer hexagonal architecture**:

```
┌─────────────────────────────────────────┐
│         Application Layer                │
│  (Use Cases, Commands, Queries, DTOs)   │
├─────────────────────────────────────────┤
│           Domain Layer                   │
│  (Entities, Aggregates, Value Objects,  │
│   Domain Events, Business Logic)        │
├─────────────────────────────────────────┤
│       Infrastructure Layer               │
│  (Repositories, External APIs, DB,      │
│   Message Brokers, Adapters)            │
└─────────────────────────────────────────┘
```

**Layers**:
1. **Application Layer** (`*-application/`)
   - Orchestrates use cases
   - Handles commands and queries (CQRS)
   - Maps between domain and DTOs
   - Transaction boundaries

2. **Domain Layer** (`*-domain/`)
   - Core business logic
   - Domain entities and aggregates
   - Value objects
   - Domain events
   - Domain services
   - Repository interfaces (ports)

3. **Infrastructure Layer** (`*-infrastructure/`)
   - Repository implementations
   - Database adapters
   - External service integrations
   - Message broker implementations
   - Framework-specific code

## Bounded Contexts

### 1. Business Intelligence (`business-intelligence/`)
Analytics, reporting, and data visualization capabilities.

**Modules**:
- `bi-application/` - BI use cases and query handlers
- `bi-domain/` - Analytics domain models
- `bi-infrastructure/` - Data warehouse adapters

### 2. Commerce (`commerce/`)
Multi-channel commerce capabilities for various sales channels.

**Sub-contexts**:
- `commerce-b2b/` - Business-to-business sales
- `commerce-ecommerce/` - Online retail
- `commerce-marketplace/` - Third-party marketplace integration
- `commerce-pos/` - Point of Sale systems
- `commerce-shared/` - Shared commerce domain models

### 3. Communication Hub (`communication-hub/`)
Centralized communication management for emails, SMS, and notifications.

**Modules**:
- `communication-application/` - Message orchestration
- `communication-domain/` - Communication domain models
- `communication-infrastructure/` - Email/SMS providers

### 4. Corporate Services (`corporate-services/`)
Internal corporate management functions.

**Sub-contexts**:
- `corporate-assets/` - Asset management and tracking
- `corporate-hr/` - Human resources management
- `corporate-shared/` - Shared corporate domain models

### 5. Customer Relation (`customer-relation/`)
Customer-facing services and relationship management.

**Sub-contexts**:
- `customer-campaigns/` - Marketing campaigns
- `customer-crm/` - Customer relationship management
- `customer-support/` - Support ticketing and case management
- `customer-shared/` - Shared customer domain models

### 6. Financial Management (`financial-management/`)
Complete financial accounting and management system.

**Sub-contexts**:
- `financial-accounting/` - General ledger, journal entries
- `financial-ap/` - Accounts Payable
- `financial-ar/` - Accounts Receivable
- `financial-shared/` - Shared financial domain models

### 7. Inventory Management (`inventory-management/`)
Stock control and warehouse operations.

**Sub-contexts**:
- `inventory-stock/` - Stock levels and movements
- `inventory-warehouse/` - Warehouse operations
- `inventory-shared/` - Shared inventory domain models

### 8. Manufacturing Execution (`manufacturing-execution/`)
Production planning and execution systems.

**Sub-contexts**:
- `manufacturing-production/` - Production orders and scheduling
- `manufacturing-quality/` - Quality control and assurance
- `manufacturing-maintenance/` - Equipment maintenance
- `manufacturing-shared/` - Shared manufacturing domain models

### 9. Operations Service (`operations-service/`)
Field service management and operations.

**Sub-contexts**:
- `operations-field-service/` - Field service tickets and scheduling
- `operations-shared/` - Shared operations domain models

### 10. Procurement (`procurement/`)
Purchase requisitions, orders, and supplier management.

**Sub-contexts**:
- `procurement-purchasing/` - Purchase orders and receiving
- `procurement-sourcing/` - Supplier sourcing and RFQs
- `procurement-shared/` - Shared procurement domain models

### 11. Tenancy & Identity (`tenancy-identity/`)
Multi-tenancy support and identity/access management.

**Modules**:
- `identity-application/` - Authentication and authorization
- `identity-domain/` - User, role, and tenant domain models
- `identity-infrastructure/` - Identity provider integrations
- Related ADRs: ADR-005 Multi-Tenancy Data Isolation Strategy.

## Platform Components

### API Gateway (`api-gateway/`)
Central entry point for all client requests.

**Responsibilities**:
- Request routing to appropriate bounded contexts
- Rate limiting and throttling
- Authentication and authorization
- API composition and aggregation
- Cross-cutting concerns (logging, tracing)

**Configuration**:
- `config/api-gateway.env` - Environment configuration
- `config/rate-limits.yml` - Rate limiting rules
- Related ADRs: ADR-004 API Gateway Pattern.

### Platform Infrastructure (`platform-infrastructure/`)
Shared infrastructure components and patterns.

**Components**:

1. **CQRS** (`cqrs/`)
   - Command and query separation
   - Command handlers
   - Query handlers
   - Command bus and query bus abstractions
   - Related ADRs: ADR-001 Modular CQRS Implementation.

2. **Eventing** (`eventing/`)
   - Domain event publishers
   - Event subscribers
   - Event store abstractions
   - Event-driven communication patterns
   - Related ADRs: ADR-003 Event-Driven Integration Between Contexts.

3. **Monitoring** (`monitoring/`)
   - Health checks
   - Metrics collection
   - Distributed tracing
   - Performance monitoring

### Platform Shared (`platform-shared/`)
Reusable cross-cutting concerns and utilities.

**Governance:** Strictly controlled to prevent distributed monolith anti-pattern. See ADR-006 Platform-Shared Governance Rules.

**Modules**:

1. **common-messaging** - Message bus abstractions and implementations
2. **common-observability** - OpenTelemetry integration, logging, tracing
3. **common-security** - Security utilities, encryption, JWT handling
4. **common-types** - Shared value objects, base entities, domain primitives

**Allowed Content:**
- ✅ Technical primitives (Result<T>, Command, Query, DomainEvent)
- ✅ Framework integration contracts (CommandHandler, EventPublisher)
- ✅ Observability infrastructure (CorrelationId, StructuredLogger)
- ✅ Security primitives (AuthenticationPrincipal, JWT utilities)

**Forbidden Content:**
- ❌ Business domain models (Customer, Order, Invoice)
- ❌ Business logic (TaxCalculator, DiscountPolicy)
- ❌ Shared DTOs (API contracts belong in application layer)
- ❌ Utility dumping grounds (StringUtils, DateUtils)

**Enforcement:** ArchUnit tests in `tests/arch/PlatformSharedGovernanceRules.kt`

**Related ADRs:** ADR-006 Platform-Shared Governance Rules

### Portal (`portal/`)
Web-based user interface built with TypeScript.

**Technology**:
- TypeScript
- Modern frontend framework (React/Vue/Angular)
- Unified UI for all bounded contexts

## Communication Patterns

### Synchronous Communication
- **REST APIs** - HTTP/JSON for request-response patterns
- **API Gateway** - Single entry point for external clients
- **Direct calls** - Within bounded context boundaries

### Asynchronous Communication
- **Domain Events** - Event-driven communication between contexts
- **Message Bus** - Reliable message delivery
- **Event Store** - Event sourcing capabilities
- **Pub/Sub** - Decoupled event publishing and subscription

### Data Consistency
- **Eventual Consistency** - Between bounded contexts
- **Strong Consistency** - Within bounded context (transaction boundaries)
- **Saga Pattern** - Distributed transaction coordination
- Related ADRs: ADR-002 Database Per Bounded Context, ADR-005 Multi-Tenancy Data Isolation Strategy.

## Deployment Architecture

### Containerization
- **Docker** - Container images for each service
- **Multi-stage builds** - Optimized image sizes
- **Base images** - Standardized runtime environments

### Orchestration
- **Kubernetes** - Container orchestration and management
- **Helm Charts** - Declarative deployment configurations
- **Service Mesh** - Inter-service communication (optional)

### Infrastructure as Code
- **Terraform** - Cloud infrastructure provisioning
- **Kubernetes Manifests** - Service deployment configurations
- **Helm** - Application package management

### Deployment Strategies
- **Blue-Green Deployment** - Zero-downtime releases
- **Rolling Updates** - Gradual service updates
- **Canary Releases** - Progressive traffic shifting

## Testing Strategy

### Test Pyramid (`tests/`)

```
        /\
       /E2E\         ← End-to-End Tests
      /──────\
     /Contract\      ← Contract Tests
    /──────────\
   / Integration \   ← Integration Tests
  /──────────────\
 /  Unit Tests    \  ← Unit Tests (in each module)
/──────────────────\
```

**Test Types**:

1. **Unit Tests** - Within each module
   - Domain logic testing
   - Pure business rules
   - Isolated component testing

2. **Integration Tests** (`tests/integration/`)
   - Database integration
   - Message broker integration
   - External API integration

3. **Contract Tests** (`tests/contract/`)
   - API contract verification
   - Consumer-driven contracts
   - Schema validation

4. **Architecture Tests** (`tests/arch/`)
   - Enforce architectural rules
   - Layer dependency validation
   - Package structure verification

5. **End-to-End Tests** (`tests/e2e/`)
   - Full business workflows
   - User journey testing
   - Cross-context scenarios

## Security Architecture

### Authentication & Authorization
- **Multi-tenant Architecture** - Tenant isolation
- **Identity Context** - Centralized user management
- **JWT Tokens** - Stateless authentication
- **RBAC** - Role-based access control
- **Fine-grained Permissions** - Context-specific authorization

### Security Layers
1. **Gateway Level** - Initial authentication and rate limiting
2. **Service Level** - Authorization and data filtering
3. **Data Level** - Tenant isolation and encryption

## Observability

### Logging
- **Structured Logging** - JSON format for machine parsing
- **Correlation IDs** - Request tracing across services
- **Log Aggregation** - Centralized log storage

### Metrics
- **OpenTelemetry** - Standard metrics collection
- **Business Metrics** - Domain-specific KPIs
- **Infrastructure Metrics** - Resource utilization

### Tracing
- **Distributed Tracing** - Request flow visualization
- **Performance Analysis** - Bottleneck identification
- **Dependency Mapping** - Service dependency graphs

## Build System

### Gradle Configuration
- **Multi-module Build** - Monorepo structure
- **Convention Plugins** (`build-logic/`) - Shared build configuration
- **Version Catalog** (`gradle/libs.versions.toml`) - Centralized dependency management
- **Type-safe Project Accessors** - Compile-time project references

### Build Conventions
- **Automatic Module Discovery** - Dynamic project inclusion
- **Shared Configurations** - Consistent build setup
- **Dependency Management** - BOM imports for version alignment

## Design Principles

### SOLID Principles
- **Single Responsibility** - Classes have one reason to change
- **Open/Closed** - Open for extension, closed for modification
- **Liskov Substitution** - Subtypes must be substitutable
- **Interface Segregation** - Many specific interfaces over general ones
- **Dependency Inversion** - Depend on abstractions, not concretions

### DDD Tactical Patterns
- **Entities** - Objects with identity
- **Value Objects** - Immutable objects without identity
- **Aggregates** - Consistency boundaries
- **Domain Events** - Domain-significant occurrences
- **Repositories** - Collection-like access to aggregates
- **Domain Services** - Operations that don't belong to entities

### Architectural Principles
- **Separation of Concerns** - Clear layer boundaries
- **Dependency Rule** - Dependencies point inward (domain is core)
- **Screaming Architecture** - Structure reveals intent
- **Convention over Configuration** - Sensible defaults
- **Fail Fast** - Early validation and error detection

## Deployment Models

### Monolith Mode
Deploy all bounded contexts as a single application:
- **Pros**: Simpler deployment, easier local development
- **Cons**: All-or-nothing scaling, tight coupling at runtime

### Microservices Mode
Deploy each bounded context independently:
- **Pros**: Independent scaling, technology diversity, isolated failures
- **Cons**: Operational complexity, distributed system challenges

### Hybrid Mode
Start as monolith, extract performance-critical contexts:
- **Pros**: Balanced approach, gradual evolution
- **Cons**: Requires careful planning and migration strategy

## Future Enhancements

### Planned Features
- **Event Sourcing** - Full event store implementation
- **GraphQL Gateway** - Alternative query interface
- **Service Mesh** - Enhanced inter-service communication
- **Chaos Engineering** - Resilience testing
- **Machine Learning** - Predictive analytics integration

### Scalability Considerations
- **Horizontal Scaling** - Add more instances of services
- **Database Sharding** - Partition data across databases
- **Caching Strategy** - Multi-level caching (L1, L2, distributed)
- **Read Replicas** - Separate read and write databases

## Context Map

For details on how bounded contexts interact with each other, see [CONTEXT_MAP.md](CONTEXT_MAP.md).

## Architecture Decision Records

For detailed architectural decisions and their rationale, see the [ADR directory](adr/).

**Key ADRs:**
- **ADR-001:** Modular CQRS Implementation
- **ADR-002:** Database Per Bounded Context
- **ADR-003:** Event-Driven Integration Between Contexts
- **ADR-004:** API Gateway Pattern
- **ADR-005:** Multi-Tenancy Data Isolation Strategy
- **ADR-006:** Platform-Shared Governance Rules *(Critical for preventing distributed monolith)*

## References

- **Domain-Driven Design** by Eric Evans
- **Implementing Domain-Driven Design** by Vaughn Vernon
- **Building Microservices** by Sam Newman
- **Clean Architecture** by Robert C. Martin
- **Quarkus Documentation**: https://quarkus.io/
