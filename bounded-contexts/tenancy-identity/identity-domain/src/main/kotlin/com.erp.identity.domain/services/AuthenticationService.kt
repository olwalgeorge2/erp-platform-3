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
        if (!user.canLogin()) {
            return AuthenticationResult.Failure(
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
        }

        if (!credentialVerifier.verify(rawPassword, user.credential)) {
            val updatedUser = user.recordFailedLogin()
            if (updatedUser.isLocked()) {
                return AuthenticationResult.Failure(
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
            }

            return AuthenticationResult.Failure(
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
        }

        val authenticatedUser = user.recordSuccessfulLogin()
        return AuthenticationResult.Success(authenticatedUser)
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
    data class Success(val user: User) : AuthenticationResult()

    data class Failure(
        val user: User,
        val reason: DomainError,
    ) : AuthenticationResult()
}
