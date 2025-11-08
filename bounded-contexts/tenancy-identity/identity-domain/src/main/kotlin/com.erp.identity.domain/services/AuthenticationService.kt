package com.erp.identity.domain.services

import com.erp.identity.domain.exceptions.InvalidCredentialException
import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.PasswordPolicy
import com.erp.identity.domain.model.identity.User
import com.erp.shared.types.results.DomainError
import com.erp.shared.types.results.Result

class AuthenticationService(
    private val credentialVerifier: CredentialVerifier,
    private val passwordPolicy: PasswordPolicy = PasswordPolicy(),
) {
    fun authenticate(
        user: User,
        rawPassword: String,
    ): AuthenticationResult {
        val startNano = System.nanoTime()
        if (!user.canLogin()) {
            val result =
                AuthenticationResult.Failure(
                    user = user,
                    reason =
                        domainError(
                            code = "USER_NOT_ALLOWED",
                            message = "User cannot login in current state",
                            details =
                                mapOf(
                                    "status" to user.status.name,
                                    "userId" to user.id.toString(),
                                ),
                        ),
                )
            ensureMinimumDuration(startNano)
            return result
        }

        if (!credentialVerifier.verify(rawPassword, user.credential)) {
            val updatedUser = user.recordFailedLogin()
            if (updatedUser.isLocked()) {
                val result =
                    AuthenticationResult.Failure(
                        user = updatedUser,
                        reason =
                            domainError(
                                code = "ACCOUNT_LOCKED",
                                message = "Account is locked due to repeated failures",
                                details =
                                    mapOf(
                                        "userId" to updatedUser.id.toString(),
                                        "failedAttempts" to updatedUser.failedLoginAttempts.toString(),
                                    ),
                            ),
                    )
                ensureMinimumDuration(startNano)
                return result
            }

            val result =
                AuthenticationResult.Failure(
                    user = updatedUser,
                    reason =
                        domainError(
                            code = "INVALID_CREDENTIALS",
                            message = "Credentials are invalid",
                            details =
                                mapOf(
                                    "userId" to updatedUser.id.toString(),
                                    "failedAttempts" to updatedUser.failedLoginAttempts.toString(),
                                ),
                        ),
                )
            ensureMinimumDuration(startNano)
            return result
        }

        val authenticatedUser = user.recordSuccessfulLogin()
        val success = AuthenticationResult.Success(authenticatedUser)
        ensureMinimumDuration(startNano)
        return success
    }

    fun updatePassword(
        user: User,
        newRawPassword: String,
        hashedPassword: String,
        salt: String,
    ): Result<User> {
        val errors = passwordPolicy.validate(newRawPassword)
        if (errors.isNotEmpty()) {
            return Result.failure(
                code = "PASSWORD_POLICY_VIOLATION",
                message = "Password does not meet policy requirements",
                validationErrors = errors,
            )
        }

        return Result.success(user.changePassword(hashedPassword, salt))
    }

    fun requireValidCredentials(
        user: User,
        rawPassword: String,
    ) {
        val result = authenticate(user, rawPassword)
        if (result is AuthenticationResult.Failure) {
            throw InvalidCredentialException(result.reason.message)
        }
    }

    private fun ensureMinimumDuration(
        startNano: Long,
        minMillis: Long = 100,
    ) {
        val elapsedMs =
            java.time.Duration
                .ofNanos(System.nanoTime() - startNano)
                .toMillis()
        if (elapsedMs < minMillis) {
            try {
                Thread.sleep(minMillis - elapsedMs)
            } catch (_: InterruptedException) {
                // ignore
            }
        }
    }

    private fun domainError(
        code: String,
        message: String,
        details: Map<String, String> = emptyMap(),
    ): DomainError =
        DomainError(
            code = code,
            message = message,
            details = details,
        )
}

fun interface CredentialVerifier {
    fun verify(
        rawPassword: String,
        storedCredential: Credential,
    ): Boolean
}

sealed class AuthenticationResult {
    data class Success(
        val user: User,
    ) : AuthenticationResult()

    data class Failure(
        val user: User,
        val reason: DomainError,
    ) : AuthenticationResult()
}
