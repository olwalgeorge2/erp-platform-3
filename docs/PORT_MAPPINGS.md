# ERP Platform Port Mappings

This document lists all port mappings for the ERP platform services to avoid conflicts.

## Infrastructure Services (Docker)

| Port | Service | Container | Description |
|------|---------|-----------|-------------|
| 5432 | PostgreSQL | erp-postgres | Main database for identity and other services |
| 8090 | Kafka UI | erp-kafka-ui | Kafka management interface (changed from 8080 to avoid Docker conflict) |
| 9092 | Kafka Broker | erp-kafka | Kafka external listener |
| 9093 | Kafka Controller | erp-kafka | Kafka controller (KRaft mode) |

## Application Services

| Port | Service | Module | Description |
|------|---------|--------|-------------|
| 8081 | Identity Service | identity-infrastructure | Tenancy & Identity bounded context |
| TBD | API Gateway | api-gateway | Main entry point |
| TBD | Other Services | - | To be configured |

## Port Conflicts

### Port 8080 Conflict - RESOLVED
**Previous Issue**: Both Docker Desktop and Kafka UI were trying to use port 8080.

**Resolution Applied**: Changed Kafka UI to port 8090 in `docker-compose-kafka.yml`

**Current Status**:
- Docker Desktop: Uses port 8080 (internal)
- Kafka UI: Now uses port 8090 (http://localhost:8090)
- No conflicts remaining

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

## Local Infrastructure Startup

1. Stop or reconfigure any local PostgreSQL service that already listens on port `5432` (for example `postgresql-x64-17`) so the `erp-postgres` container can bind to it.
2. Start the Docker compose stack that hosts the dev data services:
   ```bash
   docker compose -f docker-compose-kafka.yml up -d postgres kafka kafka-ui
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
