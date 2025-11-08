# ADR-006: Platform-Shared Governance Rules

**Status:** Accepted (Enforced in CI)  
**Date:** 2025-11-06  
**Context:** Phase 2 - Preventing Shared Kernel Anti-Pattern

---

## Decision

Establish strict governance rules for `platform-shared` modules to prevent distributed monolith anti-pattern and maintain bounded context autonomy.

---

## Enforcement (Operational)

The following architecture suites are enforced in CI and block PRs:

- Platform-Shared Governance (this ADR): `PlatformSharedGovernanceRules`
- Layering rules: `LayeringRules`
- Hexagonal architecture rules: `HexagonalArchitectureRules`

CI configuration (summary):

- Main workflow runs (blocking):
  - `./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*"`
  - `./gradlew :tests:arch:test --tests "*LayeringRules*"`
  - `./gradlew :tests:arch:test --tests "*HexagonalArchitectureRules*"`
- Scheduled governance workflow mirrors the same steps weekly.

Local developer workflow:

- `./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*"`
- `./gradlew :tests:arch:test --tests "*LayeringRules*"`
- `./gradlew :tests:arch:test --tests "*HexagonalArchitectureRules*"`

See `docs/ARCHITECTURE_TESTING_GUIDE.md` for wiring details, scope expansion, and troubleshooting.

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
- ✅ **Automated:** ArchUnit tests in CI pipeline (`.github/workflows/ci.yml`) - **ENFORCED (Blocking)**
- ✅ **Weekly Audit:** GitHub Actions workflow (`.github/workflows/arch-governance.yml`) - **ENFORCED (Blocking)**
- ✅ **Local Audit:** PowerShell script (`scripts/audit-platform-shared.ps1`)
- 📋 **Code Review:** Manual checklist for platform-shared PRs

**Owner:** Lead Architect / Senior Engineer  
**Escalation:** Team consensus required to add new shared module

---

## Enforcement Status

**Status:** ✅ **ENFORCED** (as of November 9, 2025)

### CI Integration (Always-On)

**Main CI Pipeline** (`.github/workflows/ci.yml`):
```yaml
# Build job - blocks all PRs
- name: Enforce platform-shared governance (ADR-006)
  run: ./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*" --no-daemon --stacktrace

# Architecture-tests job - dedicated enforcement
- name: Enforce platform-shared governance (ADR-006)
  run: ./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*" --no-daemon --stacktrace
```

**Weekly Governance Audit** (`.github/workflows/arch-governance.yml`):
```yaml
# Runs every Monday at 09:00 UTC (blocking)
- name: Run ArchUnit tests (enforced)
  run: ./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*" --no-daemon --stacktrace
```

### Coverage

**All 12 bounded contexts wired and validated:**
- ✅ platform-shared (4 modules)
- ✅ tenancy-identity (3 modules)
- ✅ financial-management (10 modules)
- ✅ commerce (12 modules)
- ✅ business-intelligence (3 modules)
- ✅ communication-hub (3 modules)
- ✅ corporate-services (6 modules)
- ✅ customer-relation (9 modules)
- ✅ inventory-management (6 modules)
- ✅ manufacturing-execution (9 modules)
- ✅ operations-service (3 modules)
- ✅ procurement (6 modules)

**Total:** 74 modules under governance

### Local Validation

**Run tests locally:**
```bash
./gradlew :tests:arch:test --tests "*PlatformSharedGovernanceRules*"
```

**Run with full test suite:**
```bash
./gradlew :tests:arch:test
```

### Rollout Timeline

- **2025-11-06:** ADR accepted, ArchUnit infrastructure created
- **2025-11-07:** Opt-in advisory mode enabled, identity + platform-shared wired
- **2025-11-08:** Sprint 3 expansion completed (all 12 contexts wired)
- **2025-11-09:** ✅ **Enforcement enabled** - all rules passing, CI blocking activated

---

## Compliance Verification

**Last Verified:** November 9, 2025  
**Verification Method:** ArchUnit 1.2.1 automated tests  
**Result:** ✅ All 8 governance rules passing across 74 modules

**Next Audit:** Next sprint (scheduled weekly via CI)

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
