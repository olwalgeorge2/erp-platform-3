# Phase 3: Custom Validators & Advanced Validation

## Overview
Phase 3 extends the REST validation framework with custom Bean Validation constraints, cross-field validation rules, and business logic validation at the API boundary.

## Goals
1. **Custom Validators**: Domain-specific validation constraints (account codes, currency amounts, tenant slugs)
2. **Cross-Field Validation**: Multi-field business rules (date ranges, conditional requirements)
3. **Business Rule Validation**: Complex domain logic at REST layer (balance checks, status transitions)
4. **OpenAPI Schema Generation**: Auto-generate API documentation from validation annotations
5. **Test Coverage**: Comprehensive validation test suite

## Implementation Plan

### 1. Custom Bean Validation Constraints

#### Finance Custom Validators

**`@ValidAccountCode`**
```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AccountCodeValidator::class])
annotation class ValidAccountCode(
    val message: String = "Invalid account code format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class AccountCodeValidator : ConstraintValidator<ValidAccountCode, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        
        // Account code format: 4-6 digits, optional dash and sub-account
        val pattern = Regex("^\\d{4,6}(-\\d{2,4})?$")
        return pattern.matches(value)
    }
}
```

**`@ValidAmount`**
```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AmountValidator::class])
annotation class ValidAmount(
    val message: String = "Invalid amount",
    val minScale: Int = 2,
    val maxScale: Int = 4,
    val allowNegative: Boolean = true,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class AmountValidator : ConstraintValidator<ValidAmount, BigDecimal?> {
    private lateinit var annotation: ValidAmount
    
    override fun initialize(annotation: ValidAmount) {
        this.annotation = annotation
    }
    
    override fun isValid(value: BigDecimal?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        
        // Check scale (decimal places)
        if (value.scale() < annotation.minScale || value.scale() > annotation.maxScale) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(
                "Amount must have between ${annotation.minScale} and ${annotation.maxScale} decimal places"
            ).addConstraintViolation()
            return false
        }
        
        // Check sign
        if (!annotation.allowNegative && value < BigDecimal.ZERO) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("Amount cannot be negative")
                .addConstraintViolation()
            return false
        }
        
        return true
    }
}
```

**`@ValidCurrencyCode`**
```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [CurrencyCodeValidator::class])
annotation class ValidCurrencyCode(
    val message: String = "Invalid currency code",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class CurrencyCodeValidator : ConstraintValidator<ValidCurrencyCode, String?> {
    private val validCurrencies = setOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "INR", "MXN"
    )
    
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return validCurrencies.contains(value.uppercase())
    }
}
```

#### Identity Custom Validators

**`@ValidTenantSlug`**
```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TenantSlugValidator::class])
annotation class ValidTenantSlug(
    val message: String = "Invalid tenant slug",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class TenantSlugValidator : ConstraintValidator<ValidTenantSlug, String?> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        
        // Slug format: lowercase alphanumeric with hyphens, 3-50 chars
        // Must start/end with alphanumeric, no consecutive hyphens
        val pattern = Regex("^[a-z0-9]([a-z0-9-]{1,48}[a-z0-9])?\$")
        
        if (!pattern.matches(value)) {
            return false
        }
        
        // Check for consecutive hyphens
        if (value.contains("--")) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("Slug cannot contain consecutive hyphens")
                .addConstraintViolation()
            return false
        }
        
        return true
    }
}
```

**`@ValidUsername`**
```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UsernameValidator::class])
annotation class ValidUsername(
    val message: String = "Invalid username",
    val minLength: Int = 3,
    val maxLength: Int = 50,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class UsernameValidator : ConstraintValidator<ValidUsername, String?> {
    private lateinit var annotation: ValidUsername
    
    override fun initialize(annotation: ValidUsername) {
        this.annotation = annotation
    }
    
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        
        if (value.length < annotation.minLength || value.length > annotation.maxLength) {
            return false
        }
        
        // Username: alphanumeric, dots, underscores, hyphens
        // Must start with alphanumeric
        val pattern = Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]*$")
        return pattern.matches(value)
    }
}
```

### 2. Cross-Field Validation

