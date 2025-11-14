# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records documenting significant architectural choices for the ERP platform.

## Active ADRs

| ADR | Title | Status | Date | Impact |
|-----|-------|--------|------|--------|
| [ADR-001](ADR-001-modular-cqrs.md) | Modular CQRS Implementation | Active | - | High |
| [ADR-002](ADR-002-database-per-context.md) | Database Per Bounded Context | Active | - | High |
| [ADR-003](ADR-003-event-driven-integration.md) | Event-Driven Integration Between Contexts | Active | - | High |
| [ADR-004](ADR-004-api-gateway-pattern.md) | API Gateway Pattern | Active | - | Medium |
| [ADR-005](ADR-005-multi-tenancy-isolation.md) | Multi-Tenancy Data Isolation Strategy | Active | - | High |
| [ADR-006](ADR-006-platform-shared-governance.md) | Platform-Shared Governance Rules | **Accepted (CI Enforced)** | 2025-11-06 | **Critical** |
| [ADR-007](ADR-007-authn-authz-strategy.md) | Authentication & Authorization Strategy | Draft | 2025-11-08 | High |
| [ADR-008](ADR-008-cicd-network-resilience.md) | CI/CD Pipeline Architecture & Network Resilience | **Accepted (Implemented)** | 2025-11-09 | **High** |
| [ADR-009](ADR-009-financial-accounting-domain.md) | Financial Accounting Domain Strategy | Accepted | 2025-11-13 | High |

## What is an ADR?

An Architecture Decision Record (ADR) captures an important architectural decision made along with its context and consequences.

### When to Create an ADR

Create an ADR when:
- Making decisions with **system-wide impact**
- Choosing between **significant alternatives**
- Establishing **patterns** or **conventions**
- Making **irreversible** or **costly-to-change** decisions
- Resolving **contentious** technical debates

### ADR Template

```markdown
# ADR-XXX: [Title]

**Status:** [Proposed | Accepted | Deprecated | Superseded]
**Date:** YYYY-MM-DD
**Context:** [Which phase/component this affects]

## Decision
[What is the decision?]

## Context
[What is the issue we're trying to solve?]

## Alternatives Considered
[What other options did we evaluate?]

## Consequences
### Positive
- [Benefits]

### Negative
- [Drawbacks]

### Neutral
- [Trade-offs]

## Compliance
[How will we enforce this decision?]

## Related ADRs
- [Links to related ADRs]
```

## ADR Lifecycle

```
Proposed → Accepted → [Active]
                    ↓
         Deprecated | Superseded
```

- **Proposed:** Under discussion, not yet enforced
- **Accepted:** Approved by team, being implemented
- **Active:** Fully implemented and enforced
- **Deprecated:** No longer recommended, but not removed
- **Superseded:** Replaced by a newer ADR

## Critical ADRs Requiring Active Enforcement

### 🔴 ADR-006: Platform-Shared Governance (Accepted - CI Enforced)

**Why Critical:** Prevents the "distributed monolith" anti-pattern that would destroy bounded context autonomy.

**Enforcement Mechanisms:**
1. ✅ **ArchUnit Tests:** `tests/arch/PlatformSharedGovernanceRules.kt` (automated in CI)
2. ✅ **CI Pipeline:** Fails builds on violations (`.github/workflows/ci.yml`)
3. ✅ **Weekly Audit:** Automated monitoring every Monday (`.github/workflows/governance-audit.yml`)
4. ✅ **Local Script:** `./scripts/audit-platform-shared.ps1` for dev feedback
5. 📋 **Code Review Checklist:** Manual review for platform-shared PRs

**Current Status:**
- ArchUnit tests created ✅
- CI integration complete ✅
- Weekly audit workflow active ✅
- Documentation complete ✅
- Team training: Pending 📋

**Next Actions:**
1. ~~Run architecture tests in CI pipeline~~ ✅ **DONE**
2. ~~Create weekly audit workflow~~ ✅ **DONE**
3. Schedule team workshop on bounded context principles 📋
4. Add pre-commit hook to warn when modifying platform-shared 📋

## References

- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions) by Michael Nygard
- *Domain-Driven Design* by Eric Evans
- *Building Evolutionary Architectures* by Neal Ford, Rebecca Parsons, Patrick Kua
