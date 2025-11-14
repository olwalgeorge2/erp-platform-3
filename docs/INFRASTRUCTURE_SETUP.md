# ERP Platform - Infrastructure Setup Guide

## Overview

The ERP platform uses a hybrid infrastructure approach:
- **Shared Infrastructure**: PostgreSQL, Redis, Kafka (Redpanda) for Identity and Gateway
- **Isolated Testcontainers**: Ephemeral PostgreSQL and Kafka for Finance service (dev mode)

## Infrastructure Components

### Shared Services (Docker Compose)

| Service | Container Name | Port(s) | Purpose | Credentials |
|---------|---------------|---------|---------|-------------|
| PostgreSQL | `erp-postgres` | 5432 | Identity & shared data | `erp_user` / `erp_pass` |
| Redis | `erp-redis` | 6379 | Session store, caching | No auth (dev mode) |
| Redpanda (Kafka) | `erp-redpanda` | 19092 (external)<br>9092 (internal) | Event streaming | No auth (dev mode) |
| Redpanda Console | `erp-redpanda-console` | 8090 | Kafka UI | No auth |

### Application Services

| Service | Port | Database | Kafka | Notes |
|---------|------|----------|-------|-------|
| **Tenancy-Identity** | 8081 | Shared PostgreSQL<br>(`erp-postgres`) | Shared Redpanda<br>(`erp-redpanda`) | Uses `erp_identity` database |
| **API Gateway** | 8080 | None | Shared Redpanda<br>(`erp-redpanda`) | Stateless, uses Redis for sessions |
| **Finance** | 8082 | **Testcontainers**<br>(ephemeral PostgreSQL) | **Testcontainers**<br>(ephemeral Redpanda) | Isolated for testing<br>Auto-configured ports |

## Configuration Details

### Identity Service (Tenancy-Identity)

**Database Configuration:**
```properties
# bounded-contexts/tenancy-identity/identity-application/src/main/resources/application.properties
quarkus.http.port=8081
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=erp_user
quarkus.datasource.password=erp_pass
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/erp_identity
```

**Dependencies:**
- ✅ Requires `erp-postgres` container running
- ✅ Requires `erp-redpanda` container running
- ✅ Database schema managed by Flyway (auto-migrate)

**Startup:** `./gradlew :bounded-contexts:tenancy-identity:identity-application:quarkusDev`

---

### API Gateway

**Configuration:**
```yaml
# api-gateway/src/main/resources/application.yml
quarkus:
  http:
    port: 8080
  redis:
    hosts: redis://localhost:6379
  kafka:
    bootstrap-servers: localhost:19092
```

**Dependencies:**
- ✅ Requires `erp-redis` container running
- ✅ Requires `erp-redpanda` container running
- ✅ Health checks fail gracefully if backends are down

**Startup:** `./gradlew :api-gateway:quarkusDev`

---

### Finance Service

**Database Configuration:**
```yaml
# bounded-contexts/financial-management/financial-accounting/accounting-infrastructure/src/main/resources/application.yml
quarkus:
  http:
    port: ${FINANCE_HTTP_PORT:8082}
  datasource:
    db-kind: postgresql
    username: ${FINANCE_DB_USERNAME:postgres}
    password: ${FINANCE_DB_PASSWORD:postgres}
    jdbc:
      url: ${FINANCE_DB_JDBC_URL:jdbc:postgresql://127.0.0.1:5432/test}
```

**Quarkus DevServices (Testcontainers):**
- ✅ **Auto-starts** ephemeral PostgreSQL container (port assigned dynamically)
- ✅ **Auto-starts** ephemeral Redpanda container (port assigned dynamically)
- ✅ **Auto-configures** JDBC URL, username, password
- ✅ Database schema managed by Flyway (auto-migrate to `financial_accounting` schema)
- ⚠️ **Startup time**: 60-90 seconds (container initialization)

**Environment Variables (Optional):**
- `FINANCE_HTTP_PORT=8082` - HTTP port (default: 8082)
- Database credentials auto-configured by DevServices (ignore manual settings)

**Startup:** `./gradlew :bounded-contexts:financial-management:financial-accounting:accounting-infrastructure:quarkusDev`

## Management Scripts

### Quick Start

```powershell
# 1. Start infrastructure (PostgreSQL, Redis, Kafka)
.\scripts\start-infrastructure.ps1

# 2. Start all application services (separate windows)
.\scripts\start-all-services.ps1

# 3. Check status
.\scripts\check-status.ps1

# 4. Stop everything
.\scripts\stop-all-services.ps1
```

### Script Details

#### `start-infrastructure.ps1`

**What it does:**
- Starts `erp-postgres`, `erp-redis`, `erp-redpanda`, `erp-redpanda-console`
- Waits for PostgreSQL health check (30 attempts × 2s)
- Displays connection information

**Prerequisites:**
- Docker Desktop running
- No conflicting services on ports: 5432, 6379, 19092, 8090

#### `start-all-services.ps1`

