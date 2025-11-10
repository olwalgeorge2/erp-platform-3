# Identity Infrastructure Module

## 1. Purpose
1.1 Deliver the infrastructure layer for the Identity component within the Tenancy Identity area of the Tenancy Identity context.
1.2 Implement persistence, messaging, and external system integrations defined by the domain ports.
1.3 Provide configuration, transaction management, and resilience patterns required for production workloads.

## 2. Adapter Responsibilities
2.1 Implement repositories, gateways, and clients declared in `identity-domain`.
2.2 Translate between domain models and storage/transport schemas while preserving invariants.
2.3 Enforce resilience strategies (retries, circuit breaking, timeouts) aligned with platform guidance.

## 3. Operational Considerations
3.1 Manage database migrations, schema evolution, and infrastructure configuration assets.
3.2 Expose health checks, metrics, and structured logs to support observability and SRE runbooks.
3.3 Apply security, tenancy isolation, and compliance requirements when interacting with external services.

## 4. Related Modules
4.1 `identity-application/` - Leverages these adapters to service client workflows.
4.2 `identity-domain/` - Declares the ports and business contracts satisfied here.
4.3 Coordinate deployment artifacts with `deployment/` manifests and platform tooling.
# Identity Infrastructure

This module provides the REST adapters, persistence (Postgres/JPA/Flyway), messaging (Kafka/Redpanda), and supporting services for the Tenancy & Identity bounded context.

## Testing

- Default (fast unit tests only):
  - `./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:test`
  - Uses `withContainers=false` from the root `gradle.properties`.

- Enable Testcontainers-based integration tests (Postgres/Kafka):
  - `./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:test -PwithContainers=true`
  - Or set in CI: `ORG_GRADLE_PROJECT_withContainers=true`

- Naming conventions excluded when containers are off:
  - `*IntegrationTest*`, `*IT*`

- Tips
  - Ensure Docker is running for container tests.
  - Run a single IT: `./gradlew test -PwithContainers=true --tests '*AuthIntegrationTest*'`
