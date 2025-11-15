package com.erp.financial.shared.validation.validators

import com.erp.financial.shared.validation.constraints.ValidDateRange
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class DateRangeValidatorTest {
    private lateinit var validator: DateRangeValidator
    private lateinit var context: ConstraintValidatorContext

    @BeforeEach
    fun setUp() {
        validator = DateRangeValidator()
        context = mock(ConstraintValidatorContext::class.java)
    }

    // LocalDate tests
    @ValidDateRange(startField = "startDate", endField = "endDate")
    data class LocalDateRange(
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    @Test
    fun `should accept valid LocalDate range`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should accept LocalDate range where start equals end`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2024, 6, 15),
                endDate = LocalDate.of(2024, 6, 15),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should reject LocalDate range where start is after end`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2024, 12, 31),
                endDate = LocalDate.of(2024, 1, 1),
            )

        assertFalse(validator.isValid(range, context))
    }

    @Test
    fun `should reject LocalDate range one day apart (end before start)`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2024, 6, 16),
                endDate = LocalDate.of(2024, 6, 15),
            )

        assertFalse(validator.isValid(range, context))
    }

    // Instant tests
    @ValidDateRange(startField = "startDate", endField = "endDate")
    data class InstantRange(
        val startDate: Instant,
        val endDate: Instant,
    )

    @Test
    fun `should accept valid Instant range`() {
        val annotation = InstantRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val now = Instant.now()
        val range =
            InstantRange(
                startDate = now,
                endDate = now.plusSeconds(3600),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should accept Instant range where start equals end`() {
        val annotation = InstantRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val now = Instant.now()
        val range =
            InstantRange(
                startDate = now,
                endDate = now,
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should reject Instant range where start is after end`() {
        val annotation = InstantRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val now = Instant.now()
        val range =
            InstantRange(
                startDate = now.plusSeconds(3600),
                endDate = now,
            )

        assertFalse(validator.isValid(range, context))
    }

    // LocalDateTime tests
    @ValidDateRange(startField = "startDate", endField = "endDate")
    data class LocalDateTimeRange(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime,
    )

    @Test
    fun `should accept valid LocalDateTime range`() {
        val annotation = LocalDateTimeRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateTimeRange(
                startDate = LocalDateTime.of(2024, 1, 1, 0, 0),
                endDate = LocalDateTime.of(2024, 12, 31, 23, 59),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should reject LocalDateTime range where start is after end`() {
        val annotation = LocalDateTimeRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateTimeRange(
                startDate = LocalDateTime.of(2024, 12, 31, 23, 59),
                endDate = LocalDateTime.of(2024, 1, 1, 0, 0),
            )

        assertFalse(validator.isValid(range, context))
    }

    // Nullable endDate tests
    @ValidDateRange(startField = "startDate", endField = "endDate")
    data class NullableEndDateRange(
        val startDate: LocalDate,
        val endDate: LocalDate?,
    )

    @Test
    fun `should accept null endDate (open-ended range)`() {
        val annotation = NullableEndDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            NullableEndDateRange(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = null,
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should accept valid range with non-null endDate`() {
        val annotation = NullableEndDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            NullableEndDateRange(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should reject invalid range with non-null endDate`() {
        val annotation = NullableEndDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            NullableEndDateRange(
                startDate = LocalDate.of(2024, 12, 31),
                endDate = LocalDate.of(2024, 1, 1),
            )

        assertFalse(validator.isValid(range, context))
    }

    // Null object test
    @Test
    fun `should accept null object`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        assertTrue(validator.isValid(null, context))
    }

    // Edge cases
    @Test
    fun `should accept leap year date range`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2024, 2, 28),
                endDate = LocalDate.of(2024, 2, 29),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should accept year boundary range`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2023, 12, 31),
                endDate = LocalDate.of(2024, 1, 1),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should accept multi-year range`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2020, 1, 1),
                endDate = LocalDate.of(2030, 12, 31),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should handle minimum LocalDate`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.MIN,
                endDate = LocalDate.of(2024, 1, 1),
            )

        assertTrue(validator.isValid(range, context))
    }

    @Test
    fun `should handle maximum LocalDate`() {
        val annotation = LocalDateRange::class.annotations.first { it is ValidDateRange } as ValidDateRange
        validator.initialize(annotation)

        val range =
            LocalDateRange(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.MAX,
            )

        assertTrue(validator.isValid(range, context))
    }
}
