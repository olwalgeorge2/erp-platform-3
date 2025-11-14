# ERP Platform Service Startup Guide

**Date:** 2025-11-14  
**Status:** All services running in external PowerShell windows

## Running Services

Three external PowerShell windows have been launched:

### 1. 🔐 Tenancy-Identity Service
- **Port:** 8081
- **Window Title:** TENANCY-IDENTITY (8081)
- **Purpose:** Authentication, authorization, tenant management
- **Health Check:** http://localhost:8081/q/health
- **Swagger UI:** http://localhost:8081/q/swagger-ui
- **Database:** Uses shared PostgreSQL (erp_identity schema)

### 2. 🌐 API Gateway
- **Port:** 8080
- **Window Title:** API-GATEWAY (8080)
- **Purpose:** Main entry point, routing, authentication, rate limiting
- **Health Check:** http://localhost:8080/q/health
- **Swagger UI:** http://localhost:8080/q/swagger-ui
- **Routes:** Proxies requests to backend services

### 3. 💰 Financial Accounting Service
- **Port:** 8082
- **Window Title:** FINANCE (8082)
- **Purpose:** General ledger, multi-currency, journal entries (Phase 5A)
- **Health Check:** http://localhost:8082/q/health/ready
- **Swagger UI:** http://localhost:8082/q/swagger-ui
- **Database:** Uses Testcontainers (ephemeral PostgreSQL)
- **Kafka:** Uses Testcontainers (ephemeral Redpanda)

### API Base Paths (Versioned + Compatibility)

| Service | Canonical Path | Legacy Alias (temporary) | Notes |
|---------|----------------|--------------------------|-------|
| Tenancy-Identity | `http://localhost:8081/api/v1/identity/**` | `http://localhost:8081/api/**` | Prefer `/api/v1/identity` for all new work. The unversioned alias remains only for dev/test clients until migration completes. |
| Finance | `http://localhost:8082/api/v1/finance/**` | `http://localhost:8082/api/finance/**` | Gateway already proxies the versioned path. The legacy alias keeps existing scripts working locally. |

> Tip: when exercising flows through the API Gateway, always use `/api/v1/...`. The direct-service aliases are temporary and will be removed once verification is complete.

---

## Startup Sequence & Wait Times

**Total Startup Time:** ~2-3 minutes for all services

### Phase 1: Tenancy-Identity (30-45 seconds)
1. Connects to shared PostgreSQL (localhost:5432)
2. Runs Flyway migrations on erp_identity schema
3. Starts Quarkus on port 8081
4. **Ready when:** You see "Listening on: http://0.0.0.0:8081"

### Phase 2: API Gateway (15-30 seconds)
1. Connects to Redis (if configured)
2. Registers routes to backend services
3. Starts Quarkus on port 8080
4. **Ready when:** You see "Listening on: http://0.0.0.0:8080"

### Phase 3: Finance Service (60-90 seconds)
1. Starts PostgreSQL Testcontainer (~30s)
2. Starts Redpanda/Kafka Testcontainer (~20s)
3. Runs Flyway migrations (V001, V004, V005, V006)
4. Starts Quarkus on port 8082
5. **Ready when:** You see "Listening on: http://0.0.0.0:8082"

---

## Verification Steps

### 1. Check All Services Are Up

Run these commands in a new PowerShell terminal:

```powershell
# Tenancy-Identity Health
curl http://localhost:8081/q/health

# API Gateway Health
curl http://localhost:8080/q/health

# Finance Service Health
curl http://localhost:8082/q/health/ready
```

**Expected:** All return `{"status":"UP"}`

### 2. Check Port Bindings

```powershell
netstat -ano | findstr "8080 8081 8082"
```

**Expected Output:**
```
TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       <PID>
TCP    0.0.0.0:8081           0.0.0.0:0              LISTENING       <PID>
TCP    0.0.0.0:8082           0.0.0.0:0              LISTENING       <PID>
```

### 3. Open Swagger UIs

Open in browser:
- http://localhost:8080/q/swagger-ui (Gateway)
- http://localhost:8081/q/swagger-ui (Tenancy-Identity)
- http://localhost:8082/q/swagger-ui (Finance)

---

## Testing the Platform

### Option 1: Direct Service Testing (Finance)

Use `docs/rest/finance-accounting.rest` to test directly on port 8082:

