# ERP Platform

[![CI](https://github.com/olwalgeorge2/erp-platform-3/actions/workflows/ci.yml/badge.svg)](https://github.com/olwalgeorge2/erp-platform-3/actions/workflows/ci.yml)
[![Nightly Build](https://github.com/olwalgeorge2/erp-platform-3/actions/workflows/nightly.yml/badge.svg)](https://github.com/olwalgeorge2/erp-platform-3/actions/workflows/nightly.yml)

## 1. Overview
This repository hosts a modular, cloud-native Enterprise Resource Planning (ERP) platform built with Kotlin and Quarkus. The system embraces Domain-Driven Design (DDD), hexagonal architecture, and microservices principles to deliver a scalable, multi-tenant solution composed of independent bounded contexts.

## 2. Key Capabilities
2.1 Modular bounded contexts that map to core ERP domains (finance, inventory, procurement, etc.).
2.2 Shared platform services for identity, messaging, CQRS, and observability.
2.3 API gateway that centralizes routing, security, and cross-cutting concerns.
2.4 Deployment tooling for containerized workloads and automated infrastructure provisioning.

## 3. Technology Stack
3.1 Kotlin 2.2 and Java 21 as the primary language and runtime.
3.2 Quarkus 3.x for reactive, cloud-native microservices.
3.3 Gradle 8 with Kotlin DSL for builds and dependency management.
3.4 PostgreSQL for relational persistence and Apache Kafka (KRaft mode) for eventing.
3.5 OpenTelemetry, JSON logging, and metrics for full-stack observability.

See `docs/BUILD_SYSTEM_UPDATE.md` for detailed dependency versions, tooling rationale, and upgrade guidance.

## 4. Repository Layout
4.1 `api-gateway/` - Edge service for request routing and composition.
4.2 `bounded-contexts/` - Domain services grouped by bounded context.
4.3 `deployment/` - Infrastructure-as-code, manifests, and release tooling.
4.4 `docs/` - Architecture, roadmap, runbooks, and ADRs.
4.5 `platform-infrastructure/` - Shared infrastructure libraries (CQRS, eventing, etc.).
4.6 `platform-shared/` - Shared domain types and utilities.
4.7 `portal/` - Client-facing applications and UI integrations.
4.8 `tests/` - Cross-context test suites and harnesses.
4.9 `build.gradle.kts` - Root Gradle build script.
4.10 `settings.gradle.kts` - Gradle settings and project wiring.

## 5. Getting Started

### Prerequisites
- **JDK 21** (Temurin or similar)
- **Kotlin 2.2+** toolchain
- **Git** for version control
- **Docker** (optional, for local infrastructure)

### Quick Start
```bash
# 1. Clone the repository
git clone https://github.com/olwalgeorge2/erp-platform-3.git
cd erp-platform-3

# 2. Copy environment configuration
cp .env.example .env
# Edit .env with your local values

# 3. Verify toolchain
./gradlew tasks              # Linux/Mac
.\gradlew.bat tasks          # Windows

# 4. Build all modules
./gradlew build

# 5. Run tests
./gradlew test

# 6. Check code style
./gradlew ktlintCheck

# 7. Auto-format code
./gradlew ktlintFormat
```

### Build Commands
| Command | Description |
|---------|-------------|
| `./gradlew build` | Compile all modules and run tests |
| `./gradlew test` | Run unit tests across all modules |
| `./gradlew check` | Run all verification tasks |
| `./gradlew ktlintCheck` | Check Kotlin code style |
| `./gradlew ktlintFormat` | Auto-format Kotlin code |
| `./gradlew projects` | List all modules |
| `./gradlew build --scan` | Build with Gradle Build Scan |
| `./scripts/audit-platform-shared.ps1` | Audit platform-shared governance (ADR-006) |

### Review Documentation
- `docs/ROADMAP.md` - Implementation phases and milestones
- `docs/ARCHITECTURE.md` - System design and bounded contexts
- `docs/BUILD_SYSTEM_UPDATE.md` - Build tooling and conventions
- `docs/PLATFORM_SHARED_GUIDE.md` - Governance rules for shared modules (Critical!)
- `docs/adr/` - Architecture Decision Records


## 6. Documentation Guide
6.1 `docs/ARCHITECTURE.md` outlines the domain model, bounded contexts, and system design.
6.2 `docs/ROADMAP.md` captures the phase-by-phase implementation plan.
6.3 `docs/BUILD_SYSTEM_UPDATE.md` records build tooling decisions and conventions.
6.4 `docs/runbooks/` contains operational guides and incident response procedures.
6.5 `docs/adr/` stores architecture decision records for major technical choices.
6.6 `bounded-contexts/README.md` indexes every bounded context and links to service-level documentation.
6.7 `bounded-contexts/tenancy-identity/README.md` describes the platform identity and tenancy services.
6.8 `bounded-contexts/financial-management/README.md` covers accounting, AP/AR, and shared finance modules.
6.9 `bounded-contexts/commerce/README.md` summarizes omnichannel commerce capabilities and shared kernels.
6.10 `bounded-contexts/communication-hub/README.md` highlights messaging orchestration and channel adapters.

## 7. Contributing

### Development Workflow
1. Create a feature branch from `main`
2. Make your changes following the coding standards
3. Run `./gradlew ktlintFormat` to auto-format code
4. Run `./gradlew check` to verify all tests pass
5. Commit with clear, descriptive messages
6. Push and create a Pull Request

### Code Standards
- Follow ktlint rules (enforced via `.editorconfig`)
- Write unit tests for new functionality
- Update documentation for architectural changes
- Create ADRs for significant design decisions
- Keep bounded contexts autonomous
- Share functionality through `platform-shared/` only when necessary

### CI/CD Pipeline
All pull requests trigger automated checks:
- ✅ Kotlin code style verification (ktlint)
- ✅ Unit and integration tests
- ✅ Build verification across all modules
- ✅ Architecture tests (boundary enforcement)
- ✅ Static analysis and security checks

See `.github/workflows/ci.yml` for the complete pipeline configuration.

7.4 Add tests alongside features and update relevant runbooks or documentation.
7.5 Open a pull request with a concise summary, testing notes, and links to related tickets.
