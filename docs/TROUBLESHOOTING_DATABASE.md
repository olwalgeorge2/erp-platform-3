# Database Connection Troubleshooting Guide

## Issue: PostgreSQL Password Authentication Failed

### Symptom
When starting the identity service with `quarkusDev`, you encounter:
```
FATAL: password authentication failed for user "erp_user"
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "erp_user"
```

### Root Cause
**Host PostgreSQL Service Intercepting Docker Connections**

If you have PostgreSQL installed natively on Windows/macOS/Linux and the service is running, it will bind to `localhost:5432` and intercept all database connections intended for the Docker container.

This happens because:
1. Docker container publishes port `5432` → host port `5432`
2. Host PostgreSQL service is already listening on port `5432`
3. Applications connecting to `localhost:5432` reach the **host PostgreSQL**, not the Docker container
4. Host PostgreSQL has different credentials than the Docker container
5. Authentication fails because credentials don't match

### Verification Steps

#### 1. Check if Host PostgreSQL is Running

**Windows:**
```powershell
Get-Service -Name postgresql* | Where-Object {$_.Status -eq 'Running'}
Get-Process -Name postgres -ErrorAction SilentlyContinue
```

**macOS/Linux:**
```bash
sudo lsof -i :5432
ps aux | grep postgres
```

#### 2. Test JDBC Connection
```powershell
# Run the test script
.\test-jdbc.ps1
```

If this fails with "password authentication failed" but `docker exec` commands work, the host service is intercepting connections.

#### 3. Verify Docker Container
```powershell
docker ps | Select-String postgres
docker port erp-postgres
docker exec erp-postgres psql -U erp_user -d erp_identity -c "SELECT version();"
```

### Solutions

#### Solution 1: Stop Host PostgreSQL Service (Recommended for Development)

**Windows:**
```powershell
# Stop the service
Stop-Service -Name postgresql-x64-17 -Force

# Disable automatic startup (optional)
Set-Service -Name postgresql-x64-17 -StartupType Disabled

# Verify it's stopped
Get-Service -Name postgresql-x64-17
```

**macOS:**
```bash
# Stop PostgreSQL (Homebrew)
brew services stop postgresql@17

# Or using pg_ctl
pg_ctl stop -D /usr/local/var/postgres
```

**Linux (systemd):**
```bash
sudo systemctl stop postgresql
sudo systemctl disable postgresql  # Prevent auto-start
```

#### Solution 2: Use Different Port for Docker

Modify `docker-compose-kafka.yml`:
```yaml
services:
  postgres:
    ports:
      - "5433:5432"  # Map to different host port
```

Then update `application.yaml`:
```yaml
quarkus:
  datasource:
    jdbc:
      url: ${QUARKUS_DATASOURCE_JDBC_URL:jdbc:postgresql://127.0.0.1:5433/erp_identity}
```

#### Solution 3: Use Docker Network Name

Connect to PostgreSQL using the Docker network name instead of localhost:
```yaml
quarkus:
  datasource:
    jdbc:
      url: jdbc:postgresql://erp-postgres:5432/erp_identity
```

**Note:** This only works if the application also runs in Docker.

### Prevention

The `dev-identity.ps1` script now includes automatic detection and handling:

```powershell
# Checks for host PostgreSQL service
# Warns if running
# Optionally stops it with user confirmation
```

### Related Issues

- **Issue:** Configuration files (application.properties vs application.yaml) creating duplicates
  - **Solution:** Use only `application.yaml`, delete any `application.properties` or `application-dev.properties`

- **Issue:** `localhost` vs `127.0.0.1` resolution differences
  - **Solution:** Use `127.0.0.1` explicitly to avoid IPv6/IPv4 resolution issues

- **Issue:** pg_hba.conf authentication rules not taking effect
  - **Solution:** Restart PostgreSQL container after modifying `pg_hba.conf`

### Testing After Fix

1. **Verify Host Service is Stopped:**
   ```powershell
   Get-Service -Name postgresql* | Where-Object {$_.Status -eq 'Running'}
   # Should return nothing
   ```

2. **Test Docker PostgreSQL Connection:**
   ```powershell
   .\test-jdbc.ps1
   # Should output: "✓ Connection successful!"
   ```

3. **Start Identity Service:**
   ```powershell
   .\scripts\dev-identity.ps1 -PreferredPort 8081
   # Should start without password authentication errors
   ```

4. **Verify Health Endpoints:**
   ```powershell
   curl http://localhost:8081/q/health/live
   curl http://localhost:8081/q/health/ready
   ```

### Additional Notes

- **PostgreSQL Logs:** Check Docker container logs if issues persist
  ```powershell
  docker logs erp-postgres --tail 50
  ```

- **Connection Pooling:** Quarkus uses Agroal connection pool. Check pool configuration if you see connection exhaustion

- **Firewall Rules:** Ensure port 5432 is not blocked by Windows Firewall

### Quick Reference Commands

```powershell
# Stop host PostgreSQL (Windows)
Stop-Service -Name postgresql-x64-17 -Force

# Restart Docker PostgreSQL
docker restart erp-postgres

# Test connection
.\test-jdbc.ps1

# Start identity service
.\scripts\dev-identity.ps1 -PreferredPort 8081

# Check service logs
docker logs erp-postgres -f
```

### See Also
- [PostgreSQL Docker Setup](QUICK_DEPLOY_GUIDE.md)
- [Health Check Configuration](ARCHITECTURE.md#health-checks)
- [Port Mappings](PORT_MAPPINGS.md)
