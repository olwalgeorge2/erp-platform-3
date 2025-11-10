# ERP Platform Port Mappings

This document lists all port mappings for the ERP platform services to avoid conflicts.

## Infrastructure Services (Docker)

| Port | Service | Container | Description |
|------|---------|-----------|-------------|
| 5432 | PostgreSQL | erp-postgres | Main database for identity and other services |
| 8090 | Redpanda Console | erp-redpanda-console | Redpanda management interface (Kafka-compatible) |
| 19092 | Redpanda Kafka API | erp-redpanda | External Kafka protocol listener |
| 18081 | Schema Registry | erp-redpanda | Built-in Schema Registry (Confluent-compatible) |
| 18082 | HTTP Proxy | erp-redpanda | REST API for Kafka operations |
| 19644 | Admin API | erp-redpanda | Redpanda admin and metrics endpoint |
| 9092 | Redpanda Internal | erp-redpanda | In-cluster Kafka listener for other containers |

## Application Services

| Port | Service | Module | Description |
|------|---------|--------|-------------|
| 8181 (dev) | Identity Service | identity-infrastructure | Tenancy & Identity bounded context (tests use ephemeral port) |
| TBD | API Gateway | api-gateway | Main entry point |
| TBD | Other Services | - | To be configured |

## Port Conflicts

### Port 8080 Conflict - RESOLVED
**Previous Issue**: Both Docker Desktop and Kafka UI were trying to use port 8080.

**Resolution Applied**: Changed Kafka UI to port 8090 in `docker-compose-kafka.yml`

**Current Status**:
- Docker Desktop: Uses port 8080 (internal)
- Redpanda Console: Uses port 8090 (http://localhost:8090)
- No conflicts remaining

### Migration from Kafka to Redpanda
**Changes Applied**: 
- Replaced Apache Kafka with Redpanda (10x faster, simpler, Kafka-compatible)
- External port changed from 9092 → 19092 to avoid conflicts
- Built-in Schema Registry (18081) and HTTP Proxy (18082)
- See `docs/REDPANDA_MIGRATION.md` for complete details

## Database Credentials

### Development Environment
- **Host**: localhost:5432
- **Database**: erp_identity
- **Username**: erp_user
- **Password**: erp_pass

### Connection Strings
- **JDBC**: `jdbc:postgresql://localhost:5432/erp_identity`
- **Quarkus**: See `application.yaml` in each service

## Notes

- All ports can be overridden using environment variables
- In production, use proper secrets management
- Ensure PostgreSQL is running before starting application services
- Redpanda bootstrap servers (Kafka-compatible):
  - Host access: set `KAFKA_BOOTSTRAP_SERVERS=localhost:19092` (note: port changed to 19092)
  - In-cluster (Docker): set `KAFKA_BOOTSTRAP_SERVERS=redpanda:9092`
- Dev server script auto-selects a free port starting at 8181: `scripts/dev-identity.ps1`

## Local Infrastructure Startup

1. Stop or reconfigure any local PostgreSQL service that already listens on port `5432` (for example `postgresql-x64-17`) so the `erp-postgres` container can bind to it.
2. Start the Docker compose stack that hosts the dev data services:
   ```bash
   docker compose -f docker-compose-kafka.yml up -d postgres redpanda redpanda-console
   ```
3. Verify the credentials with the helper script:
   ```powershell
   .\test-db-connection.ps1
   ```
   The script launches a temporary `postgres:16-alpine` client container and confirms that `erp_user / erp_pass` can connect to `erp_identity`.
4. Once `docker ps --format "table {{.Names}}\t{{.Status}}"` shows `erp-postgres` as `healthy`, start the identity service:
   ```bash
   ./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:quarkusDev
   ```

> Tip: Keep Docker Desktop running while `quarkusDev` is active so Flyway can reuse the same PostgreSQL instance during live reload.
> 
> **Note**: We now use Redpanda instead of Apache Kafka. Redpanda is 10x faster, simpler to operate, and 100% Kafka-compatible. See `docs/REDPANDA_MIGRATION.md` for complete migration details.
