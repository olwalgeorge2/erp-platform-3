# Sprint 2: Architecture Governance Remediation (ADR-006)

## 🎯 **Objective**
Fix 6 ArchUnit rule violations to enable always-on enforcement of ADR-006 platform-shared governance rules.

**Current Status:** Advisory mode (non-blocking weekly CI)  
**Target:** Flip to enforcing in Sprint 2  
**Estimated Effort:** 12-20 hours (split across 2-3 engineers)

---

## ✅ Outcomes (Enforced in CI)

- All bounded contexts are wired into `:tests:arch` classpath and validated against real code (no empty‑match false positives)
- ADR‑006 Platform‑Shared Governance is enforced (blocking) in CI
- Layering and Hexagonal architecture suites are also enforced (blocking)
- Weekly governance workflow retained for scheduled visibility
- Contributor guide published: `docs/ARCHITECTURE_TESTING_GUIDE.md`
- Sprint 3 expansion plan documented: `docs/SPRINT3_ARCH_EXPANSION_PLAN.md`

These outcomes mirror our error‑handling rollout: harden → advise → enforce, with clear developer guidance and sustainable governance.

## 📊 **Violation Summary**

**Status as of 2025-11-09:**
- ✅ **2 Rules PASSING:**
  - `common-types` modules must use only standard Java/Kotlin libraries
  - `common-messaging` and `common-observability` must be framework-agnostic
  
- ❌ **6 Rules FAILING** (needs investigation):
  - Rule 1: platform-shared must not depend on bounded contexts
  - Rule 2: bounded contexts must not depend on each other directly  
  - Rule 3: platform-shared should not contain REST resources
  - Rule 4: platform-shared should not contain JPA entities
  - Rule 5: platform-shared should not contain Quarkus-specific services
  - Rule 6: platform-shared must not depend on platform-infrastructure

**Note:** Rules may be failing due to ArchUnit "no classes matched" issue (empty should clause). This means either:
1. No violations exist (good!)
2. Package patterns need adjustment
3. Need to add `.allowEmptyShould(true)` to rules

---

## 🔍 **Investigation Phase (Week 1, Day 1-2)**

### **Task 1.1: Validate Rule Behavior**
**Owner:** TBD  
**Effort:** 2 hours  
**Action:**
```bash
# Run tests with detailed output
./gradlew :tests:arch:test -PrunArchTests=true --tests "*PlatformSharedGovernanceRules*" --stacktrace

# Check HTML report
open tests/arch/build/reports/tests/test/index.html
```

**Outcome:** Determine if failures are real violations or rule configuration issues.

---

### **Task 1.2: Scan for Actual Violations**
**Owner:** TBD  
**Effort:** 1-2 hours  
**Action:**

#### **Rule 1: Bounded-context imports in platform-shared**
```powershell
# Windows (PowerShell)
Get-ChildItem -Recurse platform-shared\**\*.kt | Select-String "import com\.erp\.(identity|finance|commerce|inventory|manufacturing|procurement|customer|corporate|communication|operations|bi)\."

# Expected: No results (clean)
```

#### **Rule 2: Cross-context dependencies**
```powershell
# Check identity context for imports from other contexts
Get-ChildItem -Recurse bounded-contexts\tenancy-identity\**\*.kt | Select-String "import com\.erp\.(finance|commerce|inventory|manufacturing|procurement|customer|corporate|communication|operations|bi)\."

# Repeat for each bounded context
```

#### **Rule 3: JAX-RS in platform-shared**
```powershell
Get-ChildItem -Recurse platform-shared\**\*.kt | Select-String "@Path|jakarta\.ws\.rs"
```

#### **Rule 4: JPA entities in platform-shared**
```powershell
Get-ChildItem -Recurse platform-shared\**\*.kt | Select-String "jakarta\.persistence\.(Entity|MappedSuperclass)"
```

#### **Rule 5: CDI annotations in platform-shared**
```powershell
Get-ChildItem -Recurse platform-shared\**\*.kt | Select-String "jakarta\.enterprise\.context\.(ApplicationScoped|RequestScoped)"
```

#### **Rule 6: platform-infrastructure imports in platform-shared**
```powershell
Get-ChildItem -Recurse platform-shared\**\*.kt | Select-String "import com\.erp\.infrastructure\."
```

**Outcome:** Document all violations found with file paths and line numbers.

---

## 🛠️ **Remediation Phase (Week 1-2)**

### **Priority 1: Cross-Context Coupling (High Risk)**

#### **Issue 1: platform-shared → bounded contexts** ⚠️
**Priority:** HIGH  
**Effort:** 2-4 hours  
**Owner:** TBD

**If violations found:**
- **Problem:** platform-shared importing from bounded contexts creates coupling
- **Solution:**
  1. Move domain-specific logic to the owning context
  2. Define abstractions/interfaces in `platform-shared/common-types`
  3. Implement in bounded context infrastructure layer
  
