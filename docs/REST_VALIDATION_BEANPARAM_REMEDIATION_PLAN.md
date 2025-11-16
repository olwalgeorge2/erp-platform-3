# REST Validation BeanParam Remediation Plan

## 1. Background
- **Policy Reference**: ADR-010 (REST Validation Standard) mandates `@Valid @BeanParam` DTOs for every multi-tenant, UUID, enum, or finance endpoint plus prohibitions for health/metrics (`docs/adr/ADR-010-rest-validation-standard.md`).
- **Drivers**: Stop manual UUID parsing (HTTP 500), enforce localized validation, and capture SOX/GDPR audit signals uniformly.
- **Observation**: Implementation is uneven—query endpoints are compliant while legacy command and identity endpoints bypass BeanParam, creating validation debt.

## 2. Compliance Snapshot (Nov 2025)

| Module | Endpoints | Compliant | Violations | Notes |
| --- | --- | --- | --- | --- |
| Tenancy-Identity (`AuthResource`) | 14 | 4 | 10 | Manual UUID parsing, no audit logging |
| Finance Accounting – Query | 3 | 3 | 0 | Fully compliant |
| Finance Accounting – Command | 5 | 2 | 3 | Missing BeanParam on create/post actions |
| Finance Accounting – Dimensions | 11 | 0 | 11 | Zero validation, highest risk |
| Financial AP | 11 | 9 | 2 | Create vendor endpoints violate policy |
| Financial AR | 10 | 8 | 2 | Create customer endpoints violate policy |
| API Gateway | 8 | 8 | 0 | Reference implementation |
| **Total** | **62** | **34** | **28** | **55% compliance** |

**Key Risks**
1. Finance dimension APIs accept unvalidated tenant/company data (SOX blocker).
2. Identity services emit non-localized 500 errors; no audit trail for auth changes.
3. Finance command APIs bypass validation metrics and rate-limits.

## 3. Goals & Success Criteria
- **G1**: Reach **100% BeanParam adoption** for all non-exempt REST endpoints by **Q2 2026**.
- **G2**: Eliminate manual UUID/enum parsing in resources; validation exceptions return 400/422 with localized codes.
- **G3**: Emit validation audit + metrics for every failure path (ValidationMetricsFilter counters + audit log entries).
- **G4**: Prevent regressions via automated checks (ArchUnit/ktlint + PR checklist).

**Measurable KPIs**
- Compliance dashboard shows 0 outstanding violations.
- Validation failure P95 latency \< 1 ms (validated in perf tests).
- Audit pipeline contains entries for ≥ 99% of 4xx responses in finance + identity services.

## 4. Implementation Waves

### Wave 1 – Finance Dimension Resource (Weeks 1-2)
**Scope**: `bounded-contexts/financial-management/financial-accounting/accounting-infrastructure/.../FinanceDimensionResource.kt`

**Affected Endpoints (11 total)**:
1. `POST /company-codes` - createCompanyCode
2. `GET /company-codes` - listCompanyCodes
3. `PUT /company-codes/{code}` - updateCompanyCode
4. `DELETE /company-codes/{code}` - deleteCompanyCode
5. `POST /dimensions/{dimensionType}` - createDimension
6. `GET /dimensions/{dimensionType}` - listDimensions
7. `PUT /dimensions/{dimensionType}/{code}` - updateDimension
8. `DELETE /dimensions/{dimensionType}/{code}` - deleteDimension
9. `POST /fiscal-year-variants` - createFiscalYearVariant
10. `GET /fiscal-year-variants` - listFiscalYearVariants
11. `POST /period-controls/blackout` - scheduleBlackout

**Implementation Steps**:
1. **Create DTO Package Structure**
   ```
   accounting-infrastructure/src/main/kotlin/com/erp/finance/accounting/infrastructure/adapter/input/rest/dto/
   ├── companycode/
   │   ├── CompanyCodeQueryParams.kt
   │   ├── CompanyCodePathParams.kt
   │   └── CreateCompanyCodeRequest.kt
   ├── dimension/
   │   ├── DimensionQueryParams.kt
   │   ├── DimensionPathParams.kt
   │   └── DimensionRequest.kt
   └── fiscalyear/
       └── FiscalYearQueryParams.kt
   ```

2. **Create BeanParam DTOs per endpoint** with validation annotations:
   - `@field:NotNull` for required tenant/company/dimension identifiers
   - `@field:Pattern` for dimension types (cost-center|profit-center|business-area|etc.)
   - `@field:Valid` for nested request bodies
   - Custom validators: `@ValidDimensionType`, `@ValidFiscalYearVariant`

3. **Implement ValidationException Handling**
   - Move `parseDimensionType()` logic into DTO property validators
   - Replace manual UUID.fromString() with DTO property binding
   - Throw `FinanceValidationException` with error codes from ValidationMessages bundle

4. **Add Comprehensive Tests**
   ```kotlin
   // Example test structure
   @QuarkusTest
   @TestProfile(FinanceDimensionValidationTestProfile::class)
   class FinanceDimensionResourceValidationTest {
       @Test
       fun `createCompanyCode with missing tenantId returns 400`()
       
       @Test
       fun `createDimension with invalid dimensionType returns 422`()
       
       @Test
       fun `listCompanyCodes with malformed UUID returns 400`()
   }
   ```

5. **Update OpenAPI Annotations**
   - Add `@Schema` descriptions to all DTOs
   - Document error response schemas (400/422/500)
   - Include example values for enum types

6. **Verify Audit & Metrics**
   - Confirm `ValidationMetricsFilter` increments counters
   - Verify audit log entries contain tenant/dimension context
   - Test circuit breaker behavior under validation load

**Exit Criteria**
1. ✅ All 11 violations resolved; zero manual parsing in resource methods
2. ✅ Test coverage ≥ 90% for validation paths (measured by JaCoCo)
3. ✅ ADR-010 compliance checklist updated with dimension module status
4. ✅ OpenAPI spec regenerated with new DTO schemas
5. ✅ Validation metrics visible in Grafana dashboard
6. ✅ Documentation in `REST_VALIDATION_IMPLEMENTATION_SUMMARY.md` updated with DTO catalog

### Wave 2 – Identity Auth Resource (Weeks 3-4)
**Scope**: `bounded-contexts/tenancy-identity/identity-infrastructure/.../AuthResource.kt`

**Affected Endpoints (10 total)**:
1. `POST /users` - createUser
2. `POST /auth/login` - authenticate
3. `POST /users/{userId}/roles` - assignRole
4. `DELETE /users/{userId}/roles/{roleId}` - revokeRole
5. `POST /users/{userId}/activate` - activateUser
6. `POST /users/{userId}/suspend` - suspendUser
7. `POST /users/{userId}/reactivate` - reactivateUser
8. `POST /users/{userId}/reset-password` - resetPassword
9. `PUT /users/{userId}/credentials` - updateCredentials
10. `GET /users/{userId}` - getUser

**Current Violations**:
- Manual UUID parsing: `UUID.fromString(userIdRaw)` throws unhandled IllegalArgumentException → HTTP 500
- No tenant context validation on user operations
- Missing audit log entries for auth lifecycle changes
- No localized error messages for validation failures

