# ADR-006: Platform-Shared Governance Rules

**Status:** Accepted (Enforced in CI)  
**Date:** 2025-11-06  
**Context:** Phase 2 - Preventing Shared Kernel Anti-Pattern

---

## Decision

Establish strict governance rules for `platform-shared` modules to prevent distributed monolith anti-pattern and maintain bounded context autonomy.

---

## Allowed in platform-shared

### 1. Technical Primitives ONLY
```kotlin
// ✅ Pure abstractions with no business semantics
sealed class Result<out T>
interface Command
interface Query<out R>
interface DomainEvent
data class DomainError(code: String, message: String)
```

### 2. Framework Integration Contracts
```kotlin
// ✅ CQRS infrastructure
interface CommandHandler<in C : Command, out R>
interface QueryHandler<in Q : Query<R>, out R>

// ✅ Event abstractions
interface EventPublisher
interface EventSubscriber<in E : DomainEvent>
```

### 3. Observability Infrastructure
```kotlin
// ✅ Logging, metrics, tracing
data class CorrelationId(val value: String)
interface StructuredLogger
object MetricsCollector
```

### 4. Security Primitives
```kotlin
// ✅ Authentication/authorization infrastructure
interface AuthenticationPrincipal
sealed class SecurityContext
```

---

## Forbidden in platform-shared

### 1. Domain Models
```kotlin
// ❌ Business domain concepts belong in bounded contexts
data class CustomerAddress  // → customer-relation context
enum class OrderStatus      // → commerce context
data class InvoiceLineItem  // → financial-management context
```

### 2. Business Logic
```kotlin
// ❌ Business rules belong in domain layer of specific context
class TaxCalculator         // → financial-management context
object DiscountPolicy       // → commerce context
fun validatePassword()      // → tenancy-identity context
```

### 3. Shared DTOs
```kotlin
// ❌ API contracts are context-specific
data class CreateOrderRequest    // → commerce-ecommerce/application
data class CustomerResponse      // → customer-relation/application
```

### 4. Utility Classes
```kotlin
// ❌ Avoid "utils" dumping ground
object StringUtils
object DateUtils
object CollectionUtils
```

---

## Governance Mechanisms

### 1. Module Size Limit
- **Maximum:** 4 modules in `platform-shared/`
- **Current:** 4 (common-types, common-observability, common-security, common-messaging)
- **Adding 5th module requires:** Architecture review + team consensus

### 2. File Count Alert
- **Warning threshold:** 25 files per module
- **Critical threshold:** 50 files per module
- **Action:** Trigger refactoring review

### 3. Dependency Rules (ArchUnit)
```kotlin
// Enforce with architecture tests
@Test
fun `platform-shared must not depend on bounded contexts`() {
    noClasses()
        .that().resideInAPackage("com.erp.shared..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "com.erp.identity..",
            "com.erp.finance..",
            "com.erp.commerce.."
            // ... all bounded contexts
        )
}

@Test
fun `bounded contexts must not depend on each other`() {
    noClasses()
        .that().resideInAPackage("com.erp.identity..")
        .should().dependOnClassesThat().resideInAPackage("com.erp.finance..")
}
```

### 4. Code Review Checklist
Every PR touching `platform-shared` must answer:
- [ ] Is this a pure technical primitive?
- [ ] Does it contain zero business semantics?
- [ ] Would 2+ contexts use the EXACT same behavior?
- [ ] Is coupling cost < duplication cost?
- [ ] Could this belong in a specific context instead?

---

## Duplication vs. Sharing Decision Matrix

| Scenario | Decision | Rationale |
|----------|----------|-----------|
| `Result<T>` for error handling | **SHARE** | Pure technical contract, identical semantics |
| `Email` value object | **DUPLICATE** | Identity validation ≠ Marketing campaign validation |
| `Address` data structure | **DUPLICATE** | Tax address ≠ Shipping address ≠ User profile address |
| `Currency` enum | **SHARE (carefully)** | ISO 4217 standard, but context-specific formatting |
| `CommandHandler` interface | **SHARE** | Pure CQRS infrastructure pattern |
| `AuditInfo` metadata | **SHARE** | Technical audit trail, consistent semantics |
| `PhoneNumber` validation | **DUPLICATE** | Customer contact ≠ HR emergency contact ≠ Supplier phone |
| `Money` value object | **DUPLICATE** | Finance precision ≠ Commerce display rounding |

---

## Alternatives Considered

### Alternative 1: Shared Domain Module
Create `platform-shared/common-domain` with reusable domain models.

**Rejected because:**
- Violates bounded context autonomy
- Creates tight coupling across all contexts
- Single change requires redeploying all services
- Semantic differences hidden behind shared types

### Alternative 2: No Shared Modules
Force each context to implement everything from scratch.

**Rejected because:**
- Duplicates technical infrastructure (Result, Command, Event patterns)
- Inconsistent observability and security implementations
- Wastes effort on non-differentiating technical code

### Alternative 3: Shared Libraries via Maven/Gradle Publishing
Publish shared modules as versioned dependencies.

**Deferred because:**
- Adds complexity during rapid development phase
- Version management overhead
- Consider for Phase 7 (Production deployment)

---

## Consequences

### Positive
- ✅ Bounded contexts remain autonomous
- ✅ Teams can evolve independently
- ✅ Deploy contexts without coordinating changes
- ✅ Clear ownership and responsibility
- ✅ Prevents distributed monolith

### Negative
- ❌ Some code duplication across contexts
- ❌ Requires discipline during code reviews
- ❌ Need to educate team on bounded context principles

### Neutral
- 🔄 Periodic audits required to prevent drift
- 🔄 ArchUnit tests need maintenance as contexts grow

---

## Compliance

**Review Frequency:** Every Sprint (2 weeks)  
**Enforcement:** 
- ✅ **Automated:** ArchUnit tests in CI pipeline (`.github/workflows/ci.yml`)
- ✅ **Weekly Audit:** GitHub Actions workflow (`.github/workflows/governance-audit.yml`)
- ✅ **Local Audit:** PowerShell script (`scripts/audit-platform-shared.ps1`)
- 📋 **Code Review:** Manual checklist for platform-shared PRs

**Owner:** Lead Architect / Senior Engineer  
**Escalation:** Team consensus required to add new shared module

**CI Integration (Opt-In for now):**
```yaml
# .github/workflows/ci.yml
- name: Enforce platform-shared governance (ADR-006)
  run: ./gradlew :tests:arch:test -PrunArchTests=true --tests "*PlatformSharedGovernanceRules*"
```

**Weekly Monitoring:**
- A scheduled workflow runs every Monday at 09:00 UTC
- It executes ArchUnit tests with `-PrunArchTests=true` and uploads the report
- It can be configured as non-blocking (advisory) until violations are addressed

---

## Related ADRs
- ADR-001: Modular CQRS Implementation
- ADR-003: Event-Driven Integration Between Contexts
- ADR-005: Multi-Tenancy Data Isolation Strategy

---

## References
- *Domain-Driven Design* by Eric Evans (Ch. 14: Maintaining Model Integrity)
- *Implementing Domain-Driven Design* by Vaughn Vernon (Ch. 3: Context Mapping)
- *Building Microservices* by Sam Newman (Ch. 1: Microservices at Scale)
