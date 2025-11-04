# ERP Platform

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
3.4 PostgreSQL for relational persistence and Kafka-class brokers for eventing.
3.5 OpenTelemetry, JSON logging, and metrics for full-stack observability.

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
5.1 Install prerequisites: Git, JDK 21, and Kotlin 2.2 toolchain.
5.2 Clone the repository and copy `.env.example` to `.env` with environment-specific values.
5.3 Run `./gradlew tasks` (or `gradlew.bat tasks` on Windows) to verify the toolchain.
5.4 Execute `./gradlew build` to compile modules and run the default test suite.
5.5 Review `docs/ROADMAP.md` and `docs/ARCHITECTURE.md` for project context before contributing changes.

## 6. Documentation Guide
6.1 `docs/ARCHITECTURE.md` outlines the domain model, bounded contexts, and system design.
6.2 `docs/ROADMAP.md` captures the phase-by-phase implementation plan.
6.3 `docs/BUILD_SYSTEM_UPDATE.md` records build tooling decisions and conventions.
6.4 `docs/runbooks/` contains operational guides and incident response procedures.
6.5 `docs/adr/` stores architecture decision records for major technical choices.

## 7. Contributing
7.1 Create or update ADRs for noteworthy design decisions.
7.2 Follow the coding standards enforced by ktlint and the project’s static analysis.
7.3 Keep bounded contexts autonomous; share functionality through `platform-shared/` only when necessary.
7.4 Add tests alongside features and update relevant runbooks or documentation.
7.5 Open a pull request with a concise summary, testing notes, and links to related tickets.
