# PostgreSQL Host Service Conflict - Resolution Summary

**Date:** November 10, 2025  
**Issue ID:** Database Connection Authentication Failure  
**Severity:** High (Blocks local development)

## Problem Statement

When starting the identity service with `.\scripts\dev-identity.ps1`, developers encountered:

```
FATAL: password authentication failed for user "erp_user"
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "erp_user"
```

Despite having correct credentials configured in `application.yaml` and Docker PostgreSQL container running with the correct password.

## Root Cause

**Windows PostgreSQL 17 service was running on the host machine** and intercepting all connections to `localhost:5432`.

### Technical Details

1. **Port Binding Conflict:**
   - Docker publishes PostgreSQL: `0.0.0.0:5432` → container port `5432`
   - Windows PostgreSQL service binds to: `0.0.0.0:5432`
   - When both are running, host service wins the `localhost:5432` binding

2. **Connection Flow:**
   ```
   Application → localhost:5432 → Host PostgreSQL (NOT Docker!)
                                    ↓
                              Different credentials
                                    ↓
                          Authentication fails
   ```

3. **Why Docker Commands Worked:**
   - `docker exec erp-postgres psql` connects **inside** the container (Unix socket)
   - Does not go through TCP/IP port 5432
   - Therefore, never hits the host PostgreSQL service

## Detection Process

Multiple troubleshooting steps were performed:

1. ❌ Configuration files checked - were correct
2. ❌ Password reset multiple times - had no effect
3. ❌ Changed `localhost` to `127.0.0.1` - no change
4. ❌ Removed duplicate config files - no change  
5. ❌ Added `trust` authentication to pg_hba.conf - still failed
6. ❌ Recreated PostgreSQL container from scratch - still failed
7. ✅ **Checked for host PostgreSQL processes** - FOUND IT!

```powershell
Get-Process -Name postgres
# Output: 7 postgres.exe processes from C:\Program Files\PostgreSQL\17\
```

## Solution

### Immediate Fix
```powershell
Stop-Service -Name postgresql-x64-17 -Force
```

After stopping the host service, connections immediately worked.

### Permanent Prevention

1. **Automated Script Enhancement**
   - Modified `scripts/dev-identity.ps1` with pre-flight checks
   - Automatically detects host PostgreSQL service
   - Prompts user to stop it before starting
   - Provides informative warnings and guidance

2. **Documentation Created**
   - `docs/TROUBLESHOOTING_DATABASE.md` - Comprehensive troubleshooting guide
   - `scripts/README.md` - Script usage guide with examples
   - Updated main `README.md` with troubleshooting section

3. **Test Utilities**
   - `test-jdbc.ps1` - Raw JDBC connection tester
   - Quickly verifies if issue is present

## Changes Made

### 1. Enhanced Launch Script (`scripts/dev-identity.ps1`)

**Added Features:**
- `Test-HostPostgreSQL()` - Detects Windows PostgreSQL services
- `Test-DockerPostgreSQL()` - Verifies Docker container running
- Interactive prompts to fix issues automatically
- `-SkipPostgresCheck` parameter for CI/CD environments
- Color-coded warnings and status messages

**New Behavior:**
```
=== Pre-flight Checks ===

⚠️  WARNING: Host PostgreSQL service detected running!
   Service(s): postgresql-x64-17

   This will prevent connections to Docker PostgreSQL on localhost:5432
   causing 'password authentication failed' errors.

   See docs/TROUBLESHOOTING_DATABASE.md for details

   Stop the host PostgreSQL service now? (y/N):
```

### 2. Comprehensive Documentation

**`docs/TROUBLESHOOTING_DATABASE.md`:**
- Symptom descriptions
- Root cause explanation
- Verification steps  
- 3 different solution approaches
- Prevention strategies
- Quick reference commands
- Related issues section

**`scripts/README.md`:**
- Script usage examples
- Pre-flight check explanations
- Common scenarios
- Troubleshooting procedures

### 3. Updated Main README

Added troubleshooting section with links to detailed guides.

## Verification

### Test Procedure
```powershell
# 1. Stop host PostgreSQL
Stop-Service -Name postgresql-x64-17 -Force

# 2. Test JDBC connection
.\test-jdbc.ps1
# Output: ✓ Connection successful!

# 3. Start identity service
.\scripts\dev-identity.ps1 -PreferredPort 8081
# Output: Quarkus starts successfully, no authentication errors

# 4. Verify health endpoints
curl http://localhost:8081/q/health/live
curl http://localhost:8081/q/health/ready
```

### Results
✅ All tests passed  
✅ Identity service running successfully  
✅ Database connections working  
✅ Health checks responding  
✅ Kafka warnings present but non-fatal (expected)

## Impact

**Before Fix:**
- 🔴 Identity service failed to start
- 🔴 Database authentication errors
- 🔴 Confusing error messages (credentials appeared correct)
- 🔴 Time-consuming manual troubleshooting

**After Fix:**
- ✅ Automatic detection of the issue
- ✅ User-friendly prompts to resolve
- ✅ Service starts successfully  
- ✅ Clear documentation for future reference
- ✅ CI/CD bypass option (`-SkipPostgresCheck`)

## Lessons Learned

1. **Port conflicts are subtle** - Both services can bind to 0.0.0.0:5432 on different network interfaces
2. **Test at multiple layers** - Docker exec tests bypassed the actual TCP/IP connection path
3. **Host services persist** - Windows services continue running even when Docker is primary development tool
4. **Proactive checking** - Script validation prevents frustrating debugging sessions
5. **Documentation is critical** - Detailed guides help future developers resolve issues quickly

## Alternative Solutions Considered

1. **Use Different Port for Docker** (e.g., 5433)
   - Rejected: Requires changing all configuration files and documentation

2. **Always Use Docker Network Names** (e.g., `erp-postgres:5432`)
   - Rejected: Only works when app runs in Docker, not for local dev

3. **Disable Host PostgreSQL Permanently**
   - Partially implemented: Script offers to stop but doesn't disable
   - Rationale: Developers may use host PostgreSQL for other projects

## Recommendations

1. **For New Developers:**
   - Run `.\scripts\dev-identity.ps1` and follow prompts
   - Read `docs/TROUBLESHOOTING_DATABASE.md` if issues persist

2. **For CI/CD:**
   - Use `-SkipPostgresCheck` parameter to bypass interactive checks
   - Ensure Docker PostgreSQL is in healthy state before builds

3. **For Production:**
   - Not applicable (production uses managed database services)
   - Docker/local conflicts don't occur in deployed environments

## Files Modified

1. ✅ `scripts/dev-identity.ps1` - Enhanced with pre-flight checks
2. ✅ `docs/TROUBLESHOOTING_DATABASE.md` - Created comprehensive guide
3. ✅ `scripts/README.md` - Created script usage documentation
4. ✅ `README.md` - Added troubleshooting section
5. ✅ `docs/POSTGRESQL_CONFLICT_RESOLUTION.md` - This summary document

## Related Issues

- Configuration file conflicts (application.properties vs application.yaml)
- IPv6 vs IPv4 resolution (`localhost` vs `127.0.0.1`)
- pg_hba.conf authentication rules not reloading

See `docs/TROUBLESHOOTING_DATABASE.md` for details on these related issues.

## Status

**RESOLVED** ✅

The issue is fully resolved with:
- Working solution implemented
- Automated prevention in place
- Comprehensive documentation created
- Verified with multiple tests

---

**For questions or issues, see:**
- [docs/TROUBLESHOOTING_DATABASE.md](TROUBLESHOOTING_DATABASE.md)
- [scripts/README.md](../scripts/README.md)
