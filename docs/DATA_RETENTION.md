# Data Retention, Backup, and Recovery Strategy

_Scope: Tenancy‑Identity bounded context (PostgreSQL) and platform eventing (Redpanda)._  
_Last updated: 2025‑11‑13_

This document fulfils Phase 3 Task 4.4 of the roadmap by defining how long we keep operational data, how we back it up, and how we recover from failures while meeting compliance and SLA requirements.

---

## 1. Objectives
| Area | RPO | RTO | Notes |
|------|-----|-----|-------|
| Postgres (`erp_identity`) | 15 minutes (WAL archiving) | 30 minutes | Meets availability targets in `docs/SECURITY_SLA.md`. |
| Redpanda topics (`identity.domain.events.v1`, DLQs) | 5 minutes (multi-replica, ledger snapshots) | 30 minutes | Aligns with outbox replay objectives. |
| Observability data (logs/metrics) | 24 hours | 60 minutes | Managed by shared logging stack (outside this repo). |

---

## 2. PostgreSQL (Identity Service)

### 2.1 Retention Classes
| Table / Domain | Retention | Rationale |
|----------------|-----------|-----------|
| `identity_tenants`, `identity_users`, `identity_roles` | Permanent (logical deletes via `status = DELETED`) | Needed for audit/compliance; deletions happen via GDPR workflows, not automatic TTL. |
| `identity_user_roles`, metadata tables | Permanent | Linked to primary entities. |
| `identity_outbox_events` | 30 days | After Kafka confirms consumption & reconciliation, rows older than 30 days can be vacuumed. |
| `identity_audit_*` (future) | 365 days | To meet security investigation requirements. |

### 2.2 Backup Plan
1. **Nightly base backup** of the `erp_identity` volume (pg_dump or `pg_basebackup`) stored in encrypted object storage (S3/Azure Blob).
2. **WAL archiving** every 5 minutes to the same storage bucket, enabling point-in-time recovery (PITR).
3. **Integrity verification**: weekly restore drill into a staging cluster, run `./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:test -PwithContainers=true` against it.
4. **Access control**: backup bucket is write-only for the database host; restore requires ops break-glass credentials.

### 2.3 Purge / Vacuum
- Schedule a weekly job that deletes processed outbox rows older than 30 days: `DELETE FROM identity_outbox_events WHERE status='PUBLISHED' AND recorded_at < now() - interval '30 days'`.
- Run `VACUUM (VERBOSE, ANALYZE)` afterwards to keep index bloat low (ties to Task 3.1 DB tuning).

---

## 3. Redpanda / Kafka Topics

| Topic | Retention Policy | Notes |
|-------|------------------|-------|
| `identity.domain.events.v1` | 30 days, compacted future | Allows consumers to catch up after outages; long enough to replay user lifecycle events. |
| `identity.domain.events.dlq` | 90 days | Gives ops time to reprocess poison messages. |
| `identity.audit.events` (future) | 365 days | Compliance/audit feed. |

### Replication & Snapshots
- Redpanda compose stack (`docker-compose-kafka.yml`) runs with single broker for local dev; production clusters must use ≥3 brokers with replication factor = 3.
- Enable topic shadowing or object storage tiering for retention beyond local disks.

### Recovery
1. Recreate the broker from infrastructure-as-code (Kubernetes/Helm).
2. Restore topic data from S3 tier (if tiering enabled) or from snapshots (Redpanda `rpk cluster storage recover`).
3. Replay outbox events (see below) to rebuild derived state.

---

## 4. Outbox → Kafka Replay Procedure
1. Stop the identity service (to avoid new writes) and ensure Postgres is healthy.
2. Rewind consumers to the desired offset or truncate downstream read models.
3. Reset affected outbox rows: `UPDATE identity_outbox_events SET status='PENDING', failure_count=0 WHERE recorded_at >= :recovery_point`.
4. Start the service; the scheduler (`OutboxEventScheduler`) republishes the backlog, carrying original `trace-id`/`tenant-id` headers.

---

## 5. Testing & Drills
| Drill | Frequency | Owner | Notes |
|-------|-----------|-------|-------|
| Postgres restore to staging | Monthly | Platform Ops | Validate PITR, run regression tests. |
| Redpanda broker failover simulation | Quarterly | Platform Ops | Kill a broker, verify cluster heals and consumers stay caught up. |
| Outbox replay dry run | Quarterly | Identity squad | Use staging DB snapshot, ensure replays are idempotent. |

All drill outcomes must be logged in `docs/runbooks/drills.md` (create if missing) with success/failure notes.

---

## 6. Responsibilities
- **Platform Ops** – Maintains backup pipelines, storage security, restore drills.
- **Identity Squad** – Keeps outbox purge jobs running, manages Kafka DLQ reprocessing, ensures docs stay current.
- **Security** – Audits retention configuration annually to meet GDPR and SOC2 commitments.

---

## 7. References
- `docker-compose-kafka.yml` – Dev Redpanda/Postgres topology (ports 19092/5432).
- `docs/SECURITY_SLA.md` – Availability targets tied to this strategy.
- `docs/OBSERVABILITY_BASELINE.md` – Metrics used to detect retention issues (e.g., `identity.outbox.events.pending`).