**Implementation Steps**:

1. **Create Path Parameter DTOs**
   ```kotlin
   // Example: User path params
   data class UserPathParams(
       @field:PathParam("userId")
       @field:NotNull(message = "{identity.user.id.required}")
       var userId: UUID? = null,
       
       @field:HeaderParam("Accept-Language")
       var acceptLanguage: String? = null
   ) {
       fun getUserId(locale: Locale): UUID =
           userId ?: throw IdentityValidationException(
               code = "IDENTITY_USER_ID_REQUIRED",
               locale = locale
           )
       
       fun locale(): Locale = 
           acceptLanguage?.let { Locale.forLanguageTag(it) } ?: Locale.ENGLISH
   }
   ```

2. **Create Request Body DTOs with Validation**
   ```kotlin
   data class CreateUserRequest(
       @field:NotNull(message = "{identity.tenant.id.required}")
       var tenantId: UUID? = null,
       
       @field:NotBlank(message = "{identity.username.required}")
       @field:Size(min = 3, max = 50, message = "{identity.username.size}")
       @field:Pattern(
           regexp = "^[a-zA-Z0-9._-]+$",
           message = "{identity.username.pattern}"
       )
       var username: String? = null,
       
       @field:Email(message = "{identity.email.invalid}")
       var email: String? = null,
       
       @field:NotNull(message = "{identity.password.required}")
       @field:ValidPassword  // Custom validator
       var password: String? = null
   ) : BeanParam()
   ```

3. **Add Custom Validators**
   ```kotlin
   @Target(AnnotationTarget.FIELD)
   @Retention(AnnotationRetention.RUNTIME)
   @Constraint(validatedBy = [PasswordValidator::class])
   annotation class ValidPassword(
       val message: String = "{identity.password.invalid}",
       val groups: Array<KClass<*>> = [],
       val payload: Array<KClass<out Payload>> = []
   )
   
   class PasswordValidator : ConstraintValidator<ValidPassword, String> {
       override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
           if (value == null) return true
           return value.length >= 12 && 
                  value.any { it.isUpperCase() } &&
                  value.any { it.isLowerCase() } &&
                  value.any { it.isDigit() } &&
                  value.any { !it.isLetterOrDigit() }
       }
   }
   ```

4. **Update Resource Methods**
   ```kotlin
   // Before (❌ Non-compliant)
   @POST
   @Path("/users/{userId}/activate")
   fun activateUser(
       @PathParam("userId") userIdRaw: String
   ): Response =
       parseUuid(userIdRaw).let { userId ->
           identityCommandUseCase.activateUser(ActivateUserCommand(userId))
           Response.ok().build()
       }
   
   // After (✅ Compliant)
   @POST
   @Path("/users/{userId}/activate")
   fun activateUser(
       @Valid @BeanParam pathParams: UserPathParams
   ): Response {
       val locale = pathParams.locale()
       val userId = pathParams.getUserId(locale)
       identityCommandUseCase.activateUser(
           ActivateUserCommand(userId = userId, locale = locale)
       )
       return Response.ok().build()
   }
   ```

5. **Add Localization Files**
   ```properties
   # ValidationMessages_en.properties
   identity.user.id.required=User ID is required
   identity.tenant.id.required=Tenant ID is required
   identity.username.required=Username is required
   identity.username.size=Username must be between {min} and {max} characters
   identity.username.pattern=Username can only contain letters, numbers, dots, underscores, and hyphens
   identity.email.invalid=Invalid email address
   identity.password.required=Password is required
   identity.password.invalid=Password must be at least 12 characters with uppercase, lowercase, digit, and special character
   ```

6. **Implement Integration Tests**
   ```kotlin
   @QuarkusTest
   @TestProfile(AuthValidationTestProfile::class)
   class AuthResourceValidationTest {
       @Test
       fun `createUser with invalid UUID tenantId returns 400`() {
           given()
               .contentType(ContentType.JSON)
               .body("""{"tenantId": "invalid-uuid", "username": "test"}""")
           .`when`()
               .post("/api/v1/identity/users")
           .then()
               .statusCode(400)
               .body("code", equalTo("VALIDATION_FAILED"))
               .body("message", containsString("Invalid UUID"))
       }
       
       @Test
       fun `activateUser with malformed userId returns 400`() {
           given()
           .`when`()
               .post("/api/v1/identity/users/not-a-uuid/activate")
           .then()
               .statusCode(400)
               .body("code", equalTo("IDENTITY_USER_ID_REQUIRED"))
       }
       
       @Test
       fun `createUser with weak password returns 422`() {
           given()
               .contentType(ContentType.JSON)
               .body("""{"tenantId": "${UUID.randomUUID()}", "username": "test", "password": "weak"}""")
           .`when`()
               .post("/api/v1/identity/users")
           .then()
               .statusCode(422)
               .body("code", equalTo("IDENTITY_PASSWORD_INVALID"))
       }
       
       @Test
       fun `assignRole emits audit log entry`() {
           // ... verify audit log contains userId, roleId, tenantId, timestamp
       }
   }
   ```

7. **Update OpenAPI Spec**
   - Document all path/query/body parameters with examples
   - Add error response schemas for 400/401/422/500
   - Include security requirements (JWT bearer token)

**Exit Criteria**
1. ✅ All 10 violations removed; zero manual UUID parsing
2. ✅ Validation metrics exported via Micrometer: `identity_validation_failures_total{endpoint, error_code}`
3. ✅ Audit log entries contain: tenantId, userId, operation, timestamp, IP address
4. ✅ Localized error messages for all 6 supported languages (EN, DE, ES, FR, JA, ZH)
5. ✅ Integration test coverage ≥ 85% for validation paths
6. ✅ Auth runbook updated with new validation behavior and troubleshooting guide
7. ✅ OpenAPI spec published with updated schemas

### Wave 3 – Finance Command Resource (Week 5)
**Scope**: `.../FinanceCommandResource.kt`

**Affected Endpoints (3 total)**:
1. `POST /ledgers` - createLedger
2. `POST /journal-entries` - postJournalEntry
3. `POST /periods/{periodId}/close` - closePeriod

**Current Violations**:
- Command requests missing `@BeanParam` annotation
- No automatic audit logging of SOX-critical operations
- Validation metrics not captured
- Inconsistent with query endpoints (FinanceQueryResource is compliant)

**Implementation Steps**:

