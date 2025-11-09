package com.erp.apigateway.security

import io.smallrye.jwt.auth.principal.JWTParser
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.jwt.JsonWebToken

@ApplicationScoped
class JwtValidator {
    @Inject
    lateinit var jwtParser: JWTParser

    fun parse(token: String): JsonWebToken = jwtParser.parse(token)
}
