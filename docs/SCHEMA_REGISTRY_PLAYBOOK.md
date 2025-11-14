# Schema Registry Playbook (Finance Slice)

_Last updated: 2025-11-14_

This playbook documents how we validate and publish JSON Schemas for the finance bounded context, following the policies from ADR‑003 (event‑driven integration) and ADR‑007 (hybrid EDA with transactional outbox). The same workflow should be reused by future contexts.

---

## 1. Artifacts & Conventions

- Schemas live under `docs/schemas/<context>/<stream>.<event>.v<version>.json`
  - Finance slice: `docs/schemas/finance/finance.journal.events.v1.json`, `finance.period.events.v1.json`, `finance.reconciliation.events.v1.json`
- Each schema must include:
  - `$id` and `$schema` fields
  - `eventType`, `version`, `recordedAt`, and context-specific payload sections (see existing files)
- Topic naming: `<context>.<stream>.events.v<version>` (matches ADR‑003 §4)
- Registry subjects follow `<topic>-value` (Kafka default)

---

## 2. Validation (Local)

1. Install tooling (one-time):
   ```bash
   npm install -g ajv-cli
   ```
2. Run the validation script from repo root:
   ```bash
   ajv validate -s docs/schemas/finance/finance.journal.events.v1.json \
     -d docs/schemas/finance/examples/journal/*.json --spec=draft2020
   ```
3. Ensure CI also validates (future: add Gradle task / GitHub action)

---

## 3. Publishing to Schema Registry

Assuming Confluent-compatible registry at `${SCHEMA_REGISTRY_URL}` (basic auth optional).

### 3.1 Register / Update Schema

- Preferred: run the helper script (wraps curl + jq):
  ```bash
  SCHEMA_REGISTRY_URL=http://localhost:8085 \
    SCHEMA_REGISTRY_BASIC_AUTH="$(printf 'user:pass' | base64)" \
    ./scripts/register_finance_schemas.sh
  ```
  (Leave `SCHEMA_REGISTRY_BASIC_AUTH` unset if auth is disabled.)

- Manual curl (if needed):
  ```bash
  curl -X POST \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    "${SCHEMA_REGISTRY_URL}/subjects/finance.journal.events.v1-value/versions" \
    -d @docs/schemas/finance/finance.journal.events.v1.json
  ```
  Repeat for the other subjects.

### 3.2 Verify

```bash
# List subjects
curl "${SCHEMA_REGISTRY_URL}/subjects" | jq

# Inspect latest version
curl "${SCHEMA_REGISTRY_URL}/subjects/finance.journal.events.v1-value/versions/latest" | jq
```

Capture the returned `id` in release notes (helps consumers pin schema).

---

## 4. Runtime Wiring

1. Finance service (`accounting-infrastructure`) already emits Kafka events via transactional outbox + `KafkaFinanceEventPublisher`.
2. Add the following config (example) to point to registry from finance service:
   ```yaml
   mp:
     messaging:
       outgoing:
         finance-journal-events-out:
           schema.registry.url: ${SCHEMA_REGISTRY_URL}
           value.serializer: io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
   ```
   (actual serializer selection depends on platform decision—see ADR‑003).
3. Consumers should validate incoming payloads against the same registry entry (ensure compatibility mode is `BACKWARD`).

---

## 5. Checklist (per schema change)

- [ ] Update JSON schema file + README under `docs/schemas/finance/`
- [ ] Validate locally via `ajv` (or Gradle task once added)
- [ ] Publish to registry (`POST /subjects/.../versions`)
- [ ] Record schema ID + version in release notes / CHANGELOG
- [ ] Update downstream consumers if breaking change (follow ADR‑003 compatibility rules)
- [ ] Update PHASE4 readiness matrix if this unblocks a milestone

---

## 6. References

- ADR‑003 – Event Driven Integration Policy
- ADR‑007 – Hybrid Event-Driven Architecture Policy
- ADR‑009 – Financial Accounting Domain
- `docs/schemas/finance/README.md` – schema field definitions
- `docs/PHASE4_READINESS.md` – readiness tracker referencing registry integration