1. **Create Command Request DTOs**
   ```kotlin
   data class CreateLedgerRequest(
       @field:NotNull(message = "{finance.tenant.id.required}")
       var tenantId: UUID? = null,
       
       @field:NotNull(message = "{finance.company.code.required}")
       @field:ValidCompanyCode  // Custom validator checking existence
       var companyCode: String? = null,
       
       @field:NotBlank(message = "{finance.ledger.name.required}")
       @field:Size(max = 100, message = "{finance.ledger.name.size}")
       var name: String? = null,
       
       @field:NotNull(message = "{finance.ledger.type.required}")
       @field:ValidLedgerType  // Enum: GENERAL, SUBSIDIARY, CONSOLIDATION
       var type: LedgerType? = null,
       
       @field:NotNull(message = "{finance.currency.required}")
       @field:Pattern(regexp = "[A-Z]{3}", message = "{finance.currency.pattern}")
       var currency: String? = null,
       
       @field:HeaderParam("Accept-Language")
       var acceptLanguage: String? = null
   ) : BeanParam() {
       fun locale(): Locale = 
           acceptLanguage?.let { Locale.forLanguageTag(it) } ?: Locale.ENGLISH
   }
   
   data class PostJournalEntryRequest(
       @field:NotNull(message = "{finance.tenant.id.required}")
       var tenantId: UUID? = null,
       
       @field:NotNull(message = "{finance.company.code.required}")
       var companyCode: String? = null,
       
       @field:NotNull(message = "{finance.entry.date.required}")
       @field:ValidBusinessDate  // Custom validator: not holiday, not closed period
       var postingDate: LocalDate? = null,
       
       @field:NotBlank(message = "{finance.entry.reference.required}")
       var reference: String? = null,
       
       @field:Valid
       @field:NotEmpty(message = "{finance.entry.lines.required}")
       @field:Size(min = 2, message = "{finance.entry.lines.min}")
       @field:BalancedJournalLines  // Custom validator: debits = credits
       var lines: List<JournalLineRequest>? = null,
       
       @field:HeaderParam("Accept-Language")
       var acceptLanguage: String? = null
   ) : BeanParam()
   ```

2. **Implement Custom Validators**
   ```kotlin
   @Constraint(validatedBy = [CompanyCodeValidator::class])
   annotation class ValidCompanyCode
   
   class CompanyCodeValidator @Inject constructor(
       private val companyCodeCache: CompanyCodeExistenceCache
   ) : ConstraintValidator<ValidCompanyCode, String> {
       override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
           if (value == null) return true
           return companyCodeCache.exists(value)
       }
   }
   
   @Constraint(validatedBy = [BalancedJournalLinesValidator::class])
   annotation class BalancedJournalLines
   
   class BalancedJournalLinesValidator : 
       ConstraintValidator<BalancedJournalLines, List<JournalLineRequest>> {
       override fun isValid(
           value: List<JournalLineRequest>?,
           context: ConstraintValidatorContext
       ): Boolean {
           if (value == null) return true
           val debits = value.filter { it.debitAmount != null }
               .sumOf { it.debitAmount!! }
           val credits = value.filter { it.creditAmount != null }
               .sumOf { it.creditAmount!! }
           return debits == credits
       }
   }
   ```

3. **Update Resource Methods**
   ```kotlin
   // Before (❌ Non-compliant)
   @POST
   @Path("/ledgers")
   fun createLedger(@Valid request: CreateLedgerRequest): Response {
       val result = financeCommandUseCase.createLedger(request.toCommand())
       return Response.created(URI.create("/ledgers/${result.ledgerId}"))
           .entity(result)
           .build()
   }
   
   // After (✅ Compliant)
   @POST
   @Path("/ledgers")
   @Counted(name = "finance_ledger_create_requests")
   @Timed(name = "finance_ledger_create_duration")
   fun createLedger(
       @Valid @BeanParam request: CreateLedgerRequest
   ): Response {
       val locale = request.locale()
       val command = request.toCommand().copy(locale = locale)
       
       // Audit log automatically captured by ValidationAuditFilter
       val result = financeCommandUseCase.createLedger(command)
       
       return Response.created(URI.create("/ledgers/${result.ledgerId}"))
           .entity(result)
           .build()
   }
   ```

4. **Ensure Audit + Metrics Wiring**
   - Verify `ValidationMetricsFilter` increments counters for validation failures
   - Confirm `FinanceAuditFilter` logs all command operations with tenant context
   - Test that circuit breaker (`@CircuitBreaker`) still triggers on downstream failures
   - Validate rate limiter (`@RateLimit`) applies to command endpoints

5. **Add Integration Tests**
   ```kotlin
   @QuarkusTest
   @TestProfile(FinanceCommandValidationTestProfile::class)
   class FinanceCommandResourceValidationTest {
       @Test
       fun `createLedger with invalid companyCode returns 422`() {
           given()
               .contentType(ContentType.JSON)
               .body("""
                   {
                       "tenantId": "${UUID.randomUUID()}",
                       "companyCode": "INVALID",
                       "name": "Test Ledger",
                       "type": "GENERAL",
                       "currency": "USD"
                   }
               """)
           .`when`()
               .post("/api/v1/finance/ledgers")
           .then()
               .statusCode(422)
               .body("code", equalTo("FINANCE_COMPANY_CODE_INVALID"))
       }
       
       @Test
       fun `postJournalEntry with unbalanced lines returns 422`() {
           given()
               .contentType(ContentType.JSON)
               .body("""
                   {
                       "tenantId": "${UUID.randomUUID()}",
                       "companyCode": "1000",
                       "postingDate": "2025-11-16",
                       "reference": "TEST001",
                       "lines": [
                           {"account": "100000", "debitAmount": 100.00},
                           {"account": "200000", "creditAmount": 99.00}
                       ]
                   }
               """)
           .`when`()
               .post("/api/v1/finance/journal-entries")
           .then()
               .statusCode(422)
               .body("code", equalTo("FINANCE_ENTRY_UNBALANCED"))
       }
       
       @Test
       fun `createLedger success emits audit log and metrics`() {
           // Verify audit log contains: userId, tenantId, companyCode, operation, timestamp
           // Verify Prometheus metrics incremented: finance_ledger_create_requests_total
       }
   }
   ```

6. **Performance Regression Testing**
   - Baseline: Measure P50/P95/P99 latency before BeanParam conversion
   - Target: No more than 5% increase in latency after conversion
   - Load test: 1000 req/s for 5 minutes, verify circuit breaker doesn't trip
   - Cache validation: Ensure CompanyCodeExistenceCache hit rate > 95%

**Exit Criteria**
1. ✅ All 3 finance command endpoints compliant with ADR-010
2. ✅ Validation audit logs contain SOX-required fields (user, tenant, operation, data changes)
3. ✅ Prometheus metrics exported: `finance_validation_failures_total{endpoint, error_code}`
4. ✅ Circuit breaker + rate limiter verified working with BeanParam
5. ✅ Performance regression tests pass (< 5% latency increase)
6. ✅ Observability dashboards updated with new validation metrics panels
7. ✅ Finance runbook updated with validation troubleshooting procedures

### Wave 4 – AP & AR Create Endpoints (Week 6)
**Scope**: `VendorResource.registerVendor`, `CustomerResource.registerCustomer`

**Affected Endpoints (4 total)**:
1. `POST /vendors` - registerVendor (AP module)
2. `POST /vendors/{vendorId}/contacts` - addVendorContact (AP module)
3. `POST /customers` - registerCustomer (AR module)
4. `POST /customers/{customerId}/contacts` - addCustomerContact (AR module)

**Current Violations**:
- Create endpoints use `@Valid request: XxxRequest` without `@BeanParam`
- Missing automatic audit logging for master data creation
- Validation metrics not captured
- Inconsistent with query/update endpoints (which ARE compliant)

