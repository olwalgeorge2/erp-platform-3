package com.erp.identity.infrastructure.crypto

import com.erp.identity.application.port.output.CredentialCryptoPort
import com.erp.identity.application.port.output.HashedCredential
import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.enterprise.context.ApplicationScoped
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@ApplicationScoped
class Pbkdf2CredentialCryptoAdapter : CredentialCryptoPort {
    private val secureRandom = SecureRandom()

    override fun hashPassword(
        tenantId: TenantId,
        userId: UserId?,
        rawPassword: String,
        algorithm: HashAlgorithm,
    ): HashedCredential {
        val selectedAlgorithm =
            when (algorithm) {
                HashAlgorithm.PBKDF2, HashAlgorithm.BCRYPT -> HashAlgorithm.PBKDF2
                HashAlgorithm.ARGON2, HashAlgorithm.SCRYPT -> HashAlgorithm.PBKDF2
            }

        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val hash = pbkdf2(rawPassword, salt)
        return HashedCredential(
            hash = Base64.getEncoder().encodeToString(hash),
            salt = Base64.getEncoder().encodeToString(salt),
            algorithm = selectedAlgorithm,
        )
    }

    override fun verify(
        rawPassword: String,
        storedCredential: Credential,
    ): Boolean =
        when (storedCredential.algorithm) {
            HashAlgorithm.PBKDF2 -> {
                val salt = Base64.getDecoder().decode(storedCredential.salt)
                val expected = Base64.getDecoder().decode(storedCredential.passwordHash)
                val actual = pbkdf2(rawPassword, salt)
                expected.contentEquals(actual)
            }
            else -> false
        }

    private fun pbkdf2(
        rawPassword: String,
        salt: ByteArray,
    ): ByteArray {
        val keySpec: KeySpec = PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(keySpec).encoded
    }

    companion object {
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
        private const val SALT_BYTES = 32
    }
}
