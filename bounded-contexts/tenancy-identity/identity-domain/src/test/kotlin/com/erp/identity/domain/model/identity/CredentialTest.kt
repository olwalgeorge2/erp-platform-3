package com.erp.identity.domain.model.identity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CredentialTest {
    @Test
    fun `withNewPassword refreshes hash salt and timestamp`() {
        val original =
            Credential(
                passwordHash = "hash",
                salt = "salt",
                lastChangedAt = Instant.now().minusSeconds(60),
            )

        val updated = original.withNewPassword("hash2", "salt2")

        assertEquals("hash2", updated.passwordHash)
        assertEquals("salt2", updated.salt)
        assertFalse(updated.requiresChange())
        assertTrue(!updated.lastChangedAt.isBefore(original.lastChangedAt))
    }

    @Test
    fun `requireChangeOnNextLogin marks credential`() {
        val credential =
            Credential(
                passwordHash = "hash",
                salt = "salt",
            )

        val marked = credential.requireChangeOnNextLogin()

        assertTrue(marked.requiresChange())
        assertEquals(credential.passwordHash, marked.passwordHash)
    }

    @Test
    fun `isExpired reflects expiration timestamp`() {
        val expired =
            Credential(
                passwordHash = "hash",
                salt = "salt",
                expiresAt = Instant.now().minusSeconds(10),
            )
        val active =
            Credential(
                passwordHash = "hash",
                salt = "salt",
                expiresAt = Instant.now().plusSeconds(3600),
            )

        assertTrue(expired.isExpired())
        assertFalse(active.isExpired())
    }
}