**Implementation Steps**:

1. **Align with Existing Update Endpoint Patterns**
   
   **Current Update Endpoint (✅ Compliant)**:
   ```kotlin
   @PUT
   @Path("/{vendorId}")
   fun updateVendor(
       @Valid @BeanParam vendorPath: VendorPathParams,
       @Valid request: VendorRequest,
   ): VendorResponse
   ```
   
   **Current Create Endpoint (❌ Non-compliant)**:
   ```kotlin
   @POST
   fun registerVendor(
       @Valid request: VendorRequest,  // Missing @BeanParam
   ): Response
   ```

2. **Add BeanParam to Request Bodies**
   ```kotlin
   // Update VendorRequest to extend BeanParam
   data class VendorRequest(
       @field:NotNull(message = "{vendor.tenant.id.required}")
       @field:HeaderParam("X-Tenant-ID")  // Can also come from header
       var tenantId: UUID? = null,
       
       @field:NotBlank(message = "{vendor.code.required}")
       @field:Size(max = 20, message = "{vendor.code.size}")
       @field:Pattern(regexp = "^[A-Z0-9-]+$", message = "{vendor.code.pattern}")
       var vendorCode: String? = null,
       
       @field:NotBlank(message = "{vendor.name.required}")
       @field:Size(max = 200, message = "{vendor.name.size}")
       var name: String? = null,
       
       @field:Valid
       @field:NotNull(message = "{vendor.address.required}")
       var address: AddressRequest? = null,
       
       @field:NotNull(message = "{vendor.payment.terms.required}")
       @field:ValidPaymentTerms  // Custom validator
       var paymentTerms: PaymentTermsRequest? = null,
       
       @field:HeaderParam("Accept-Language")
       var acceptLanguage: String? = null
   ) : BeanParam() {
       fun locale(): Locale = 
           acceptLanguage?.let { Locale.forLanguageTag(it) } ?: Locale.ENGLISH
   }
   
   // Similarly for CustomerRequest
   data class CustomerRequest(
       @field:NotNull(message = "{customer.tenant.id.required}")
       @field:HeaderParam("X-Tenant-ID")
       var tenantId: UUID? = null,
       
       @field:NotBlank(message = "{customer.code.required}")
       @field:Size(max = 20, message = "{customer.code.size}")
       var customerCode: String? = null,
       
       @field:NotBlank(message = "{customer.name.required}")
       @field:Size(max = 200, message = "{customer.name.size}")
       var name: String? = null,
       
       @field:Valid
       @field:NotNull(message = "{customer.address.required}")
       var address: AddressRequest? = null,
       
       @field:NotNull(message = "{customer.credit.limit.required}")
       @field:DecimalMin(value = "0.0", message = "{customer.credit.limit.min}")
       var creditLimit: BigDecimal? = null,
       
       @field:HeaderParam("Accept-Language")
       var acceptLanguage: String? = null
   ) : BeanParam()
   ```

3. **Update Resource Methods**
   ```kotlin
   // AP - VendorResource
   // Before (❌)
   @POST
   fun registerVendor(@Valid request: VendorRequest): Response
   
   // After (✅)
   @POST
   @Counted(name = "ap_vendor_register_requests")
   @Timed(name = "ap_vendor_register_duration")
   fun registerVendor(
       @Valid @BeanParam request: VendorRequest
   ): Response {
       val locale = request.locale()
       val command = request.toCommand().copy(locale = locale)
       val result = vendorCommandUseCase.registerVendor(command)
       return Response.created(URI.create("/vendors/${result.vendorId}"))
           .entity(result)
           .build()
   }
   
   // AR - CustomerResource (same pattern)
   @POST
   @Counted(name = "ar_customer_register_requests")
   @Timed(name = "ar_customer_register_duration")
   fun registerCustomer(
       @Valid @BeanParam request: CustomerRequest
   ): Response
   ```

4. **Reuse Validation Utilities from Update Endpoints**
   - Share `VendorCodeValidator` between create and update
   - Share `CustomerCodeValidator` between create and update
   - Share `PaymentTermsValidator`, `AddressValidator`
   - Avoid duplication by extracting validators to `financial-shared` module

5. **Extend Existing Test Suites**
   ```kotlin
   @QuarkusTest
   @TestProfile(ApValidationTestProfile::class)
   class VendorResourceValidationTest {
       // Existing update tests...
       
       @Test
       fun `registerVendor with duplicate vendorCode returns 422`() {
           given()
               .contentType(ContentType.JSON)
               .header("X-Tenant-ID", testTenantId)
               .body("""
                   {
                       "vendorCode": "V001",
                       "name": "Test Vendor",
                       "address": {...},
                       "paymentTerms": {...}
                   }
               """)
           .`when`()
               .post("/api/v1/ap/vendors")
           .then()
               .statusCode(422)
               .body("code", equalTo("VENDOR_CODE_DUPLICATE"))
       }
       
       @Test
       fun `registerVendor with invalid paymentTerms returns 422`() {
           // Verify custom validator works
       }
       
       @Test
       fun `registerVendor success increments cache and emits audit log`() {
           // Verify VendorExistenceCache updated
           // Verify audit log contains: tenantId, vendorCode, userId, operation
       }
   }
   ```

6. **Validate Cache Consistency**
   - After `registerVendor()` succeeds, `VendorExistenceCache` must contain new vendor
   - After `registerCustomer()` succeeds, `CustomerExistenceCache` must contain new customer
   - Add integration tests verifying cache consistency
   - Monitor cache hit/miss ratios in Grafana dashboards

7. **Performance Validation**
   - Measure cache lookup latency (target: < 5ms P95)
   - Verify no performance degradation vs. non-BeanParam version
   - Load test: 500 vendor/customer creates per minute

**Exit Criteria**
1. ✅ All 4 create endpoints compliant with ADR-010
2. ✅ Validation utilities shared between create/update/query endpoints (DRY principle)
3. ✅ Cache consistency verified: new entities immediately available for validation
4. ✅ Audit logs contain master data creation events with full context
5. ✅ Prometheus metrics exported: `ap_validation_failures_total`, `ar_validation_failures_total`
6. ✅ Integration test coverage ≥ 90% for create validation paths
7. ✅ Cache hit rate dashboards updated: vendor_cache_hit_rate, customer_cache_hit_rate
8. ✅ AP/AR runbooks updated with validation troubleshooting sections

### Wave 5 – Enforcement & Governance (Weeks 7-8, overlaps)

**Objective**: Prevent future violations through automated checks, developer tooling, and ongoing governance.

#### **5.1 Static Analysis Rules**

**ArchUnit Rules** (`platform-shared/src/test/kotlin/com/erp/platform/architecture/RestValidationArchitectureTest.kt`):

