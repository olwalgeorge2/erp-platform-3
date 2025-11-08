# Platform-Shared Governance: Practical Guide

**Related:** ADR-006 Platform-Shared Governance Rules  
**Audience:** All developers working on the ERP platform  
**Last Updated:** 2025-11-06

---

## 🎯 Quick Decision Tree

When you're about to add code, ask:

```
Is this code needed by 2+ bounded contexts?
├─ NO → Put it in the specific bounded context
└─ YES → Is it a technical primitive or business concept?
    ├─ BUSINESS → Duplicate across contexts (different semantics)
    └─ TECHNICAL → Ask: Does it contain business rules?
        ├─ YES → Keep in bounded context
        └─ NO → Safe for platform-shared
```

---

## ✅ Examples: What Belongs in Platform-Shared

### 1. Pure Abstractions (common-types)

```kotlin
// ✅ GOOD: Technical contract, no business semantics
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: DomainError) : Result<Nothing>()
}

// ✅ GOOD: CQRS infrastructure pattern
interface Command
interface Query<out R>
interface CommandHandler<in C : Command, out R>

// ✅ GOOD: Event-driven pattern
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val aggregateId: String
}
```

### 2. Observability Infrastructure (common-observability)

```kotlin
// ✅ GOOD: Technical correlation tracking
data class CorrelationId(val value: String) {
    companion object {
        fun generate(): CorrelationId = 
            CorrelationId(UUID.randomUUID().toString())
    }
}

// ✅ GOOD: Structured logging abstraction
interface StructuredLogger {
    fun info(message: String, context: Map<String, Any>)
    fun error(message: String, exception: Throwable, context: Map<String, Any>)
}
```

### 3. Security Primitives (common-security)

```kotlin
// ✅ GOOD: Authentication abstraction
interface AuthenticationPrincipal {
    val userId: String
    val tenantId: String
    val roles: Set<String>
}

// ✅ GOOD: Security context (no business rules)
sealed class SecurityContext {
    data class Authenticated(val principal: AuthenticationPrincipal) : SecurityContext()
    object Anonymous : SecurityContext()
}
```

---

## ❌ Examples: What Does NOT Belong

### 1. Business Domain Models

```kotlin
// ❌ BAD: Business domain concept
data class CustomerAddress(
    val street: String,
    val city: String,
    val country: String,
    val taxJurisdiction: String  // ← Business rule!
)

// ✅ SOLUTION: Put in customer-relation/customer-shared/
// Finance will have its own TaxAddress with different rules
```

### 2. Domain-Specific Value Objects

```kotlin
// ❌ BAD: Looks generic, but has business semantics
data class Email(val value: String) {
    init {
        require(value.matches(Regex("^[^@]+@[^@]+$"))) {
            "Invalid email"
        }
        // What about marketing email vs. login email vs. invoice email?
        // Different contexts have different validation rules!
    }
}

// ✅ SOLUTION: Each context defines its own Email:
// - identity-domain: Email (login authentication rules)
// - customer-crm: MarketingEmail (opt-in, deliverability checks)
// - financial-ar: BillingEmail (invoice delivery requirements)
```

### 3. Business Logic

```kotlin
// ❌ BAD: Business calculation in shared module
object TaxCalculator {
    fun calculateTax(amount: Money, jurisdiction: String): Money {
        // Tax logic belongs in financial-management context!
    }
}

// ❌ BAD: Business validation
class PasswordPolicy {
    fun validate(password: String): Boolean {
        // Password rules belong in identity context!
    }
}
```

### 4. Utility Dumping Grounds

```kotlin
// ❌ BAD: Generic utilities
object StringUtils {
    fun capitalize(s: String) = s.replaceFirstChar { it.uppercase() }
    fun truncate(s: String, len: Int) = s.take(len)
}

// ✅ SOLUTION: Use Kotlin standard library or put in specific context
```

---

## 🤔 Gray Areas: When to Duplicate vs. Share

### Case Study: Money Value Object

```kotlin
// Option 1: Shared Money (seems logical?)
// ❌ RISKY: Different contexts have different rounding/precision rules

// Option 2: Context-specific Money
// ✅ BETTER: Each context controls its own money semantics

// financial-accounting/Money.kt
data class Money(
    val amount: BigDecimal,  // High precision (scale=4)
    val currency: Currency
) {
    // Banker's rounding for accounting
}

// commerce-ecommerce/Money.kt
data class Money(
    val amount: BigDecimal,  // Display precision (scale=2)
    val currency: Currency
) {
    // Customer-facing rounding rules
}
```

