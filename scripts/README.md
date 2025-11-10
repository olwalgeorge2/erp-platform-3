# Development Scripts Usage Guide

## Identity Service Launch Script

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
