# Development Scripts Usage Guide

---

## 🚀 Quick Start - Run All Services

### Complete Platform Startup (Recommended)

```powershell
# Step 1: Start infrastructure (PostgreSQL, Kafka, Redis)
.\scripts\start-infrastructure.ps1

# Step 2: Wait 15 seconds, then start applications
.\scripts\start-all-services.ps1

# Step 3: Check everything is healthy
.\scripts\check-status.ps1
```

**Three external PowerShell windows will open:**
- 🔵 **Tenancy-Identity** (Cyan) - Port 8081
- 🟢 **API Gateway** (Green) - Port 8080
- 🟡 **Finance** (Yellow) - Port 8082

### Quick Stop

```powershell
.\scripts\stop-all-services.ps1
```

---

## 📜 Management Scripts Reference

### `start-infrastructure.ps1` ⭐
**Purpose:** Start Docker infrastructure services

**Services Started:**
- PostgreSQL (5432) - `erp-postgres`
- Redpanda/Kafka (19092) - `erp-redpanda`
- Redpanda Console (8090) - `erp-redpanda-console`
- Redis (6379) - `erp-redis`

**What it does:**
1. ✅ Verifies Docker is running
2. ✅ Starts containers via `docker-compose-kafka.yml`
3. ✅ Polls PostgreSQL health (30 attempts × 2s = 60s timeout)
4. ✅ Displays connection information

**Output:**
```
🚀 Starting ERP Platform Infrastructure...
✅ Docker is running
✅ Starting containers...
✅ PostgreSQL is healthy!

Services running:
- PostgreSQL: localhost:5432 (user: erp_user, password: erp_pass)
- Redpanda: localhost:19092
- Redpanda Console: http://localhost:8090
```

**Usage:**
```powershell
.\scripts\start-infrastructure.ps1
```

---

### `start-all-services.ps1` ⭐
**Purpose:** Start all application services in external PowerShell windows

**Pre-flight Checks:**
- ✅ Verifies infrastructure containers are running
- ✅ Checks for port conflicts (8080, 8081, 8082)

**Services Started:**
1. **Tenancy-Identity** (port 8081) - Authentication & authorization
2. **API Gateway** (port 8080) - Main entry point
3. **Finance** (port 8082) - Financial accounting

**What it does:**
- Launches each service in a separate external PowerShell window
- Sets color-coded window titles for easy identification
- Configures environment variables (DB credentials for Finance)
- Runs Quarkus in dev mode (`./gradlew quarkusDev`)

**Output:**
```
🚀 Starting ERP Platform Services...
✅ Infrastructure is ready
✅ Checking ports...
✅ Starting Tenancy-Identity (8081)...
✅ Starting API Gateway (8080)...
✅ Starting Finance (8082)...

All services started! Check the colored PowerShell windows.

Service URLs:
- Tenancy-Identity: http://localhost:8081/q/health
- API Gateway: http://localhost:8080/q/health
- Finance: http://localhost:8082/q/health

Wait 30-60s for services to start, then run:
  .\scripts\check-status.ps1
```

**Usage:**
```powershell
.\scripts\start-all-services.ps1
```

---

### `check-status.ps1` ⭐
**Purpose:** Verify health of infrastructure and applications

**What it checks:**
1. Docker status
2. Infrastructure containers (PostgreSQL, Redpanda)
3. Application ports (8080, 8081, 8082)
4. HTTP health endpoints

**Output:**
```
🔍 ERP Platform Status Check

Docker Status: ✅ Running

Infrastructure Containers:
✅ erp-postgres       (healthy)
✅ erp-redpanda       (healthy)

Application Ports:
✅ 8081 (Tenancy-Identity)
✅ 8080 (API Gateway)
✅ 8082 (Finance)

Health Endpoints:
✅ Tenancy-Identity - UP
✅ API Gateway - UP
✅ Finance - UP

All services healthy! 🎉
```

**Usage:**
```powershell
.\scripts\check-status.ps1
```

---

### `stop-all-services.ps1` ⭐
**Purpose:** Stop all services (infrastructure + applications)

**What it does:**
1. ✅ Stops Java/Gradle processes (identity, gateway, finance)
2. ✅ Stops Docker infrastructure containers
3. ✅ Cleans Testcontainers (finance uses ephemeral PostgreSQL)
4. ✅ Verifies ports are released

**Output:**
```
🛑 Stopping ERP Platform Services...
✅ Stopped Java processes (identity-infrastructure, api-gateway, finance)
✅ Stopped Docker containers
✅ Cleaned Testcontainers
✅ Ports released

All services stopped successfully! ✅
```

