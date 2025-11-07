package com.erp.identity.infrastructure.crypto

import com.erp.identity.application.port.output.HashedCredential
import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Argon2idCredentialCryptoAdapterTest {
    private val adapter = Argon2idCredentialCryptoAdapter()
    private val tenantId = TenantId.generate()

    @Test
    fun `hashPassword defaults to Argon2id`() {
        val hashed = adapter.hashPassword(tenantId, null, STRONG_PASSWORD)

        assertEquals(HashAlgorithm.ARGON2, hashed.algorithm)
        assertTrue(hashed.hash.startsWith("\$argon2id\$"))
        assertTrue(hashed.salt.isNotBlank())
    }

    @Test
    fun `verify succeeds for Argon2 hash`() {
        val hashed = adapter.hashPassword(tenantId, null, STRONG_PASSWORD)
        val credential = hashed.toCredential()

        assertTrue(adapter.verify(STRONG_PASSWORD, credential))
    }

    @Test
    fun `verify succeeds for PBKDF2 hash`() {
        val hashed =
            adapter.hashPassword(
                tenantId = tenantId,
                userId = UserId.generate(),
                rawPassword = STRONG_PASSWORD,
                algorithm = HashAlgorithm.PBKDF2,
            )
        val credential = hashed.toCredential()

        assertEquals(HashAlgorithm.PBKDF2, credential.algorithm)
        assertTrue(adapter.verify(STRONG_PASSWORD, credential))
        assertNotEquals(HashAlgorithm.ARGON2, credential.algorithm)
    }

    private fun HashedCredential.toCredential(): Credential =
        Credential(
            passwordHash = hash,
            salt = salt,
            algorithm = algorithm,
        )

    companion object {
        private const val STRONG_PASSWORD = "S1gnal!Password#2025"
    }
}
