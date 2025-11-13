# Event Versioning & Schema Registry Policy

_Last updated: 2025‑11‑13_

This document fulfils Phase 3 Task 4.6 by defining how we evolve Kafka/Redpanda event contracts and enforce compatibility through Schema Registry tooling.

---

## 1. Scope
- Topics owned by the platform (`identity.domain.events.v1`, `identity.domain.events.dlq`, future audit topics).
- Producers: Tenancy‑Identity outbox (`KafkaOutboxMessagePublisher`).
- Consumers: Any bounded context subscribing via SmallRye Reactive Messaging or Testcontainers-based integration tests.

---

## 2. Governance

| Rule | Description |
|------|-------------|
| **Registry first** | Every event type must have an Avro (preferred) or JSON Schema stored in Schema Registry before it is produced in non-dev environments. |
| **Semantic versioning** | Topic suffix (`.v1`, `.v2`) changes only when breaking changes are introduced. Non-breaking field additions stay within the same version. |
| **Compatibility mode** | Schema Registry compatibility set to `BACKWARD_TRANSITIVE`. Producers must be able to read previously written data. |
| **Change reviews** | Any schema update requires a PR touching `docs/schemas/<topic>/<version>.avsc` plus consumer-impact checklist. |
| **DLQ payloads** | Dead-letter topics reuse the same schema as the source event, wrapped in an envelope containing failure metadata. |

---

## 3. Allowed Changes (v1 topics)

| Change Type | Allowed inside `.v1`? | Notes |
|-------------|-----------------------|-------|
| Add optional field with default | ✅ | Default must be provided to maintain backwards compatibility. |
| Add required field | ❌ | Requires `.v2` topic. |
| Remove field | ❌ | Breaking change – create `.v2`. |
| Change data type | ❌ | Use new field name + migration. |
| Add enum value | ✅ | Provided consumers treat unknown values gracefully. |
| Reorder fields | ✅ (Avro) | Schema resolution handles order, but keep diffs clean. |

---

## 4. Registry Workflow
1. Author schema file under `docs/schemas/<topic>/<version>.avsc` (create directory if missing).
2. `just schema-validate` (future script) or run `./gradlew :platform-shared:common-types:test` once schema classes exist.
3. During deployment, CI step publishes schema via `rpk registry schema upload` with compatibility check.
4. Producer code references schema ID (Avro) or the canonical subject. `KafkaOutboxMessagePublisher` to be wired with Confluent serializer in Phase 3.

---

## 5. Consumer Contracts
- Consumers must pin the schema version they expect and add defensive parsing:
  - Ignore unknown fields.
  - Default missing optional fields.
- Integration tests (`bounded-contexts/.../*IntegrationTest.kt`) should load schemas from `docs/schemas` to ensure drift detection.

---

## 6. Breaking Change Procedure
1. Publish ADR describing why `.v2` topic is needed.
2. Deploy dual-write (publish to both v1 and v2) until all consumers cut over.
3. Monitor `identity.domain.events.v1` traffic; when zero consumers remain, decommission topic after 30 days.

---

## 7. References
- `docs/adr/ADR-003-event-driven-integration.md`
- `bounded-contexts/tenancy-identity/identity-infrastructure/src/main/kotlin/com.erp.identity.infrastructure/outbox/KafkaOutboxMessagePublisher.kt`
- `docker-compose-kafka.yml` – local Redpanda + Schema Registry endpoints (port 18081).