**`@ValidDateRange`** (Class-Level)
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DateRangeValidator::class])
annotation class ValidDateRange(
    val startField: String,
    val endField: String,
    val message: String = "End date must be after start date",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class DateRangeValidator : ConstraintValidator<ValidDateRange, Any> {
    private lateinit var startField: String
    private lateinit var endField: String
    
    override fun initialize(annotation: ValidDateRange) {
        this.startField = annotation.startField
        this.endField = annotation.endField
    }
    
    override fun isValid(obj: Any?, context: ConstraintValidatorContext): Boolean {
        if (obj == null) return true
        
        val startDate = getFieldValue(obj, startField) as? LocalDate
        val endDate = getFieldValue(obj, endField) as? LocalDate
        
        if (startDate == null || endDate == null) return true
        
        if (!endDate.isAfter(startDate)) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(context.defaultConstraintMessageTemplate)
                .addPropertyNode(endField)
                .addConstraintViolation()
            return false
        }
        
        return true
    }
    
    private fun getFieldValue(obj: Any, fieldName: String): Any? {
        return obj::class.members
            .firstOrNull { it.name == fieldName }
            ?.call(obj)
    }
}
```

**Usage in DTOs**:
```kotlin
@ValidDateRange(startField = "startDate", endField = "endDate")
data class ReportPeriodRequest(
    @field:NotNull
    val startDate: LocalDate?,
    
    @field:NotNull
    val endDate: LocalDate?
)
```

**`@ConditionalRequired`** (Field-Level)
```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ConditionalRequiredValidator::class])
annotation class ConditionalRequired(
    val field: String,
    val fieldValue: String,
    val message: String = "Field is required when {field} is {fieldValue}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
```

### 3. Business Rule Validation

**Service-Layer Validators** (for complex domain logic)

```kotlin
@ApplicationScoped
class JournalEntryBusinessValidator @Inject constructor(
    private val accountRepository: AccountRepository,
    private val currencyService: CurrencyService
) {
    fun validateJournalEntry(request: PostJournalEntryRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Business Rule 1: Debits must equal credits
        val debitTotal = request.lines.filter { it.type == "DEBIT" }
            .sumOf { it.amount }
        val creditTotal = request.lines.filter { it.type == "CREDIT" }
            .sumOf { it.amount }
        
        if (debitTotal != creditTotal) {
            errors.add(ValidationError(
                field = "lines",
                code = "UNBALANCED_ENTRY",
                message = "Debits ($debitTotal) must equal credits ($creditTotal)"
            ))
        }
        
        // Business Rule 2: All accounts must exist and be active
        val accountCodes = request.lines.map { it.accountCode }.distinct()
        val invalidAccounts = accountCodes.filterNot { 
            accountRepository.existsByCodeAndActive(it, true) 
        }
        
        if (invalidAccounts.isNotEmpty()) {
            errors.add(ValidationError(
                field = "lines",
                code = "INVALID_ACCOUNTS",
                message = "Accounts do not exist or are inactive: ${invalidAccounts.joinToString()}"
            ))
        }
        
        // Business Rule 3: Foreign currency lines require exchange rate
        request.lines.forEach { line ->
            if (line.currency != request.baseCurrency && line.exchangeRate == null) {
                errors.add(ValidationError(
                    field = "lines[].exchangeRate",
                    code = "MISSING_EXCHANGE_RATE",
                    message = "Exchange rate required for foreign currency ${line.currency}"
                ))
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<ValidationError>) : ValidationResult()
}

data class ValidationError(
    val field: String,
    val code: String,
    val message: String
)
```

### 4. OpenAPI Schema Generation

**Configure Quarkus OpenAPI**:
```yaml
# application.yml
quarkus:
  smallrye-openapi:
    info-title: ERP Platform API
    info-version: 1.0.0
    info-description: Enterprise Resource Planning Platform
    auto-add-security: true
    schema-available: true
    enable: true
```

**Annotated DTOs**:
```kotlin
@Schema(description = "Request to post a journal entry")
data class PostJournalEntryRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 100)
    @Schema(description = "Journal entry description", example = "Monthly rent payment")
    val description: String?,
    
    @field:NotNull
    @field:ValidCurrencyCode
    @Schema(description = "Base currency code", example = "USD")
    val baseCurrency: String?,
    
    @field:NotNull
    @field:Size(min = 2)
    @Schema(description = "Journal entry lines (debits and credits)")
    val lines: List<JournalLineRequest>?
) {
    @Schema(description = "Individual journal entry line")
    data class JournalLineRequest(
        @field:NotBlank
        @field:ValidAccountCode
        @Schema(description = "Account code", example = "1000-01")
        val accountCode: String?,
        
        @field:NotNull
        @field:ValidAmount(allowNegative = false)
        @Schema(description = "Line amount", example = "1500.00")
        val amount: BigDecimal?,
        
        @field:NotBlank
        @field:Pattern(regexp = "^(DEBIT|CREDIT)$")
        @Schema(description = "Line type", allowableValues = ["DEBIT", "CREDIT"])
        val type: String?
    )
}
```

### 5. Testing Strategy

**Validator Unit Tests**:
```kotlin
@QuarkusTest
class AccountCodeValidatorTest {
    private lateinit var validator: Validator
    
    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }
    
    @Test
    fun `valid account code passes validation`() {
        val request = TestRequest(accountCode = "1000-01")
        val violations = validator.validate(request)
        assertTrue(violations.isEmpty())
    }
    
    @Test
    fun `invalid account code format fails validation`() {
        val request = TestRequest(accountCode = "ABC-123")
        val violations = validator.validate(request)
        assertEquals(1, violations.size)
        assertEquals("Invalid account code format", violations.first().message)
    }
    
    data class TestRequest(
        @field:ValidAccountCode
        val accountCode: String
    )
}
```

**Cross-Field Validation Tests**:
```kotlin
@Test
fun `end date before start date fails validation`() {
    val request = ReportPeriodRequest(
        startDate = LocalDate.of(2025, 12, 31),
        endDate = LocalDate.of(2025, 1, 1)
    )
    val violations = validator.validate(request)
    assertEquals(1, violations.size)
    assertTrue(violations.first().message.contains("End date must be after start date"))
}
```

**Business Rule Tests**:
```kotlin
@Test
fun `unbalanced journal entry fails validation`() {
    val request = PostJournalEntryRequest(
        description = "Test entry",
        baseCurrency = "USD",
        lines = listOf(
            JournalLineRequest("1000", BigDecimal("100.00"), "DEBIT"),
            JournalLineRequest("2000", BigDecimal("50.00"), "CREDIT") // Unbalanced!
        )
    )
    
    val result = validator.validateJournalEntry(request)
    assertTrue(result is ValidationResult.Failure)
    val failure = result as ValidationResult.Failure
    assertTrue(failure.errors.any { it.code == "UNBALANCED_ENTRY" })
}
```

## Implementation Order

1. **Week 1**: Custom validators for Finance (account codes, amounts, currencies)
2. **Week 2**: Custom validators for Identity (slugs, usernames, emails)
3. **Week 3**: Cross-field validation (date ranges, conditional requirements)
4. **Week 4**: Business rule validators (journal balance, account status, permissions)
5. **Week 5**: OpenAPI schema generation and documentation
6. **Week 6**: Comprehensive test suite and integration testing

## File Structure

```
bounded-contexts/
  financial-management/
    financial-shared/src/main/kotlin/
      com/erp/financial/shared/validation/
        constraints/
          ValidAccountCode.kt
          ValidAmount.kt
          ValidCurrencyCode.kt
          ValidDateRange.kt
        validators/
          AccountCodeValidator.kt
          AmountValidator.kt
          CurrencyCodeValidator.kt
          DateRangeValidator.kt
        business/
          JournalEntryBusinessValidator.kt
          AccountBalanceValidator.kt
  
  tenancy-identity/
    identity-infrastructure/src/main/kotlin/
      com/erp/identity/infrastructure/validation/
        constraints/
          ValidTenantSlug.kt
          ValidUsername.kt
          ConditionalRequired.kt
        validators/
          TenantSlugValidator.kt
          UsernameValidator.kt
          ConditionalRequiredValidator.kt
        business/
          TenantProvisioningValidator.kt
          RoleAssignmentValidator.kt
```

## Benefits

1. **Type Safety**: Compile-time validation annotation checks
2. **Reusability**: Validators can be used across multiple DTOs
3. **Documentation**: OpenAPI schemas auto-generated from annotations
4. **Testability**: Isolated validator unit tests
5. **Business Logic**: Domain rules enforced at API boundary
6. **Consistency**: Standardized validation approach across contexts

## Migration Path

1. Start with high-traffic endpoints (journal posting, user creation)
2. Gradually add validators to existing DTOs
3. Monitor validation metrics for increased coverage
4. Update API documentation with new constraints
5. Train team on custom validator creation

## Success Metrics

- **Validator Coverage**: >80% of DTOs use custom validators
- **Business Rule Coverage**: >90% of domain rules validated at API layer
- **OpenAPI Completeness**: 100% of endpoints documented with validation schemas
- **Test Coverage**: >95% for validation logic
- **Validation Error Rate**: <0.5% of requests fail custom validation

## Next Steps

After Phase 3 completion:
- **Phase 4**: Integration with API Gateway for early validation
- **Phase 5**: GraphQL validation support
- **Phase 6**: Async validation for expensive checks (database lookups)
- **Phase 7**: Validation rule versioning for API evolution