**Example Fix:**
```kotlin
// ❌ BAD: platform-shared importing identity domain
import com.erp.identity.domain.User

// ✅ GOOD: Abstract in common-types
package com.erp.shared.types
interface Principal { val id: UUID; val tenantId: UUID }

// Implement in identity-infrastructure
package com.erp.identity.infrastructure
class IdentityPrincipal : Principal { ... }
```

**Acceptance Criteria:**
- [ ] No imports from bounded contexts in platform-shared
- [ ] Abstractions defined in common-types
- [ ] ArchUnit rule passes
- [ ] All existing tests pass

---

#### **Issue 2: Cross-context direct dependencies** ⚠️
**Priority:** HIGH  
**Effort:** 4-6 hours  
**Owner:** TBD

**If violations found:**
- **Problem:** Bounded contexts importing from each other (distributed monolith)
- **Solution:**
  1. Replace direct calls with event-driven integration
  2. Use `platform-infrastructure/eventing` for async communication
  3. Define integration events in each context's public API
  
**Example Fix:**
```kotlin
// ❌ BAD: Identity calling Finance directly
import com.erp.finance.domain.Invoice
fun processPayment(invoice: Invoice) { ... }

// ✅ GOOD: Event-driven integration
// identity-domain
data class TenantActivatedEvent(val tenantId: UUID)

// finance-application (subscriber)
@ApplicationScoped
class TenantActivationHandler {
    fun on(@Observes event: TenantActivatedEvent) {
        // Create billing account asynchronously
    }
}
```

**Acceptance Criteria:**
- [ ] No cross-context imports in bounded contexts
- [ ] Integration events defined in each context
- [ ] Event handlers implemented using platform-infrastructure/eventing
- [ ] ArchUnit rule passes
- [ ] Integration tests verify event flow

---

### **Priority 2: Platform-Shared Purity (Medium Risk)**

#### **Issue 3: REST resources in platform-shared** ⚙️
**Priority:** MEDIUM  
**Effort:** 1-2 hours  
**Owner:** TBD

**If violations found:**
- **Problem:** JAX-RS annotations in platform-shared couples to HTTP transport
- **Solution:** Move REST resources to infrastructure/application layers
  
**Example Fix:**
```kotlin
// ❌ BAD: platform-shared/common-types
@Path("/health")
class HealthResource { ... }

// ✅ GOOD: Move to infrastructure layer
// bounded-contexts/*/infrastructure/rest/
@Path("/health")
class HealthResource { ... }
```

**Acceptance Criteria:**
- [ ] No `@Path` or `jakarta.ws.rs.*` in platform-shared
- [ ] REST resources moved to appropriate infrastructure modules
- [ ] ArchUnit rule passes

---

#### **Issue 4: JPA entities in platform-shared** ⚙️
**Priority:** MEDIUM  
**Effort:** 2-3 hours  
**Owner:** TBD

**If violations found:**
- **Problem:** JPA entities in platform-shared couples to persistence framework
- **Solution:** Keep DTOs/interfaces in shared, entities in infrastructure
  
**Example Fix:**
```kotlin
// ❌ BAD: platform-shared/common-types
@Entity
data class AuditLog(...)

// ✅ GOOD: DTO in shared, entity in infrastructure
// platform-shared/common-types
data class AuditLogDto(...)

// bounded-context/*/infrastructure/persistence
@Entity
@Table(name = "audit_logs")
class AuditLogEntity(...)
```

**Acceptance Criteria:**
- [ ] No `@Entity` or `jakarta.persistence.*` in platform-shared
- [ ] DTOs remain in common-types
- [ ] Entities in infrastructure with mapper functions
- [ ] ArchUnit rule passes

---

#### **Issue 5: Quarkus-specific code in platform-shared** ⚙️
**Priority:** MEDIUM  
**Effort:** 1-2 hours  
**Owner:** TBD

**If violations found:**
- **Problem:** CDI annotations in platform-shared couples to Quarkus
- **Solution:** Move services to infrastructure, keep interfaces in shared
  
**Example Fix:**
```kotlin
// ❌ BAD: platform-shared/common-observability
@ApplicationScoped
class MetricsCollector { ... }

// ✅ GOOD: Interface in shared, impl in infrastructure
// platform-shared/common-observability
interface MetricsCollector { ... }

// platform-infrastructure/monitoring
@ApplicationScoped
class QuarkusMetricsCollector : MetricsCollector { ... }
```

**Acceptance Criteria:**
- [ ] No CDI annotations in platform-shared
- [ ] Interfaces remain in common modules
- [ ] Implementations in infrastructure with DI
- [ ] ArchUnit rule passes

---

#### **Issue 6: platform-infrastructure dependency in platform-shared** ⚙️
**Priority:** MEDIUM  
**Effort:** 2-3 hours  
**Owner:** TBD

**If violations found:**
- **Problem:** Inverted layering (shared should be lower than infrastructure)
- **Solution:** Reverse dependency or extract to new common module
  