```kotlin
@AnalyzeClasses(packages = ["com.erp"])
class RestValidationArchitectureTest {
    
    @ArchTest
    val `REST endpoints with 3+ parameters must use BeanParam` = 
        methods()
            .that().areAnnotatedWith(Path::class.java)
            .and().areAnnotatedWith(anyOf(GET::class.java, POST::class.java, PUT::class.java, DELETE::class.java))
            .and().haveParameterCountGreaterThanOrEqualTo(3)
            .should().haveRawParameterOfType(BeanParam::class.java)
            .because("ADR-010 mandates BeanParam for endpoints with 3+ parameters")
    
    @ArchTest
    val `REST endpoints with tenantId parameter must use BeanParam` =
        methods()
            .that().areAnnotatedWith(anyOf(GET::class.java, POST::class.java, PUT::class.java, DELETE::class.java))
            .and().haveRawParameterOfType(PathParam::class.java, "tenantId")
                .or().haveRawParameterOfType(QueryParam::class.java, "tenantId")
            .should().haveRawParameterAnnotatedWith(BeanParam::class.java)
            .because("ADR-010 mandates BeanParam for all multi-tenant endpoints")
    
    @ArchTest
    val `REST endpoints parsing UUID must use BeanParam DTO` =
        methods()
            .that().areAnnotatedWith(anyOf(GET::class.java, POST::class.java, PUT::class.java, DELETE::class.java))
            .and().containCodeUnit("UUID.fromString")
            .should().notContainCodeUnit("UUID.fromString")
            .because("Manual UUID parsing creates HTTP 500 risk; use BeanParam with UUID property instead")
    
    @ArchTest
    val `health and metrics endpoints must NOT use BeanParam` =
        methods()
            .that().haveNameMatching(".*(health|liveness|readiness|metrics).*")
            .should().notHaveRawParameterAnnotatedWith(BeanParam::class.java)
            .because("ADR-010 prohibits BeanParam for health/metrics endpoints")
    
    @ArchTest
    val `financial module endpoints must use BeanParam for audit compliance` =
        methods()
            .that().areAnnotatedWith(anyOf(POST::class.java, PUT::class.java, DELETE::class.java))
            .and().areDeclaredInClassesThat().resideInAPackage("..financial..")
            .should().haveRawParameterAnnotatedWith(BeanParam::class.java)
            .andShould().haveRawParameterAnnotatedWith(Valid::class.java)
            .because("ADR-010 mandates BeanParam for SOX audit compliance in finance modules")
}
```

**Gradle Integration**:
```kotlin
// build-logic/src/main/kotlin/erp.kotlin-common-conventions.gradle.kts
tasks.register("checkRestValidation") {
    dependsOn("test")
    group = "verification"
    description = "Validates REST endpoints comply with ADR-010 BeanParam policy"
    doLast {
        logger.lifecycle("REST validation compliance check completed via ArchUnit tests")
    }
}

tasks.named("check") {
    dependsOn("checkRestValidation")
}
```

#### **5.2 ktlint Custom Rules**

**Custom ktlint Rule** (optional, supplements ArchUnit):
```kotlin
// build-logic/src/main/kotlin/ktlint/BeanParamRule.kt
class BeanParamRule : Rule("beanparam-required") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == FUN && node.hasRestAnnotation()) {
            val params = node.findChildByType(VALUE_PARAMETER_LIST)
            if (params?.children?.count() ?: 0 >= 3 && !params.hasBeanParamAnnotation()) {
                emit(
                    node.startOffset,
                    "REST endpoint with 3+ parameters must use @BeanParam (ADR-010)",
                    false
                )
            }
        }
    }
}
```

#### **5.3 PR Checklist Template**

**Update `.github/PULL_REQUEST_TEMPLATE.md`**:
```markdown
## REST API Changes Checklist (if applicable)

- [ ] **BeanParam Compliance (ADR-010)**
  - [ ] Endpoints with 3+ parameters use `@Valid @BeanParam`
  - [ ] Multi-tenant endpoints (tenantId) use `@Valid @BeanParam`
  - [ ] UUID/Date/Enum parsing done in DTOs, not manually in resource methods
  - [ ] Financial/transactional endpoints use `@Valid @BeanParam` for audit compliance
  - [ ] Health/metrics endpoints do NOT use BeanParam
  
- [ ] **Validation & Error Handling**
  - [ ] Validation messages localized in `ValidationMessages_*.properties`
  - [ ] Custom validators documented with usage examples
  - [ ] Error responses tested for 400/422/500 with correct error codes
  
- [ ] **Testing**
  - [ ] Integration tests cover validation failure paths (400/422)
  - [ ] Audit log entries verified for financial operations
  - [ ] Validation metrics verified in Prometheus
  
- [ ] **Documentation**
  - [ ] OpenAPI spec updated with new DTO schemas
  - [ ] Runbook updated if validation behavior changes
  - [ ] Exception documented if BeanParam deliberately omitted (ARB approval required)
```

#### **5.4 Compliance Dashboard**

**Grafana Dashboard** (`dashboards/grafana/rest-validation-compliance.json`):

```json
{
  "dashboard": {
    "title": "REST Validation Compliance (ADR-010)",
    "panels": [
      {
        "title": "BeanParam Adoption Rate",
        "targets": [{
          "expr": "(count(rest_endpoints{uses_beanparam='true'}) / count(rest_endpoints)) * 100"
        }],
        "type": "gauge",
        "thresholds": {
          "steps": [
            {"value": 0, "color": "red"},
            {"value": 90, "color": "yellow"},
            {"value": 100, "color": "green"}
          ]
        }
      },
      {
        "title": "Validation Failures by Module",
        "targets": [{
          "expr": "sum by (module, error_code) (increase(validation_failures_total[1h]))"
        }],
        "type": "graph"
      },
      {
        "title": "Validation Latency P95",
        "targets": [{
          "expr": "histogram_quantile(0.95, validation_duration_seconds_bucket)"
        }],
        "type": "graph",
        "thresholds": [
          {"value": 0.001, "color": "green"},
          {"value": 0.005, "color": "yellow"},
          {"value": 0.010, "color": "red"}
        ]
      },
      {
        "title": "Manual UUID Parsing Violations",
        "targets": [{
          "expr": "count(rest_endpoints{manual_uuid_parsing='true'})"
        }],
        "type": "stat",
        "thresholds": {
          "steps": [
            {"value": 0, "color": "green"},
            {"value": 1, "color": "red"}
          ]
        }
      },
      {
        "title": "Audit Log Coverage (Finance Modules)",
        "targets": [{
          "expr": "(count(audit_log_entries{module=~'finance.*'}) / count(http_requests{module=~'finance.*', method=~'POST|PUT|DELETE'})) * 100"
        }],
        "type": "gauge",
        "thresholds": {
          "steps": [
            {"value": 0, "color": "red"},
            {"value": 95, "color": "yellow"},
            {"value": 99, "color": "green"}
          ]
        }
      }
    ]
  }
}
```

#### **5.5 Architecture Review Board (ARB) Process**

**Exception Request Template** (`docs/templates/beanparam-exception-request.md`):

