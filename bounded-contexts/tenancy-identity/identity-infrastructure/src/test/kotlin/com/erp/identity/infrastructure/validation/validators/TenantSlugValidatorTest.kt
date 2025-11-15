package com.erp.identity.infrastructure.validation.validators

import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock

class TenantSlugValidatorTest {
    private lateinit var validator: TenantSlugValidator
    private lateinit var context: ConstraintValidatorContext

    @BeforeEach
    fun setUp() {
        validator = TenantSlugValidator()
        context = mock(ConstraintValidatorContext::class.java)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme-corp",
            "tenant-123",
            "my-company",
            "abc",
            "test-1",
            "my-long-slug-name-with-multiple-hyphens",
            "a1b2c3",
            "slug123",
            "123-456",
        ],
    )
    fun `should accept valid tenant slugs`(slug: String) {
        assertTrue(validator.isValid(slug, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "ACME-CORP",
            "Acme-Corp",
            "Tenant-123",
            "MY-COMPANY",
            "Test-1",
        ],
    )
    fun `should reject uppercase tenant slugs`(slug: String) {
        assertFalse(validator.isValid(slug, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme corp",
            "tenant 123",
            "my company",
            "test slug",
        ],
    )
    fun `should reject slugs with spaces`(slug: String) {
        assertFalse(validator.isValid(slug, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme_corp",
            "tenant.123",
            "my@company",
            "test!slug",
            "slug#123",
        ],
    )
    fun `should reject slugs with special characters`(slug: String) {
        assertFalse(validator.isValid(slug, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "-acme",
            "-tenant-123",
            "-my-company",
        ],
    )
    fun `should reject slugs starting with hyphen`(slug: String) {
        assertFalse(validator.isValid(slug, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme-",
            "tenant-123-",
            "my-company-",
        ],
    )
    fun `should reject slugs ending with hyphen`(slug: String) {
        assertFalse(validator.isValid(slug, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme--corp",
            "tenant--123",
            "my---company",
        ],
    )
    fun `should reject slugs with consecutive hyphens`(slug: String) {
        assertFalse(validator.isValid(slug, context))
    }

    @Test
    fun `should reject too short slug`() {
        assertFalse(validator.isValid("ab", context))
    }

    @Test
    fun `should accept slug at minimum length`() {
        assertTrue(validator.isValid("abc", context))
    }

    @Test
    fun `should accept slug at maximum length`() {
        val fiftyCharSlug = "a".repeat(50)
        assertTrue(validator.isValid(fiftyCharSlug, context))
    }

    @Test
    fun `should reject slug exceeding maximum length`() {
        val fiftyOneCharSlug = "a".repeat(51)
        assertFalse(validator.isValid(fiftyOneCharSlug, context))
    }

    @Test
    fun `should reject blank slug`() {
        assertFalse(validator.isValid("   ", context))
    }

    @Test
    fun `should reject empty slug`() {
        assertFalse(validator.isValid("", context))
    }

    @Test
    fun `should accept null slug`() {
        // Null handling delegated to @NotNull
        assertTrue(validator.isValid(null, context))
    }
}
