package com.erp.apigateway.admin

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

object AdminKeys {
    @Volatile
    lateinit var privateKey: PrivateKey
}

class AdminJwtTestResource : QuarkusTestResourceLifecycleManager {
    override fun start(): Map<String, String> {
        val kp: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
        AdminKeys.privateKey = kp.private
        val pub = kp.public as RSAPublicKey

        val pem =
            buildString {
                appendLine("-----BEGIN PUBLIC KEY-----")
                val b64 = Base64.getEncoder().encodeToString(pub.encoded)
                b64.chunked(64).forEach { appendLine(it) }
                appendLine("-----END PUBLIC KEY-----")
            }
        val tmp = File.createTempFile("jwt-pub", ".pem")
        tmp.writeText(pem)

        return mapOf(
            "quarkus.smallrye-jwt.enabled" to "true",
            "mp.jwt.verify.publickey.location" to "file:${tmp.absolutePath}",
            "mp.jwt.verify.issuer" to "erp-platform-dev",
            "gateway.auth.protected-prefixes" to "/admin/",
        )
    }

    override fun stop() {}
}