```http
GET http://localhost:8082/q/health/ready
```

### Option 2: Gateway Testing (Preferred)

Test through API Gateway on port 8080:

```http
### Via Gateway (requires authentication)
POST http://localhost:8080/api/v1/finance/ledgers
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
Authorization: Bearer <JWT_TOKEN>

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "chartOfAccountsId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "baseCurrency": "USD",
  "chartCode": "MAIN",
  "chartName": "Main Chart"
}
```

### Option 3: End-to-End Test

1. **Authenticate** via Tenancy-Identity (port 8081)
2. **Get JWT Token** from response
3. **Call Finance API** via Gateway (port 8080) with token
4. **Verify Event Publishing** to Kafka

---

## Port Mapping Reference

| Port | Service | Purpose |
|------|---------|---------|
| 5432 | PostgreSQL | Shared database (Identity) |
| 8080 | API Gateway | Main entry point |
| 8081 | Tenancy-Identity | Auth service |
| 8082 | Finance | Accounting service |
| 8090 | Redpanda Console | Kafka UI |
| 19092 | Redpanda | Kafka API (external) |

---

## Troubleshooting

### Service Won't Start

**Tenancy-Identity:**
- Check PostgreSQL is running: `docker ps | findstr postgres`
- Verify database credentials: `.\test-db-connection.ps1`
- Check port 8081 is free: `netstat -ano | findstr 8081`

**API Gateway:**
- Check port 8080 is free (Docker Desktop may conflict)
- Verify backend service URLs are correct
- Check Redis is accessible (if configured)

**Finance Service:**
- Ensure Docker Desktop is running (Testcontainers needs it)
- Check Docker has available resources (4GB+ RAM recommended)
- Wait full 90 seconds for containers to initialize
- View logs in the external PowerShell window

### Port Conflicts

If you see "Address already in use":

```powershell
# Find process using the port
netstat -ano | findstr "<PORT>"

# Kill the process (use PID from above)
taskkill /PID <PID> /F
```

### Database Connection Errors

**Finance Service:**
- Uses Testcontainers (automatic, no setup needed)
- If fails, restart Docker Desktop
- Check Docker Desktop → Settings → Resources → Memory (4GB+)

**Tenancy-Identity:**
- Needs shared PostgreSQL container
- Start with: `docker compose -f docker-compose-kafka.yml up -d postgres`
- Verify: `docker ps | findstr postgres`

### Kafka Connection Errors

Finance service uses Testcontainers for Kafka (automatic):
- If Redpanda fails to start, increase Docker memory
- Check logs: Look for "Container redpanda started"
- Verify: Should see "Dev Services for Kafka started"

---

## Stopping Services

### Option 1: Graceful Shutdown
Press `Ctrl+C` in each PowerShell window, then type `Y` to confirm.

### Option 2: Kill All Gradle Processes
```powershell
Get-Process -Name java | Where-Object {$_.MainWindowTitle -like "*gradle*"} | Stop-Process -Force
```

### Option 3: Close PowerShell Windows
Simply close the three external PowerShell windows. Services will terminate automatically.

---

## Next Steps

1. ✅ **Verify all services are running** (health checks pass)
2. ✅ **Test Finance API** directly (port 8280)
3. ⏭️ **Configure Gateway routes** for Finance endpoints
4. ⏭️ **Test end-to-end flow** through Gateway (port 8080)
5. ⏭️ **Run integration tests** across all services
6. ⏭️ **Test Phase 5A features** (multi-currency, revaluation)

---

## Quick Reference

**Health Checks:**
```bash
curl http://localhost:8081/q/health          # Identity
curl http://localhost:8080/q/health          # Gateway
curl http://localhost:8082/q/health/ready    # Finance
```

**Metrics:**
```bash
curl http://localhost:8081/q/metrics         # Identity
curl http://localhost:8080/q/metrics         # Gateway
curl http://localhost:8082/q/metrics         # Finance
```

**Test Files:**
- `docs/rest/finance-accounting.rest` - Finance API examples
- `docs/FINANCE_LIVE_TEST_GUIDE.md` - Detailed test scenarios
- `docs/PORT_MAPPINGS.md` - Complete port reference

**Logs:**
- View live logs in each external PowerShell window
- Colored output for easy debugging
- Services auto-reload on code changes (Quarkus Dev Mode)