```markdown
# BeanParam Exception Request

**Requester**: [Name, Team]  
**Date**: [YYYY-MM-DD]  
**Endpoint**: [HTTP Method + Path]  
**Module**: [e.g., financial-accounting]  

## Exception Details

**Reason for Exception**:
[Explain why BeanParam cannot be used]

**Policy Section**: [Which MUST/SHOULD requirement is violated?]

**Alternative Approach**:
[How will validation/audit/metrics be handled instead?]

**Business Impact**:
[What happens if this exception is denied?]

**Security/Compliance Review**:
[ ] No PII/financial data in this endpoint
[ ] No SOX audit requirements
[ ] No multi-tenant concerns

## Approval

**ARB Decision**: [ ] Approved [ ] Denied  
**Sunset Date**: [YYYY-MM-DD] (6 months from approval)  
**Review Date**: [YYYY-MM-DD] (before sunset)  
**Conditions**: [Any conditions for approval]

**Signatures**:
- Architecture Lead: _______________
- Security Lead: _______________
- Compliance Lead: _______________
```

**ARB Meeting Cadence**:
- **Quarterly reviews** of all exceptions
- **Emergency reviews** for urgent cases (48-hour SLA)
- **Sunset enforcement**: Exceptions expire after 6 months, require renewal

#### **5.6 Developer Training & Documentation**

**Training Materials**:
1. **Onboarding Guide**: `docs/developer-guides/rest-validation-guide.md`
   - Why BeanParam matters (audit, metrics, validation)
   - Step-by-step DTO creation guide
   - Common pitfalls and solutions
   - Before/after examples

2. **Video Tutorial**: 15-minute walkthrough (recorded)
   - Creating BeanParam DTOs
   - Adding custom validators
   - Testing validation paths
   - Reviewing metrics/audit logs

3. **Code Examples Repository**: `examples/rest-validation/`
   - Query endpoint examples
   - Command endpoint examples
   - Multi-tenant endpoint examples
   - Exception handling examples

**Documentation Updates**:
- Update `CONTRIBUTING.md` with BeanParam requirements
- Add validation checklist to `docs/ARCHITECTURE.md`
- Create troubleshooting guide: `docs/REST_VALIDATION_TROUBLESHOOTING.md`

#### **5.7 Continuous Monitoring**

**Weekly Compliance Report** (automated):
```bash
#!/usr/bin/env bash
# scripts/check-rest-compliance.sh

echo "=== REST Validation Compliance Report ==="
echo "Generated: $(date)"
echo ""

# Run ArchUnit tests
./gradlew checkRestValidation --quiet

# Count violations
violations=$(grep -r "VIOLATION" build/reports/tests/ | wc -l)
echo "Total violations: $violations"

# List violating endpoints
echo "Violating endpoints:"
grep -r "@GET\|@POST\|@PUT\|@DELETE" --include="*Resource.kt" \
  | grep -v "@BeanParam" \
  | grep -E "(tenantId|userId|UUID\.fromString)" \
  | sort

echo ""
echo "Target: 0 violations by Q2 2026"
```

**Slack Integration**:
```kotlin
// Post weekly compliance report to #architecture channel
// Alert on new violations detected in PRs
// Celebrate milestones (50%, 75%, 100% compliance)
```

**Exit Criteria**
1. ✅ ArchUnit rules pass on all modules (0 violations)
2. ✅ ktlint custom rule integrated into CI pipeline
3. ✅ PR checklist enforced via GitHub required checks
4. ✅ Compliance dashboard published and accessible to all teams
5. ✅ ARB exception process documented and approved
6. ✅ Developer training materials published
7. ✅ Weekly compliance report automated and running
8. ✅ Zero new violations introduced in Q1 2026 (regression prevention verified)

## 5. Timeline Overview

| Week | Milestone | Owner | Dependencies | Risk Level |
| --- | --- | --- | --- | --- |
| 1 | FinanceDimension DTO scaffolding complete | Finance Engineering | Validation utilities in `financial-shared` | Low |
| 2 | FinanceDimension tests + docs done | Finance Engineering + QA | Localization bundles | Low |
| 3 | AuthResource DTOs merged | Identity Engineering | Audit filter wiring | Medium |
| 4 | Identity validation tests + documentation complete | Identity Engineering + QA | OpenAPI spec updates | Medium |
| 5 | Finance command endpoints compliant | Finance Engineering | Circuit breaker config | Low |
| 6 | AP/AR create endpoints compliant | Finance Engineering | Cache consistency validation | Low |
| 7 | Static analysis rule merged | Platform Architecture | ArchUnit setup complete | Low |
| 8 | Compliance dashboard live; project close-out review | Platform Architecture + ARB | Grafana provisioning | Low |

**Critical Path**: Weeks 1-2 (FinanceDimension) → Weeks 3-4 (AuthResource)  
**Parallel Work**: Weeks 5-6 can start after Week 2 completion  
**Governance Setup**: Weeks 7-8 run in parallel with Weeks 5-6

**Total Duration**: 8 weeks (target completion: **January 11, 2026**)  
**Buffer**: 2 weeks for unexpected issues (adjusted target: **January 25, 2026**)

## 6. Dependencies & Resourcing

### **6.1 Team Allocation**

| Team | Role | Effort (hours/week) | Duration (weeks) |
| --- | --- | --- | --- |
| **Finance Engineering** | Implement Waves 1, 3, 4 | 20-25 | 6 weeks |
| **Identity Engineering** | Implement Wave 2 | 20-25 | 2 weeks |
| **Platform Architecture** | Implement Wave 5, code reviews | 10-15 | 8 weeks |
| **QA Automation** | Test coverage, validation testing | 15-20 | 8 weeks |
| **Technical Writing** | Documentation updates | 5-10 | 8 weeks |
| **DevOps** | Grafana dashboards, CI integration | 5-10 | 2 weeks (Weeks 7-8) |

**Total Effort**: ~600-800 person-hours over 8 weeks

### **6.2 Shared Module Dependencies**

**Validation Utilities** (`platform-shared/validation/`):
- `TenantValidator.kt` - Validates tenant context
- `LocaleExtractor.kt` - Extracts locale from headers
- `UuidValidator.kt` - Safe UUID parsing with error codes
- `ValidationExceptionMapper.kt` - Maps validation failures to HTTP responses

**Localization Bundles** (`platform-shared/resources/`):
- `ValidationMessages_en.properties` (English)
- `ValidationMessages_de.properties` (German)
- `ValidationMessages_es.properties` (Spanish)
- `ValidationMessages_fr.properties` (French)
- `ValidationMessages_ja.properties` (Japanese)
- `ValidationMessages_zh.properties` (Chinese)

**Audit Infrastructure** (`platform-infrastructure/observability/`):
- `ValidationAuditFilter.kt` - Captures validation events
- `ValidationMetricsFilter.kt` - Exports Prometheus metrics
- `AuditLogPublisher.kt` - Publishes to audit log stream

### **6.3 Tooling & Infrastructure**

**Build System**:
- Gradle 8.x with Kotlin DSL
- Quarkus 3.x with Bean Validation (Hibernate Validator)
- ArchUnit 1.x for architecture tests
- ktlint 11.x (optional custom rule)

**Testing Framework**:
- JUnit 5 for unit/integration tests
- REST Assured for API testing
- Testcontainers for database tests
- JaCoCo for coverage reporting

