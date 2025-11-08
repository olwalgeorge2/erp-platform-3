# Sprint 3 – Architecture Test Coverage Expansion (ADR-006)

Goal: Expand ArchUnit coverage beyond identity + platform-shared, while keeping CI advisory. Wire one bounded context at a time to minimize risk and create fast feedback.

## Scope (Sprint 3)
- Wire these contexts into :tests:arch testRuntimeOnly classpath:
  1) financial-management
  2) commerce
  3) inventory-management
- Remove allowEmptyShould(true) for each context as it becomes wired
- Fix any discovered violations per ADR-006/Sprint 2 pattern

## Step-by-step (repeat per context)
1) Add runtime classpath wiring
   - File: tests/arch/build.gradle.kts
   - Example (finance):
     - testRuntimeOnly(project(":bounded-contexts:financial-management:financial-accounting:accounting-domain"))
     - testRuntimeOnly(project(":bounded-contexts:financial-management:financial-accounting:accounting-application"))
     - testRuntimeOnly(project(":bounded-contexts:financial-management:financial-accounting:accounting-infrastructure"))
   - Adjust module names if different (use `./gradlew projects`).

2) Remove empty-guard for the wired context
   - File: tests/arch/src/test/kotlin/com/erp/tests/arch/PlatformSharedGovernanceRules.kt
   - Remove `.allowEmptyShould(true)` for that context’s rule.

3) Run tests
   - `./gradlew :tests:arch:test -PrunArchTests=true --tests "*PlatformSharedGovernanceRules*"`
   - Review: tests/arch/build/reports/tests/test/index.html

4) If violations exist
   - Create a checklist and PR(s) to remediate:
     - Replace cross-context imports with integration ports/events (ADR‑003)
     - Move REST/JPA/CDI from shared to infra modules
     - Remove infra dependencies from shared; introduce abstractions in shared

5) Acceptance criteria (per context)
   - Arch tests pass locally for PlatformSharedGovernanceRules
   - No new ktlint/style regressions
   - CI advisory job is green (reports uploaded)

## Execution order & estimate
- Phase 1 (Day 1–2): financial-management
  - Est: 0.5–1h wiring + 2–4h fixes (if any)
- Phase 2 (Day 2–3): commerce
  - Est: 0.5–1h wiring + 2–4h fixes (if any)
- Phase 3 (Day 3–4): inventory-management
  - Est: 0.5–1h wiring + 2–4h fixes (if any)

Total Sprint 3 estimate: 1.5–3h wiring + 6–12h fixes = 8–15h (parallelizable across 2–3 engineers).

## Risk management
- Keep CI advisory while expanding
- Wire one context at a time; short PRs; fast review
- If violations are large, freeze baseline for that context and schedule refactor tasks

## Flip to enforcement (Sprint 4+)
Prereqs:
- All contexts wired
- All rules passing locally + CI
- Team comfortable running arch tests locally

Actions:
- .github/workflows/ci.yml
  - Remove `-PrunArchTests=true` and `continue-on-error: true` for ADR‑006 steps
- tests/arch/build.gradle.kts
  - Ensure tests are enabled by default
- ADR‑006
  - Update status to ENFORCED and remove advisory notes

---

Owner: Architecture Guild / Senior Eng
Status: Planned (Sprint 3)
