# Finance Event Schemas

These JSON Schemas define the contract for financial-accounting outbound topics. Register each document with the platform Schema Registry before publishing events in non-dev environments.

| File | Topic | Purpose | Notes |
|------|-------|---------|-------|
| `finance.journal.events.v1.json` | `finance.journal.events.v1` | Emitted after a journal entry posts successfully | Amounts in minor units, lines capture account + direction + currency |
| `finance.period.events.v1.json` | `finance.period.events.v1` | Broadcast when an accounting period is frozen/closed/reopened | Includes old/new status for idempotent consumers |
| `finance.reconciliation.events.v1.json` | `finance.reconciliation.events.v1` | Future hook for reconciliation/audit workflows | Placeholder until reconciliation service lands |

## Governance Checklist
1. Follow [`EVENT_VERSIONING_POLICY.md`](../EVENT_VERSIONING_POLICY.md) for naming and compatibility.
2. Update `docs/PHASE4_READINESS.md` once schemas are registered in dev/test registries (see `docs/SCHEMA_REGISTRY_PLAYBOOK.md` for the publishing workflow).
3. Mirror schema changes into the `platform-shared/common-messaging` contracts module when consumers materialize.
4. Ensure Kafka topics are created with 3 partitions / 3 replicas (default data-tier settings) before enabling producers.