**Observability Stack**:
- Prometheus for metrics collection
- Grafana for dashboards
- ELK/Loki for log aggregation
- OpenTelemetry for distributed tracing

**CI/CD Pipeline**:
- GitHub Actions (existing)
- SonarQube for code quality
- Snyk for dependency scanning

### **6.4 Training & Knowledge Transfer**

**Onboarding Sessions** (1 hour each):
- Week 1: Kickoff meeting - ADR-010 overview, project goals
- Week 3: Mid-project review - lessons learned from Waves 1-2
- Week 6: Technical deep-dive - custom validators, audit/metrics
- Week 8: Close-out presentation - final results, maintenance plan

**Pair Programming**:
- Finance Engineering pairs with Platform Architecture (Weeks 1-2)
- Identity Engineering pairs with Platform Architecture (Weeks 3-4)
- Knowledge documented in Wiki after each wave

**Documentation Deliverables**:
- Developer guide: `docs/developer-guides/rest-validation-guide.md`
- Troubleshooting guide: `docs/REST_VALIDATION_TROUBLESHOOTING.md`
- API examples: `examples/rest-validation/`
- Video walkthrough: 15-minute tutorial (published to internal training portal)

## 7. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation Strategy | Contingency Plan |
| --- | --- | --- | --- | --- |
| **Regression in DTO-to-Command conversion** | Medium | High | • Implement "golden tests" comparing command objects pre/post change<br>• Pair programming during DTO creation<br>• Comprehensive integration tests | • Keep old code path behind feature flag<br>• Quick rollback plan<br>• Hotfix process documented |
| **Missing localized strings for new error codes** | High | Medium | • Expand `ValidationMessages_*.properties` as first step<br>• Add localization checklist to PR template<br>• QA validates all 6 languages | • Use English fallback for missing translations<br>• Parallel translation work in Week 7-8<br>• Professional translation service on standby |
| **Timeline slippage due to parallel finance initiatives** | Medium | Medium | • Time-box each wave strictly<br>• Secure architecture OKR commitment<br>• Weekly status reports to ARB<br>• Buffer weeks built into schedule | • Extend timeline by 2 weeks if needed<br>• Reduce scope to P0 violations only<br>• Defer Wave 5 governance automation |
| **Performance regression from validation overhead** | Low | High | • Baseline P50/P95/P99 latencies before changes<br>• Performance tests after each wave<br>• Cache warming strategies<br>• Target: < 5% latency increase | • Optimize validator implementations<br>• Increase cache sizes/TTL<br>• Add validation circuit breaker<br>• Scale out if needed |
| **Static analysis false positives** | Medium | Low | • Provide `@SkipBeanParamCheck` annotation for approved exceptions<br>• Tune ArchUnit rules iteratively<br>• Document exemption criteria clearly | • Temporarily disable strict mode<br>• Manual review process<br>• Refine rules in Week 7-8 |
| **Cache invalidation inconsistencies** | Low | High | • Event-driven cache invalidation (Kafka)<br>• Monitor cache consistency metrics<br>• Define acceptable staleness window (5 min)<br>• Integration tests for cache behavior | • Reduce cache TTL as temporary fix<br>• Implement cache versioning<br>• Add manual cache refresh API |
| **Breaking changes to OpenAPI spec** | Medium | Medium | • Version API endpoints (v1, v2)<br>• Publish migration guide for API consumers<br>• Deprecation period for old DTOs (3 months) | • Maintain backward compatibility layer<br>• Parallel endpoints (old + new)<br>• Gradual consumer migration |
| **Resistance from development teams** | Low | Medium | • Early involvement of tech leads<br>• Show quick wins (better errors, audit logs)<br>• Highlight compliance benefits<br>• Make it easy (templates, examples) | • Executive sponsorship escalation<br>• Architecture Review Board mandate<br>• Include in performance reviews |
| **Security vulnerabilities in custom validators** | Low | Critical | • Security review of all custom validators<br>• Penetration testing after completion<br>• Input sanitization in validators<br>• Rate limiting on validation endpoints | • Emergency patching process<br>• Rollback vulnerable validators<br>• Security incident response plan |
| **Incomplete audit logging** | Low | High | • Automated tests verify audit log entries<br>• Grafana alerts for missing audit events<br>• Sample audit logs in each PR<br>• SOX compliance checklist | • Manual audit log insertion as stopgap<br>• Compliance team notification<br>• Remediation sprint if needed |

### **Risk Monitoring Dashboard**

**Weekly Risk Review** (Tuesdays, 30 minutes):
- Review open risks and mitigation progress
- Update risk scores based on new information
- Escalate critical risks to ARB immediately
- Document decisions in risk register

**Risk Register** (`docs/risks/beanparam-remediation-risks.md`):
- Live document updated throughout project
- Includes risk ID, owner, status, last review date
- Linked to project tracker for visibility

### **Early Warning Indicators**

| Indicator | Threshold | Action |
| --- | --- | --- |
| Wave completion behind schedule | > 3 days late | Review scope, add resources, or extend timeline |
| Test failures increasing | > 5% failure rate | Stop new work, focus on stabilization |
| Performance regression | > 10% latency increase | Performance optimization sprint |
| ARB exception requests | > 3 per wave | Review policy clarity, improve training |
| Developer survey sentiment | < 70% positive | Address concerns, improve tooling |

## 8. Rollout & Rollback Strategy

### **8.1 Phased Rollout Approach**

**Strategy**: Each wave follows a **dark launch → canary → full rollout** pattern

#### **Phase 1: Dark Launch (Day 1-2 of each wave)**
- Merge BeanParam code with feature flag disabled
- Code runs in production but old validation path still active
- Monitor for deployment issues, compilation errors
- Rollback available via feature flag toggle

**Feature Flag Configuration**:
```yaml
# application.yml
validation:
  beanparam:
    enabled: ${BEANPARAM_ENABLED:false}
    modules:
      finance-dimension: ${BEANPARAM_FINANCE_DIMENSION:false}
      auth-resource: ${BEANPARAM_AUTH_RESOURCE:false}
      finance-command: ${BEANPARAM_FINANCE_COMMAND:false}
      ap-ar-create: ${BEANPARAM_AP_AR_CREATE:false}
```

#### **Phase 2: Canary Release (Day 3-5)**
- Enable feature flag for **10% of traffic**
- Monitor metrics: validation latency, error rates, audit log completeness
- Compare old vs. new validation behavior
- Gradual increase: 10% → 25% → 50%

**Canary Criteria** (must pass all):
- ✅ Validation latency P95 < 1ms
- ✅ Error rate < 0.5%
- ✅ Audit log completeness > 99%
- ✅ Cache hit rate > 95%
- ✅ No customer-reported issues

#### **Phase 3: Full Rollout (Day 6-7)**
- Enable feature flag for **100% of traffic**
- Monitor for 48 hours
- Remove old validation code path after 1 week of stability
- Update documentation to reflect new behavior

### **8.2 Rollback Procedures**