**Example Fix:**
```kotlin
// ❌ BAD: platform-shared depending on infrastructure
import com.erp.infrastructure.cqrs.Command

// ✅ GOOD: Define port in shared, impl in infrastructure
// platform-shared/common-types
interface Command

// platform-infrastructure/cqrs
class CqrsCommand : Command { ... }
```

**Acceptance Criteria:**
- [ ] No imports from platform-infrastructure in platform-shared
- [ ] Dependencies point correct direction (infra → shared)
- [ ] ArchUnit rule passes

---

## ✅ **Enforcement Phase (Week 2, End)**

### **Task: Flip to Always-On**
**Owner:** TBD  
**Effort:** 30 minutes  
**Prerequisites:** All 6 rules passing

**Steps:**
1. **Remove advisory mode in CI:**
   ```yaml
   # .github/workflows/arch-governance.yml
   # REMOVE THIS LINE:
   # continue-on-error: true
   ```

2. **Make tests always-on:**
   ```kotlin
   // tests/arch/build.gradle.kts
   tasks.named<Test>("test") {
       // REMOVE opt-in gate (make always enabled)
       enabled = true  // Was: enabled = runArch.get()
       useJUnitPlatform()
   }
   ```

3. **Update main CI workflow:**
   ```yaml
   # .github/workflows/ci.yml
   # ADD after "Architecture Tests - Platform Shared Governance"
   - name: Architecture Governance Tests (ADR-006)
     run: ./gradlew :tests:arch:test --no-daemon --stacktrace
   ```

4. **Update documentation:**
   ```markdown
   # docs/adr/ADR-006-platform-shared-governance.md
   ## Status: ENFORCED (always-on in CI)
   
   # docs/PLATFORM_SHARED_GUIDE.md
   ## Architecture Tests
   Tests run automatically in CI. Run locally: `./gradlew :tests:arch:test`
   ```

5. **Commit and push:**
   ```bash
   git add .github/workflows/ tests/arch/ docs/
   git commit -m "feat(arch): Enable always-on enforcement of ADR-006 governance rules
   
   All 6 rules now passing:
   - platform-shared isolation verified
   - Bounded context autonomy maintained  
   - Framework-agnostic shared code
   
   CI now blocks PRs with architecture violations."
   
   git push origin main
   ```

**Acceptance Criteria:**
- [ ] All 6 ArchUnit rules passing
- [ ] CI job blocking (no `continue-on-error`)
- [ ] Tests run on every PR
- [ ] Documentation updated to "ENFORCED"

---

## 📊 **Tracking & Progress**

### **Burn-Down Checklist**
- [ ] **Investigation Phase Complete** (2-4 hours)
  - [ ] Rule behavior validated
  - [ ] Violations scanned and documented
  
- [ ] **Priority 1 Complete** (6-10 hours)
  - [ ] Issue 1: platform-shared isolation ✅
  - [ ] Issue 2: Cross-context decoupling ✅
  
- [ ] **Priority 2 Complete** (6-10 hours)
  - [ ] Issue 3: REST resources moved ✅
  - [ ] Issue 4: JPA entities separated ✅
  - [ ] Issue 5: Quarkus code extracted ✅
  - [ ] Issue 6: Layer dependencies fixed ✅
  
- [ ] **Enforcement Phase Complete** (0.5 hours)
  - [ ] CI switched to blocking
  - [ ] Tests always-on
  - [ ] Docs updated

### **Total Effort:** 12-20 hours
### **Timeline:** Sprint 2 (2 weeks)
### **Team Size:** 2-3 engineers (parallel work)

---

## 🎓 **Learning & References**

### **Key Concepts**
- **Bounded Context:** Self-contained domain with clear boundaries (DDD)
- **Anti-Corruption Layer:** Protects domain from external influences
- **Event-Driven Integration:** Async communication between contexts
- **Dependency Inversion:** Depend on abstractions, not concretions

### **References**
- [ADR-003: Event-Driven Integration](../adr/ADR-003-event-driven-integration.md)
- [ADR-006: Platform-Shared Governance](../adr/ADR-006-platform-shared-governance.md)
- [Platform Shared Guide](../PLATFORM_SHARED_GUIDE.md)
- [ArchUnit Documentation](https://www.archunit.org/userguide/html/000_Index.html)

### **Similar Work**
This follows the same pattern as:
- **Error Handling Rollout:** Harden → Fix → Enforce (A+ success)
- **Production Certification:** Build gate → Fix violations → Enable (Grade 97.75/100)

---

## 🚀 **Success Metrics**

**Definition of Done:**
- ✅ All 6 ArchUnit rules passing
- ✅ Zero architecture violations in codebase
- ✅ CI enforcing rules on every PR
- ✅ Documentation updated and comprehensive
- ✅ Team trained on governance patterns

**Quality Gates:**
- 🟢 **Code Quality:** A+ (maintained)
- 🟢 **Test Coverage:** 100% rule coverage
- 🟢 **CI Status:** Green (blocking enabled)
- 🟢 **Tech Debt:** 0 architecture violations

---

**Next Steps:** Start with Task 1.1 (Rule Validation) - Estimated 2 hours
