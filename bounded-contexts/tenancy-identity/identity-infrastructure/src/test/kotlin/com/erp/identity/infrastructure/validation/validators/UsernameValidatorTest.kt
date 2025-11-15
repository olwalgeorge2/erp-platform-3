package com.erp.identity.infrastructure.validation.validators

import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock

class UsernameValidatorTest {
    private lateinit var validator: UsernameValidator
    private lateinit var context: ConstraintValidatorContext

    @BeforeEach
    fun setUp() {
        validator = UsernameValidator()
        context = mock(ConstraintValidatorContext::class.java)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "john.doe",
            "user_123",
            "john.doe123",
            "JohnDoe",
            "user123",
            "abc",
            "john_doe_123",
            "user.name.here",
            "User_Name",
            "test1",
        ],
    )
    fun `should accept valid usernames`(username: String) {
        assertTrue(validator.isValid(username, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "john..doe",
            "user__123",
            "name...test",
            "test___user",
        ],
    )
    fun `should reject usernames with consecutive dots or underscores`(username: String) {
        assertFalse(validator.isValid(username, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "john._doe",
            "user_.123",
            "name._test",
            "test_.user",
        ],
    )
    fun `should reject usernames with consecutive special characters`(username: String) {
        assertFalse(validator.isValid(username, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            ".john",
            "_user",
            ".test",
            "_name",
        ],
    )
    fun `should reject usernames starting with dot or underscore`(username: String) {
        assertFalse(validator.isValid(username, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "john.",
            "user_",
            "test.",
            "name_",
        ],
    )
    fun `should reject usernames ending with dot or underscore`(username: String) {
        assertFalse(validator.isValid(username, context))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "john-doe",
            "user@123",
            "john doe",
            "user#test",
            "name!user",
        ],
    )
    fun `should reject usernames with invalid special characters`(username: String) {
        assertFalse(validator.isValid(username, context))
    }

    @Test
    fun `should reject too short username`() {
        assertFalse(validator.isValid("ab", context))
    }

    @Test
    fun `should accept username at minimum length`() {
        assertTrue(validator.isValid("abc", context))
    }

    @Test
    fun `should accept username at maximum length`() {
        val fiftyCharUsername = "a".repeat(50)
        assertTrue(validator.isValid(fiftyCharUsername, context))
    }

    @Test
    fun `should reject username exceeding maximum length`() {
        val fiftyOneCharUsername = "a".repeat(51)
        assertFalse(validator.isValid(fiftyOneCharUsername, context))
    }

    @Test
    fun `should reject blank username`() {
        assertFalse(validator.isValid("   ", context))
    }

    @Test
    fun `should reject empty username`() {
        assertFalse(validator.isValid("", context))
    }

    @Test
    fun `should accept null username`() {
        // Null handling delegated to @NotNull
        assertTrue(validator.isValid(null, context))
    }

    @Test
    fun `should accept mixed case usernames`() {
        assertTrue(validator.isValid("JohnDoe", context))
        assertTrue(validator.isValid("UserName123", context))
        assertTrue(validator.isValid("Test.User", context))
    }

    @Test
    fun `should accept usernames with numbers`() {
        assertTrue(validator.isValid("user123", context))
        assertTrue(validator.isValid("123user", context))
        assertTrue(validator.isValid("user1test2", context))
    }
}
