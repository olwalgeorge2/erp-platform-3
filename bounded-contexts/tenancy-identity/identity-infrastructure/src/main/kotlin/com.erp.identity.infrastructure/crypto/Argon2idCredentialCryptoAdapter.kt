package com.erp.identity.infrastructure.crypto

import com.erp.identity.application.port.output.CredentialCryptoPort
import com.erp.identity.application.port.output.HashedCredential
import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import de.mkammerer.argon2.Argon2Factory
import jakarta.enterprise.context.ApplicationScoped
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@ApplicationScoped
class Argon2idCredentialCryptoAdapter : CredentialCryptoPort {
    private val secureRandom = SecureRandom()

    override fun hashPassword(
        tenantId: TenantId,
        userId: UserId?,
        rawPassword: String,
        algorithm: HashAlgorithm,
    ): HashedCredential =
        when (algorithm) {
            HashAlgorithm.ARGON2, HashAlgorithm.BCRYPT, HashAlgorithm.SCRYPT -> hashWithArgon2(rawPassword)
            HashAlgorithm.PBKDF2 -> hashWithPbkdf2(rawPassword)
        }

    override fun verify(
        rawPassword: String,
        storedCredential: Credential,
    ): Boolean =
        when (storedCredential.algorithm) {
            HashAlgorithm.ARGON2 -> verifyArgon2(rawPassword, storedCredential.passwordHash)
            HashAlgorithm.PBKDF2 -> verifyPbkdf2(rawPassword, storedCredential)
            else -> false
        }

    private fun hashWithArgon2(rawPassword: String): HashedCredential {
        val passwordChars = rawPassword.toCharArray()
        val argon2 = createArgon2()
        val encodedHash =
            run {
                val hash = argon2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY_KB, ARGON2_PARALLELISM, passwordChars)
                argon2.wipeArray(passwordChars)
                hash
            }

        val saltSegment = extractSalt(encodedHash)
        require(saltSegment.isNotBlank()) { "Argon2 hash did not contain a salt segment" }

        return HashedCredential(
            hash = encodedHash,
            salt = saltSegment,
            algorithm = HashAlgorithm.ARGON2,
        )
    }

    private fun hashWithPbkdf2(rawPassword: String): HashedCredential {
        val salt = ByteArray(PBKDF2_SALT_BYTES).also(secureRandom::nextBytes)
        val hash = pbkdf2(rawPassword, salt)
        return HashedCredential(
            hash = Base64.getEncoder().encodeToString(hash),
            salt = Base64.getEncoder().encodeToString(salt),
            algorithm = HashAlgorithm.PBKDF2,
        )
    }

    private fun verifyArgon2(
        rawPassword: String,
        encodedHash: String,
    ): Boolean {
        val passwordChars = rawPassword.toCharArray()
        val argon2 = createArgon2()
        val result = runCatching { argon2.verify(encodedHash, passwordChars) }.getOrDefault(false)
        argon2.wipeArray(passwordChars)
        return result
    }

    private fun verifyPbkdf2(
        rawPassword: String,
        storedCredential: Credential,
    ): Boolean {
        val salt = Base64.getDecoder().decode(storedCredential.salt)
        val expected = Base64.getDecoder().decode(storedCredential.passwordHash)
        val actual = pbkdf2(rawPassword, salt)
        return constantTimeEquals(expected, actual)
    }

    private fun pbkdf2(
        rawPassword: String,
        salt: ByteArray,
    ): ByteArray {
        val keySpec: KeySpec = PBEKeySpec(rawPassword.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(keySpec).encoded
    }

    private fun constantTimeEquals(
        expected: ByteArray,
        actual: ByteArray,
    ): Boolean {
        if (expected.size != actual.size) {
            return false
        }
        var diff = 0
        for (index in expected.indices) {
            diff = diff or (expected[index].toInt() xor actual[index].toInt())
        }
        return diff == 0
    }

    private fun extractSalt(encodedHash: String): String {
        val segments = encodedHash.split('$')
        return if (segments.size >= 5) {
            segments[4]
        } else {
            ""
        }
    }

    private fun createArgon2() =
        Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            ARGON2_SALT_LENGTH,
            ARGON2_HASH_LENGTH,
        )

    companion object {
        private const val ARGON2_ITERATIONS = 3
        private const val ARGON2_MEMORY_KB = 19456 // ~19 MB
        private const val ARGON2_PARALLELISM = 1
        private const val ARGON2_SALT_LENGTH = 16
        private const val ARGON2_HASH_LENGTH = 32

        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_LENGTH = 256
        private const val PBKDF2_SALT_BYTES = 32
    }
}