**Usage:**
```powershell
.\scripts\stop-all-services.ps1
```

---

## 🗺️ Port Mapping Reference

| Port | Service | Purpose |
|------|---------|---------|
| 5432 | PostgreSQL | Shared database (erp_user/erp_pass) |
| 6379 | Redis | Session/cache store |
| 8080 | API Gateway | Main entry point |
| 8081 | Tenancy-Identity | Authentication & multi-tenancy |
| 8082 | Finance | Financial accounting (Phase 5A) |
| 8090 | Redpanda Console | Kafka UI (http://localhost:8090) |
| 19092 | Redpanda | Kafka API (external) |

---

## 🔧 Troubleshooting

### Infrastructure Won't Start

**Problem:** PostgreSQL container fails to start
```
Error: Port 5432 already in use
```

**Solution:**
```powershell
# Check if host PostgreSQL is running
Get-Service -Name postgresql-x64-17

# Stop host PostgreSQL
Stop-Service -Name postgresql-x64-17 -Force
Set-Service -Name postgresql-x64-17 -StartupType Disabled

# Restart infrastructure
.\scripts\start-infrastructure.ps1
```

### Port Already in Use

**Problem:** Application port conflict
```
Port 8082 is already in use by another process
```

**Solution:**
```powershell
# Find process using port
Get-NetTCPConnection -LocalPort 8082 | Select OwningProcess

# Kill process
Stop-Process -Id <PID> -Force

# Or use stop script
.\scripts\stop-all-services.ps1
```

### Service Not Responding

**Problem:** Health check fails after startup

**Solution:**
```powershell
# 1. Check logs in the colored PowerShell windows

# 2. Verify database connection
docker exec erp-postgres psql -U erp_user -d erp_identity -c "SELECT 1;"

# 3. Restart specific service
# Close the colored PowerShell window, then re-run:
.\scripts\start-all-services.ps1
```

### Docker Not Running

**Problem:** 
```
⚠️  Docker is not running
```

**Solution:**
1. Start Docker Desktop
2. Wait for it to show "Running"
3. Run `.\scripts\start-infrastructure.ps1` again

---

## 📚 Additional Scripts

### Identity Service Launch Script

### Basic Usage
```powershell
# Start on default port (8181)
.\scripts\dev-identity.ps1

# Start on specific port
.\scripts\dev-identity.ps1 -PreferredPort 8081

# Skip PostgreSQL checks (not recommended)
.\scripts\dev-identity.ps1 -SkipPostgresCheck
```

### Pre-flight Checks

The script automatically performs these checks before starting:

#### 1. Host PostgreSQL Service Detection
- Checks if Windows PostgreSQL service is running
- Warns if detected (causes port 5432 conflicts)
- Offers to stop the service automatically
- Provides link to detailed troubleshooting guide

**Response Options:**
- `y` - Stop the host PostgreSQL service
- `N` - Continue anyway (will likely fail with authentication errors)

#### 2. Docker PostgreSQL Container Check
- Verifies `erp-postgres` container is running
- Offers to start it if not running
- Uses `docker-compose-kafka.yml` configuration

**Response Options:**
- `y` - Start PostgreSQL container
- `N` - Continue anyway (will fail with connection errors)

### Example Output

#### When Host PostgreSQL is Detected:
```
=== Pre-flight Checks ===

⚠️  WARNING: Host PostgreSQL service detected running!
   Service(s): postgresql-x64-17

   This will prevent connections to Docker PostgreSQL on localhost:5432
   causing 'password authentication failed' errors.

   See docs/TROUBLESHOOTING_DATABASE.md for details

   Stop the host PostgreSQL service now? (y/N): y
   Stopping postgresql-x64-17...
   ✓ Stopped postgresql-x64-17

=========================

Launching identity-infrastructure on port 8081 (analytics disabled)
```

#### When Everything is OK:
```
=== Pre-flight Checks ===
=========================

Launching identity-infrastructure on port 8181 (analytics disabled)
```

### Manual PostgreSQL Service Management

#### Stop Host Service
```powershell
# Stop service
Stop-Service -Name postgresql-x64-17 -Force

# Disable automatic startup
Set-Service -Name postgresql-x64-17 -StartupType Disabled

# Check status
Get-Service -Name postgresql-x64-17
```

#### Start Docker PostgreSQL
```powershell
# Using docker-compose
docker compose -f docker-compose-kafka.yml up -d postgres

# Verify it's running
docker ps | Select-String postgres
docker exec erp-postgres psql -U erp_user -d erp_identity -c "SELECT version();"
```

### Common Scenarios

#### Scenario 1: First Time Setup
```powershell
# 1. Stop host PostgreSQL (if installed)
Stop-Service -Name postgresql-x64-17 -Force

# 2. Start Docker infrastructure
docker compose -f docker-compose-kafka.yml up -d postgres redis

# 3. Run identity service (script will detect everything is ready)
.\scripts\dev-identity.ps1 -PreferredPort 8081
```

#### Scenario 2: Forgot to Stop Host PostgreSQL
```powershell
# Run script - it will detect and prompt
.\scripts\dev-identity.ps1 -PreferredPort 8081

# Answer 'y' when prompted to stop host service
# Script continues automatically
```

#### Scenario 3: Docker Container Not Running
```powershell
# Run script - it will detect and prompt
.\scripts\dev-identity.ps1 -PreferredPort 8081

# Answer 'y' when prompted to start container
# Script waits for container to be ready and continues
```

#### Scenario 4: CI/CD or Automated Environments
```powershell
# Skip interactive prompts (assumes infrastructure is configured)
.\scripts\dev-identity.ps1 -SkipPostgresCheck -PreferredPort 8081
```

### Troubleshooting

#### Authentication Still Fails After Script Runs
1. Verify host PostgreSQL is actually stopped:
   ```powershell
   Get-Process -Name postgres -ErrorAction SilentlyContinue
   # Should return nothing
   ```

2. Test JDBC connection:
   ```powershell
   .\test-jdbc.ps1
   # Should output: "✓ Connection successful!"
   ```

3. Check Docker port mapping:
   ```powershell
   docker port erp-postgres
   # Should show: 5432/tcp -> 0.0.0.0:5432
   ```

4. See full troubleshooting guide:
   - [docs/TROUBLESHOOTING_DATABASE.md](../docs/TROUBLESHOOTING_DATABASE.md)

#### Port Already in Use
The script automatically finds the next available port if the preferred port is busy.

If port 8081 is in use, it will try 8082, 8083, etc. (up to 20 attempts).

#### Docker Not Running
```
⚠️  Could not check Docker status (Docker may not be running)
```

**Solution:**
1. Start Docker Desktop
2. Wait for it to be ready
3. Run the script again

### Environment Variables Set by Script

The script sets these environment variables before launching Quarkus:

| Variable | Value | Purpose |
|----------|-------|---------|
| `QUARKUS_HTTP_PORT` | Selected port | HTTP server port |
| `QUARKUS_ANALYTICS_DISABLED` | `true` | Disable telemetry prompts |

### Related Scripts

- `test-jdbc.ps1` - Test raw JDBC connection to PostgreSQL
- `test-db-connection.ps1` - Test psql connection (via Docker exec)
- `dev.ps1` - Main development helper script

### See Also

- [TROUBLESHOOTING_DATABASE.md](../docs/TROUBLESHOOTING_DATABASE.md) - Detailed database troubleshooting
- [QUICK_DEPLOY_GUIDE.md](../docs/QUICK_DEPLOY_GUIDE.md) - Complete deployment guide
- [PORT_MAPPINGS.md](../docs/PORT_MAPPINGS.md) - Port allocation reference
## Running Tests with/without Containers

- Default (fast unit tests only):
  - `./gradlew test` (uses `withContainers=false` from root `gradle.properties`)

- Enable Testcontainers-based integration tests (Postgres/Kafka):
  - `./gradlew test -PwithContainers=true`
  - Or set environment variable for CI: `ORG_GRADLE_PROJECT_withContainers=true`

- Naming conventions excluded when containers are off:
  - `*IntegrationTest*`, `*IT*`

- Tips:
  - Ensure Docker is running for container tests.
  - Use `--tests '*AuthIntegrationTest*'` to run a single IT.

## Monitoring Stack (Prometheus + Grafana)

- Start stack (Prometheus on 9090, Grafana on 3000):
  - PowerShell: `./scripts/dev-monitoring.ps1 up`
  - Bash: `./scripts/dev-monitoring.sh up`

- Stop stack:
  - PowerShell: `./scripts/dev-monitoring.ps1 down`
  - Bash: `./scripts/dev-monitoring.sh down`

- Logs:
  - PowerShell: `./scripts/dev-monitoring.ps1 logs`
  - Bash: `./scripts/dev-monitoring.sh logs`

Notes:
- Prometheus is configured in `monitoring/prometheus/prometheus.yml` to scrape the gateway at `host.docker.internal:8080/q/metrics`.
- Grafana auto-loads the dashboard from `dashboards/grafana` via provisioning under `monitoring/grafana/provisioning`.
