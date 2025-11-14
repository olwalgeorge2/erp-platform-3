# Finance Accounting Live Test Guide

**Date:** 2025-11-14  
**Service:** Financial Accounting (Phase 5A - Multi-Currency)  
**Port:** http://localhost:8082

## Prerequisites

✅ Service running in external PowerShell window  
✅ Testcontainers providing PostgreSQL + Kafka  
✅ REST client (VS Code REST Client extension or Postman)

## Test Sequence

### 1. Health Check (Verify Service is Ready)

```http
GET http://localhost:8082/q/health/ready
```

**Expected Response:**
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "Finance Service Readiness",
      "status": "UP"
    }
  ]
}
```

---

### 2. Check Metrics Endpoint

```http
GET http://localhost:8082/q/metrics
```

**Expected:** Prometheus metrics including:
- `finance_journal_post_seconds_count`
- `finance_journal_post_seconds_sum`
- `finance_journal_lines_total`

---

### 3. Create Ledger (Foundation)

```http
POST http://localhost:8082/api/v1/finance/ledgers
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "chartOfAccountsId": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
  "baseCurrency": "USD",
  "chartCode": "MAIN",
  "chartName": "Main Chart of Accounts"
}
```

**Expected:** 201 Created with `LedgerResponse`  
**Save:** `ledgerId` and `chartOfAccountsId` from response

---

### 4. Define Accounts (Chart Setup)

#### 4a. Create Asset Account (Cash USD)
```http
POST http://localhost:8082/api/v1/finance/chart-of-accounts/{chartId}/accounts
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "code": "1010",
  "name": "Cash - USD",
  "type": "ASSET",
  "currency": "USD",
  "isPosting": true
}
```

**Save:** `accountId` as `cashAccountId`

#### 4b. Create Revenue Account
```http
POST http://localhost:8082/api/v1/finance/chart-of-accounts/{chartId}/accounts
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "code": "4010",
  "name": "Sales Revenue",
  "type": "REVENUE",
  "currency": "USD",
  "isPosting": true
}
```

**Save:** `accountId` as `revenueAccountId`

#### 4c. Create FX Gain Account
```http
POST http://localhost:8082/api/v1/finance/chart-of-accounts/{chartId}/accounts
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "code": "7010",
  "name": "Foreign Exchange Gains",
  "type": "REVENUE",
  "currency": "USD",
  "isPosting": true
}
```

**Save:** `accountId` as `fxGainAccountId`

#### 4d. Create FX Loss Account
```http
POST http://localhost:8082/api/v1/finance/chart-of-accounts/{chartId}/accounts
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "code": "8010",
  "name": "Foreign Exchange Losses",
  "type": "EXPENSE",
  "currency": "USD",
  "isPosting": true
}
```

**Save:** `accountId` as `fxLossAccountId`

#### 4e. Create Foreign Cash Account (EUR)
```http
POST http://localhost:8082/api/v1/finance/chart-of-accounts/{chartId}/accounts
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "code": "1020",
  "name": "Cash - EUR",
  "type": "ASSET",
  "currency": "EUR",
  "isPosting": true
}
```

**Save:** `accountId` as `eurCashAccountId`

---

### 5. Post Journal Entry (Simple USD Transaction)

```http
POST http://localhost:8082/api/v1/finance/journal-entries
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "ledgerId": "{ledgerId}",
  "accountingPeriodId": "{periodId}",
  "reference": "JE-001",
  "description": "Initial sale - USD",
  "lines": [
    {
      "accountId": "{cashAccountId}",
      "direction": "DEBIT",
      "amountMinor": 100000,
      "currency": "USD",
      "description": "Cash received"
    },
    {
      "accountId": "{revenueAccountId}",
      "direction": "CREDIT",
      "amountMinor": 100000,
      "currency": "USD",
      "description": "Sales revenue"
    }
  ]
}
```

**Expected:** 202 Accepted with `JournalEntryResponse`  
**Verify:** Debits = Credits (balanced entry)

---

### 6. Post Foreign Currency Transaction (EUR → USD Conversion)

```http
POST http://localhost:8082/api/v1/finance/journal-entries
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "ledgerId": "{ledgerId}",
  "accountingPeriodId": "{periodId}",
  "reference": "JE-002",
  "description": "European sale - EUR",
  "lines": [
    {
      "accountId": "{eurCashAccountId}",
      "direction": "DEBIT",
      "amountMinor": 50000,
      "currency": "EUR",
      "description": "EUR cash received (500 EUR)"
    },
    {
      "accountId": "{revenueAccountId}",
      "direction": "CREDIT",
      "amountMinor": 50000,
      "currency": "EUR",
      "description": "Sales revenue in EUR"
    }
  ]
}
```

**Expected:** 202 Accepted  
**Note:** Service automatically converts EUR to USD using exchange rate from database

---

### 7. Run Currency Revaluation (Phase 5A Feature)

```http
POST http://localhost:8082/api/v1/finance/ledgers/{ledgerId}/periods/{periodId}/currency-revaluation
Content-Type: application/json
X-Tenant-Id: aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

{
  "tenantId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "gainAccountId": "{fxGainAccountId}",
  "lossAccountId": "{fxLossAccountId}",
  "asOfTimestamp": "2025-11-14T00:00:00Z",
  "reference": "FX-REVAL-NOV",
  "description": "Monthly FX revaluation"
}
```

**Expected:** 200 OK with adjustment `JournalEntryResponse` (if exchange rates changed)  
**Or:** `{"message": "No revaluation adjustments needed"}` (if rates unchanged)

---

### 8. Verify Metrics After Operations

```http
GET http://localhost:8082/q/metrics
```

**Verify counters increased:**
- `finance_journal_post_seconds_count` = 2 (two journal entries)
- `finance_revaluation_seconds_count` = 1 (one revaluation run)
- `finance_journal_lines_total` = 4 (two entries × 2 lines each)

---

## Success Criteria

✅ All health checks pass  
✅ Ledger and accounts created successfully  
✅ Journal entries post and balance (debits = credits)  
✅ Foreign currency transactions auto-convert  
✅ Currency revaluation calculates unrealized gains/losses  
✅ Metrics capture all operations  
✅ No errors in service logs  
✅ Database migrations applied (V001, V004, V005, V006)

---

## Troubleshooting

### Service won't start
- Check external PowerShell window for errors
- Ensure Docker is running (Testcontainers needs it)
- Wait 60s for PostgreSQL/Kafka containers to initialize

### 404 Not Found
- Verify service is listening on http://localhost:8082
- Check `quarkus:http:port` in application.yml

### 400 Bad Request
- Verify JSON payload matches DTOs
- Check `amountMinor` is Long (not decimal)
- Ensure `direction` is "DEBIT" or "CREDIT"

### Database errors
- Service auto-creates schema via Flyway
- Check logs for migration failures
- Testcontainers uses ephemeral DB (cleared on restart)

---

## Next Steps After Testing

1. **Test via API Gateway** (port 8080) with authentication scopes
2. **Verify Event Publishing** to Kafka topics (`finance.journal.events.v1`)
3. **Load Testing** with k6/JMeter (target: 100 journals/second)
4. **Integration Testing** with downstream contexts (AP, AR, BI)
5. **Phase 5A Continuation**: Cost centers, reporting APIs, approval workflows