**What it does:**
- Verifies infrastructure is running
- Checks for port conflicts (8080, 8081, 8082)
- Starts 3 services in **external PowerShell windows** with color-coded titles:
  - 🔵 **Cyan**: Tenancy-Identity (8081)
  - 🟢 **Green**: API Gateway (8080)
  - 🟡 **Yellow**: Finance (8082)

**Environment Variables Set:**
```powershell
# Identity
$env:QUARKUS_HTTP_PORT='8081'
$env:QUARKUS_ANALYTICS_DISABLED='true'

# Gateway
$env:QUARKUS_HTTP_PORT='8080'
$env:QUARKUS_ANALYTICS_DISABLED='true'

# Finance
$env:FINANCE_HTTP_PORT='8082'
$env:QUARKUS_ANALYTICS_DISABLED='true'
# Database credentials managed by Testcontainers (no manual override)
```

#### `check-status.ps1`

**What it checks:**
- Docker status
- Infrastructure container health (PostgreSQL, Redis, Redpanda)
- Application ports (8080, 8081, 8082)
- HTTP health endpoints

#### `stop-all-services.ps1`

**What it does:**
- Stops all Java/Gradle processes
- Stops Docker infrastructure containers
- Cleans up Testcontainers (Finance ephemeral containers)
- Verifies ports are released

## Troubleshooting

### PostgreSQL Connection Failures

**Problem:** `FATAL: role "postgres" does not exist` or `password authentication failed`

**For Identity Service:**
- ✅ Ensure `erp-postgres` container is running
- ✅ Credentials must be `erp_user` / `erp_pass`
- ✅ Database must be `erp_identity`

**For Finance Service:**
- ✅ Let Testcontainers auto-configure (don't set `FINANCE_DB_USERNAME`)
- ✅ Wait 60-90 seconds for Testcontainers startup
- ✅ Check logs for `Dev Services for Kafka started` message

### Port Conflicts

**Problem:** `Port already bound: 8080/8081/8082`

**Solution:**
```powershell
# Find process using port
Get-NetTCPConnection -LocalPort 8082 | Select OwningProcess

# Kill process
Stop-Process -Id <PID> -Force

# Or stop all services
.\scripts\stop-all-services.ps1
```

### Redis Connection Failures (Gateway)

**Problem:** `Connection refused: localhost/127.0.0.1:6379`

**Solution:**
```powershell
# Check if Redis is running
docker ps --filter "name=erp-redis"

# Start Redis if missing
docker compose -f docker-compose-kafka.yml up -d redis

# Verify connectivity
docker exec erp-redis redis-cli ping
# Should output: PONG
```

### Finance Service Slow Startup

**Expected behavior:** Finance takes 60-90 seconds to start due to Testcontainers

**Monitor progress:**
- Look for `Testcontainers version: 1.21.3` in yellow Finance window
- Wait for `Creating container for image: testcontainers/postgres:16-alpine`
- Wait for `Dev Services for Kafka started`
- Finally: `Listening on: http://0.0.0.0:8082`

## Architecture Decisions

### Why Shared PostgreSQL for Identity?

- **Multi-tenancy**: Identity service manages tenant data that needs persistence
- **Shared state**: User sessions, roles, permissions across services
- **Migration stability**: Flyway migrations need consistent database

### Why Testcontainers for Finance?

- **Isolation**: Financial data remains isolated during development
- **Clean slate**: Each dev session starts with fresh database
- **Testing**: Same setup used for integration tests
- **Schema independence**: Financial schema doesn't conflict with identity

### Why Redis for Gateway?

- **Session storage**: Distributed session management
- **Rate limiting**: Shared state for API throttling
- **Caching**: Future: cache backend responses

## Health Check Endpoints

| Service | Liveness | Readiness | Metrics |
|---------|----------|-----------|---------|
| Identity | `/q/health/live` | `/q/health/ready` | `/q/metrics` |
| Gateway | `/q/health/live` | `/q/health/ready` | `/q/metrics` |
| Finance | `/q/health/live` | `/q/health/ready` | `/q/metrics` |

## Swagger UI

| Service | URL |
|---------|-----|
| Identity | http://localhost:8081/q/swagger-ui |
| Gateway | http://localhost:8080/q/swagger-ui |
| Finance | http://localhost:8082/q/swagger-ui |

## Next Steps

1. ✅ **Infrastructure is running** - All containers healthy
2. ✅ **Services are starting** - Check colored PowerShell windows
3. ⏳ **Wait for Finance** - 60-90 seconds for Testcontainers
4. ✅ **Verify health** - Run `.\scripts\check-status.ps1`
5. 🧪 **Test endpoints** - Use `docs/rest/finance-accounting.rest`

## See Also

- [scripts/README.md](../scripts/README.md) - Script usage guide
- [PORT_MAPPINGS.md](PORT_MAPPINGS.md) - Port allocation reference
- [SERVICE_STARTUP_GUIDE.md](SERVICE_STARTUP_GUIDE.md) - Detailed startup procedures
- [FINANCE_LIVE_TEST_GUIDE.md](FINANCE_LIVE_TEST_GUIDE.md) - Finance testing guide