#### **Level 1: Feature Flag Rollback (< 5 minutes)**
```bash
# Emergency rollback via feature flag
kubectl set env deployment/finance-accounting-service \
  BEANPARAM_FINANCE_DIMENSION=false -n production

# Verify rollback
curl https://api.erp.example.com/health | jq '.config.validation'
```

**Triggers**:
- Error rate > 1%
- Validation latency P95 > 5ms
- Audit log completeness < 95%
- Customer-reported critical issues

#### **Level 2: Code Rollback (< 30 minutes)**
```bash
# Rollback to previous Git tag
git revert <commit-sha> --no-commit
git commit -m "Rollback BeanParam changes - Wave X"
git push origin main

# Redeploy via CI/CD
gh workflow run deploy-production.yml \
  --ref main \
  --field service=finance-accounting
```

**Triggers**:
- Feature flag rollback doesn't resolve issue
- Data corruption detected
- Security vulnerability discovered
- Performance degradation > 20%

#### **Level 3: Database Rollback (< 2 hours)**
**Only if data migration issues occur** (unlikely for validation changes)
```bash
# Restore from backup (worst case)
./scripts/restore-database.sh \
  --environment production \
  --timestamp "2026-01-15T10:00:00Z"
```

### **8.3 Rollback Decision Matrix**

| Issue Severity | Response Time | Rollback Level | Decision Authority |
| --- | --- | --- | --- |
| **Critical** (production down) | < 5 minutes | Level 1 (Feature Flag) | On-call engineer |
| **Major** (error rate > 5%) | < 30 minutes | Level 2 (Code) | Tech lead + on-call |
| **Moderate** (performance degradation) | < 2 hours | Level 1 → Level 2 | Tech lead |
| **Minor** (isolated issues) | < 24 hours | Hotfix forward | Development team |

### **8.4 Health Checks & Monitoring**

**Pre-Deployment Checks**:
```bash
# Run before enabling feature flag
./scripts/pre-deployment-checks.sh
# Checks:
# - All tests passing
# - ArchUnit rules passing
# - Performance benchmarks within threshold
# - Localization files complete
# - OpenAPI spec valid
```

**Post-Deployment Monitoring** (first 24 hours):
- **Validation latency**: P50, P95, P99, Max
- **Error rates**: 400, 422, 500 by endpoint
- **Audit log completeness**: % of requests logged
- **Cache hit rates**: Vendor, customer, company code caches
- **Circuit breaker trips**: Validation circuit breaker state
- **Custom validator failures**: By validator type

**Alerting Thresholds**:
```yaml
# Prometheus alerting rules
groups:
  - name: beanparam_rollout
    rules:
      - alert: ValidationLatencyHigh
        expr: histogram_quantile(0.95, validation_duration_seconds_bucket) > 0.005
        for: 5m
        annotations:
          summary: "Validation P95 latency > 5ms"
          action: "Consider feature flag rollback"
      
      - alert: ValidationErrorRateHigh
        expr: rate(validation_failures_total[5m]) > 0.01
        for: 10m
        annotations:
          summary: "Validation error rate > 1%"
          action: "Investigate and prepare rollback"
      
      - alert: AuditLogIncompleteness
        expr: (audit_log_entries_total / http_requests_total) < 0.99
        for: 15m
        annotations:
          summary: "< 99% of requests have audit logs"
          action: "Check audit filter configuration"
```

### **8.5 Communication Plan**

#### **Before Rollout** (1 week prior):
- Email to engineering teams: Feature flag timeline, testing guidelines
- Update #engineering-announcements Slack channel
- Schedule office hours for Q&A (1 hour session)
- Publish rollout runbook to Wiki

#### **During Rollout**:
- Real-time updates in #production-changes Slack channel
- War room open during canary phase (video call link pinned)
- Status page update if customer-facing changes expected

#### **After Rollout** (Week after 100%):
- Retrospective meeting: What went well, what didn't
- Update runbooks based on lessons learned
- Celebrate success with team (if all waves complete)
- Publish case study: "How We Achieved 100% BeanParam Compliance"

### **8.6 Success Metrics**

**Immediate Success Indicators** (Per wave):
- ✅ Zero production incidents during rollout
- ✅ Rollback not required
- ✅ All health checks green for 48 hours
- ✅ No performance degradation

**Long-Term Success Indicators** (Post-completion):
- ✅ 100% BeanParam compliance maintained for 3 months
- ✅ Zero manual UUID parsing violations
- ✅ Audit log completeness > 99% sustained
- ✅ Validation latency within SLA (P95 < 1ms)
- ✅ Developer satisfaction > 80% (survey)
- ✅ Zero critical security issues in validation layer
- Maintain a live checklist in project tracker referencing this plan; update after each merge.
- Architecture Review Board reviews status bi-weekly; escalate blockers immediately.
- On completion, update ADR-010 “Implementation Status” section and publish a lessons-learned note in `REST_VALIDATION_IMPLEMENTATION_SUMMARY.md`.

**Outcome**: All REST endpoints (except documented exemptions) follow the BeanParam validation standard, delivering SAP-grade governance, auditability, and resiliency mandated by ADR-010.
## 9. Reporting & Close-Out

### **9.1 Progress Tracking**

**Weekly Status Report Template**:
```markdown
# BeanParam Remediation - Week X Status
**Overall Status**: 🟢 On Track / 🟡 At Risk / 🔴 Blocked

## Progress This Week
- Completed: [List completed tasks]
- In Progress: [List active tasks]  
- Planned Next Week: [List upcoming tasks]

## Metrics
- Compliance Rate: X% (target: 100% by Q2 2026)
- Violations Resolved: X of 28 total
- Test Coverage: X% (target: > 85%)
```

### **9.2 Architecture Review Board (ARB) Reporting**

- Maintain live checklist in project tracker; update after each merge
- ARB reviews status bi-weekly; escalate blockers immediately
- On completion, update ADR-010 and publish lessons-learned

### **9.3 Final Deliverables Checklist**

**Technical**:
- [ ] All 28 violations resolved (100% compliance)
- [ ] ArchUnit tests passing on all modules
- [ ] Integration test coverage ≥ 85%
- [ ] Performance regression < 5%
- [ ] OpenAPI specs updated
- [ ] Localization complete (6 languages)

**Documentation**:
- [ ] ADR-010 updated with "Complete" status
- [ ] Developer guide published
- [ ] Troubleshooting guide published
- [ ] Video tutorial recorded
- [ ] Runbooks updated

**Governance**:
- [ ] Compliance dashboard live
- [ ] PR checklist integrated
- [ ] ArchUnit rules in CI
- [ ] ARB process documented
- [ ] Quarterly reviews scheduled

### **9.4 Success Criteria**

**Project "COMPLETE" when**:
1. ✅ 100% compliance (28/28 resolved)
2. ✅ Zero regressions for 2 weeks
3. ✅ All wave exit criteria met
4. ✅ ARB formal acceptance
5. ✅ Maintenance handoff complete
6. ✅ Retrospective documented

---

**Final Outcome**: SAP-grade validation framework with automated compliance, comprehensive observability, and clear ownership.

**Status**: 🚀 **READY TO EXECUTE**  
**Target**: January 25, 2026  
**Probability**: HIGH
