# ERP Platform - Live Smoke Test Guide

Complete manual smoke test for all three microservices running locally.

## Prerequisites

✅ **Infrastructure Running:**
```powershell
.\scripts\start-infrastructure.ps1
```

✅ **All Services Running:**
```powershell
.\scripts\start-all-services.ps1
```

✅ **Verify Status:**
```powershell
.\scripts\check-status.ps1
```

Expected output:
- ✅ Gateway (8080): UP (may show unhealthy - that's OK)
- ✅ Identity (8081): UP
- ✅ Finance (8082): UP

---

## Test Sequence

### 1️⃣ Health Checks (30 seconds)

#### Identity Service
```powershell
curl http://localhost:8081/q/health
```
✅ Expected: `{"status":"UP"}`

#### API Gateway
```powershell
curl http://localhost:8080/q/health
```
⚠️ Expected: `{"status":"DOWN"}` (backend services check fails - normal in dev)

#### Finance Service
```powershell
curl http://localhost:8082/q/health/ready
```
✅ Expected: `{"status":"UP"}`

---

### 2️⃣ Swagger UI Access (1 minute)

Open these URLs in your browser:

1. **Identity Swagger**: http://localhost:8081/q/swagger-ui/
   - ✅ Should show `/api/v1/identity/**` endpoints
   - ✅ Should show legacy `/api/identity/**` endpoints (deprecated)

2. **Gateway Swagger**: http://localhost:8080/q/swagger-ui/
   - ✅ Should show gateway endpoints

3. **Finance Swagger**: http://localhost:8082/q/swagger-ui/
   - ✅ Should show `/api/v1/finance/**` endpoints
   - ✅ Should show legacy `/api/finance/**` endpoints (deprecated)

**Validation:**
- All three Swagger UIs load successfully
- Endpoints are organized by tags
- Request/response schemas are visible

---

### 3️⃣ Identity Service Tests (2 minutes)

#### Test 1: Create Tenant
```bash
curl -X POST http://localhost:8081/api/v1/identity/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smoke Test Tenant",
    "slug": "smoke-test-123",
    "subscription": {
      "plan": "STARTER",
      "startDate": "2025-01-01T00:00:00Z",
      "maxUsers": 10,
      "maxStorage": 1000,
      "features": ["rbac"]
    }
  }'
```

✅ Expected: HTTP 201, returns tenant with ID
📝 **Save the `id` value as `TENANT_ID`**

#### Test 2: List Tenants
```bash
curl http://localhost:8081/api/v1/identity/tenants
```

✅ Expected: HTTP 200, array with your tenant

#### Test 3: Get Tenant by ID
```bash
curl http://localhost:8081/api/v1/identity/tenants/{TENANT_ID}
```

✅ Expected: HTTP 200, tenant details

---

### 4️⃣ Finance Service Tests (5 minutes)

#### Test 1: Create General Ledger
```bash
curl -X POST http://localhost:8082/api/v1/finance/ledgers \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "chartOfAccountsId": "00000000-0000-0000-0000-000000000001",
    "baseCurrency": "USD",
    "chartCode": "GL-001",
    "chartName": "Main Ledger"
  }'
```

✅ Expected: HTTP 201, returns ledger with ID
📝 **Save the `id` as `LEDGER_ID`**

#### Test 2: Define Chart of Accounts
```bash
# Create Asset Account
curl -X POST http://localhost:8082/api/v1/finance/chart-of-accounts/00000000-0000-0000-0000-000000000001/accounts \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "code": "1000",
    "name": "Cash",
    "type": "ASSET",
    "currency": "USD",
    "isPosting": true
  }'
```

✅ Expected: HTTP 201
📝 **Save `id` as `CASH_ACCOUNT_ID`**

```bash
# Create Revenue Account
curl -X POST http://localhost:8082/api/v1/finance/chart-of-accounts/00000000-0000-0000-0000-000000000001/accounts \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "code": "4000",
    "name": "Sales Revenue",
    "type": "REVENUE",
    "currency": "USD",
    "isPosting": true
  }'
```

✅ Expected: HTTP 201
📝 **Save `id` as `REVENUE_ACCOUNT_ID`**

#### Test 3: Create Accounting Period
```bash
curl -X POST http://localhost:8082/api/v1/finance/ledgers/{LEDGER_ID}/periods \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "name": "January 2025",
    "startDate": "2025-01-01",
    "endDate": "2025-01-31"
  }'
```

✅ Expected: HTTP 201
📝 **Save `id` as `PERIOD_ID`**

#### Test 4: Post Journal Entry (Double-Entry)
```bash
curl -X POST http://localhost:8082/api/v1/finance/journal-entries \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "ledgerId": "{LEDGER_ID}",
    "accountingPeriodId": "{PERIOD_ID}",
    "reference": "JE-SMOKE-001",
    "description": "Smoke test revenue posting",
    "lines": [
      {
        "accountId": "{CASH_ACCOUNT_ID}",
        "direction": "DEBIT",
        "amountMinor": 100000,
        "currency": "USD",
        "description": "Cash received"
      },
      {
        "accountId": "{REVENUE_ACCOUNT_ID}",
        "direction": "CREDIT",
        "amountMinor": 100000,
        "currency": "USD",
        "description": "Sales revenue"
      }
    ]
  }'
```

✅ Expected: HTTP 201 - Balanced entry posted
❌ If unbalanced (debits ≠ credits): HTTP 400

#### Test 5: Get Trial Balance
```bash
curl "http://localhost:8082/api/v1/finance/ledgers/{LEDGER_ID}/trial-balance?periodId={PERIOD_ID}" \
  -H "X-Tenant-Id: {TENANT_ID}"
```

✅ Expected: HTTP 200, JSON with:
```json
{
  "tenantId": "{TENANT_ID}",
  "ledgerId": "{LEDGER_ID}",
  "balances": [
    {
      "accountCode": "1000",
      "accountName": "Cash",
      "debitBalance": 100000,
      "creditBalance": 0
    },
    {
      "accountCode": "4000",
      "accountName": "Sales Revenue",
      "debitBalance": 0,
      "creditBalance": 100000
    }
  ],
  "totalDebits": 100000,
  "totalCredits": 100000,
  "balanced": true
}
```

---

### 5️⃣ Multi-Currency Tests (3 minutes)

#### Test 1: Create Exchange Rate
```bash
curl -X POST http://localhost:8082/api/v1/finance/exchange-rates \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "fromCurrency": "EUR",
    "toCurrency": "USD",
    "rate": 1.12,
    "rateType": "SPOT",
    "effectiveDate": "2025-01-01"
  }'
```

✅ Expected: HTTP 201

#### Test 2: Post Foreign Currency Transaction
```bash
# First create EUR account
curl -X POST http://localhost:8082/api/v1/finance/chart-of-accounts/00000000-0000-0000-0000-000000000001/accounts \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "code": "1050",
    "name": "EUR Cash",
    "type": "ASSET",
    "currency": "EUR",
    "isPosting": true
  }'
```

📝 **Save `id` as `EUR_CASH_ACCOUNT_ID`**

```bash
# Post multi-currency journal entry
curl -X POST http://localhost:8082/api/v1/finance/journal-entries \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "ledgerId": "{LEDGER_ID}",
    "accountingPeriodId": "{PERIOD_ID}",
    "reference": "JE-FX-001",
    "description": "Foreign currency payment",
    "lines": [
      {
        "accountId": "{EUR_CASH_ACCOUNT_ID}",
        "direction": "DEBIT",
        "amountMinor": 50000,
        "currency": "EUR",
        "originalCurrency": "EUR",
        "exchangeRate": 1.12,
        "description": "EUR payment received"
      },
      {
        "accountId": "{REVENUE_ACCOUNT_ID}",
        "direction": "CREDIT",
        "amountMinor": 56000,
        "currency": "USD",
        "description": "Revenue in USD (50000 EUR * 1.12)"
      }
    ]
  }'
```

✅ Expected: HTTP 201 - Multi-currency entry posted with conversion

#### Test 3: Currency Revaluation
```bash
curl -X POST http://localhost:8082/api/v1/finance/ledgers/{LEDGER_ID}/revalue-currency \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: {TENANT_ID}" \
  -d '{
    "tenantId": "{TENANT_ID}",
    "revaluationDate": "2025-01-31",
    "newRate": 1.15
  }'
```

✅ Expected: HTTP 200, returns revaluation summary with unrealized gains/losses

---

### 6️⃣ Gateway Routing Tests (2 minutes)

Test that Gateway can route to backend services:

#### Test 1: Identity via Gateway
```bash
curl http://localhost:8080/api/v1/identity/tenants
```

✅ Expected: HTTP 200 (if gateway routing configured)
⚠️ May return 404 if gateway routes not yet configured - **this is expected in Phase 1**

#### Test 2: Finance via Gateway
```bash
curl http://localhost:8080/api/v1/finance/ledgers/{LEDGER_ID}/trial-balance?periodId={PERIOD_ID} \
  -H "X-Tenant-Id: {TENANT_ID}"
```

✅ Expected: HTTP 200 with trial balance data
⚠️ May return 404 if gateway routes not yet configured

---

## Success Criteria

### ✅ All Tests Pass
- [ ] All 3 services start successfully
- [ ] All health checks return UP (except Gateway backend checks)
- [ ] All Swagger UIs accessible
- [ ] Identity: Can create/list tenants
- [ ] Finance: Can create ledger, accounts, periods
- [ ] Finance: Can post balanced journal entries
- [ ] Finance: Trial balance calculates correctly
- [ ] Finance: Multi-currency transactions work
- [ ] Finance: Currency revaluation calculates gains/losses

### ⚠️ Expected Limitations
- Gateway backend services health check shows DOWN (normal - services not registered)
- Gateway routing may not work yet (Phase 2 feature)
- No authentication/authorization (JWT integration pending)

---

## Troubleshooting

### Service Won't Start
```powershell
# Check logs in the colored PowerShell windows
# Or restart services:
.\scripts\stop-all-services.ps1
.\scripts\start-all-services.ps1
```

### 404 Errors on Endpoints
```powershell
# Verify service is listening:
netstat -ano | Select-String ":808[012].*LISTENING"

# Check Swagger UI shows endpoints:
# Identity: http://localhost:8081/q/swagger-ui/
# Finance:  http://localhost:8082/q/swagger-ui/
```

### Database Errors
```powershell
# Reset database:
docker exec -it erp-postgres psql -U erp_user -d postgres -c "DROP DATABASE IF EXISTS erp_identity;" -c "CREATE DATABASE erp_identity OWNER erp_user;"

# Restart services to re-run migrations
```

### Port Conflicts
```powershell
# Stop all Java/Gradle processes:
Get-Process -Name "java","gradle*" | Stop-Process -Force

# Then restart:
.\scripts\start-all-services.ps1
```

---

## Performance Baselines

**Expected response times (local dev):**
- Health checks: < 100ms
- Simple GET (list tenants): < 200ms
- POST journal entry: < 500ms
- Trial balance: < 1s
- Currency revaluation: < 2s

**Expected startup times:**
- Identity: 30-60 seconds
- Gateway: 30-60 seconds  
- Finance: 60-90 seconds (Testcontainers)

---

## Next Steps After Smoke Test

1. **If all tests pass**: Ready for Phase 5A.2 (Cost Centers & Dimensions)
2. **If Gateway routing fails**: Expected - implement in Phase 2
3. **If authentication fails**: Expected - JWT integration is Phase 3
4. **Document any failures**: Create issues for investigation

---

## Quick Commands Reference

```powershell
# Start everything
.\scripts\start-infrastructure.ps1
.\scripts\start-all-services.ps1

# Check status
.\scripts\check-status.ps1

# Stop everything
.\scripts\stop-all-services.ps1

# View logs (look at colored PowerShell windows)

# Reset everything
docker compose -f docker-compose-kafka.yml down -v
.\scripts\start-infrastructure.ps1
.\scripts\start-all-services.ps1
```
