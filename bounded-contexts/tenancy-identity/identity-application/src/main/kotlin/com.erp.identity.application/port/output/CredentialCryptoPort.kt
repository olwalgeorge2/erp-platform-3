package com.erp.identity.application.port.output

import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.services.CredentialVerifier

interface CredentialCryptoPort : CredentialVerifier {
    fun hashPassword(
        tenantId: TenantId,
        userId: UserId?,
        rawPassword: String,
        algorithm: HashAlgorithm = HashAlgorithm.ARGON2,
    ): HashedCredential
}

data class HashedCredential(
    val hash: String,
    val salt: String,
    val algorithm: HashAlgorithm,
)