**Decision Rule:** When in doubt, **duplicate**. Coupling is more expensive than duplication.

---

## 🛠️ Refactoring Guide

### Found Business Logic in platform-shared?

**Step 1:** Identify which bounded context owns this concept
```bash
# Ask: Where is this concept used?
git grep "CustomerAddress" bounded-contexts/
```

**Step 2:** Create context-specific version
```kotlin
// bounded-contexts/customer-relation/customer-shared/src/.../CustomerAddress.kt
data class CustomerAddress(
    val street: String,
    val city: String,
    val postalCode: String,
    // Add customer-specific fields
)
```

**Step 3:** Migrate usages
```bash
# Find all imports
git grep "import com.erp.shared.types.CustomerAddress"

# Replace with context-specific import
# (Do this in batches, context by context)
```

**Step 4:** Remove from platform-shared
```bash
git rm platform-shared/common-types/.../CustomerAddress.kt
```

---

## 📋 Code Review Checklist

When reviewing PRs that touch `platform-shared`:

- [ ] **Does this contain business logic?** → Should be in a bounded context
- [ ] **Does this have domain-specific semantics?** → Should be in a bounded context
- [ ] **Is this used by only 1 context?** → Should be in that context
- [ ] **Is this a pure technical abstraction?** → OK for platform-shared
- [ ] **Does this have zero dependencies on bounded contexts?** → Required
- [ ] **Is this interface/abstract, not implementation?** → Prefer abstractions
- [ ] **Will 2+ contexts use IDENTICAL behavior?** → Required for sharing
- [ ] **Is the module still under file count limits?** → Check audit script

---

## 🔍 Monitoring & Enforcement

### Run Weekly Audit
```powershell
# Check compliance
.\scripts\audit-platform-shared.ps1

# Output:
# ✅ PASS: No issues found
# OR
# ⚠️ Warnings: 3 (review required)
```

### Run Architecture Tests
```bash
# Automated enforcement
./gradlew :tests:arch:test -PrunArchTests=true --tests "*PlatformSharedGovernanceRules*"

# These tests fail CI if violations exist
```

### Pre-Commit Hook (Optional)
```bash
# .git/hooks/pre-commit
#!/bin/bash
if git diff --cached --name-only | grep "platform-shared/"; then
    echo "⚠️  You're modifying platform-shared"
    echo "Review ADR-006 before committing"
    echo "See: docs/adr/ADR-006-platform-shared-governance.md"
fi
```

---

## 📚 Learning Resources

### Required Reading
1. **ADR-006:** [Platform-Shared Governance Rules](ADR-006-platform-shared-governance.md)
2. **Eric Evans, DDD:** Chapter 14 - Maintaining Model Integrity
3. **Vaughn Vernon, IDDD:** Chapter 3 - Context Mapping

### Team Workshop Topics
- [ ] Bounded Context principles
- [ ] Shared Kernel vs. Customer/Supplier patterns
- [ ] When to duplicate vs. share
- [ ] Real examples from our codebase

---

## ❓ FAQ

**Q: Can I share DTOs in platform-shared?**  
**A:** No. DTOs are API contracts belonging to specific bounded contexts. Each context exposes its own API.

**Q: What about Currency enum? That's standard ISO 4217.**  
**A:** Tricky. ISO 4217 is standard, but formatting/validation rules differ. Consider context-specific wrappers.

**Q: I have 3 contexts using the same validation logic.**  
**A:** If it's truly identical, consider an internal library. But verify they won't diverge (they usually do).

**Q: Should AuditInfo (createdAt, updatedAt) be shared?**  
**A:** Yes, this is a good candidate - purely technical metadata with consistent semantics.

**Q: What if I'm not sure?**  
**A:** Default to **duplication** in bounded contexts. It's easier to extract later than to untangle coupling.

---

## 🚀 Next Steps

1. **Read ADR-006** completely
2. **Run audit script** to see current state
3. **Review your current PR** against checklist
4. **Ask questions** in architecture channel
5. **Challenge assumptions** - healthy debate welcome!

---

**Remember:** The goal is **bounded context autonomy**, not code reuse. Optimize for independent evolution, not DRY.

