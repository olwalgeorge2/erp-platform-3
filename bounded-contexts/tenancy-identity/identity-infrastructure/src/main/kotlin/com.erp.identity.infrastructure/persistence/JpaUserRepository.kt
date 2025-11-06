package com.erp.identity.infrastructure.persistence

import com.erp.identity.application.port.output.UserRepository
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.persistence.entity.UserEntity
import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Companion.failure
import com.erp.shared.types.results.Result.Companion.success
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jboss.logging.Logger
import org.hibernate.exception.ConstraintViolationException
import java.util.UUID

@ApplicationScoped
@Transactional(TxType.MANDATORY)
class JpaUserRepository(
    private val entityManager: EntityManager,
) : UserRepository {
    override fun findById(
        tenantId: TenantId,
        userId: UserId,
    ): Result<User?> =
        runQuery("findById") {
            entityManager
                .find(UserEntity::class.java, userId.value)
                ?.takeIf { it.tenantId == tenantId.value }
                ?.toDomain()
        }

    override fun findByUsername(
        tenantId: TenantId,
        username: String,
    ): Result<User?> =
        runQuery("findByUsername") {
            entityManager
                .createQuery(
                    """
                    SELECT u FROM UserEntity u
                    WHERE u.tenantId = :tenantId AND LOWER(u.username) = LOWER(:username)
                    """.trimIndent(),
                    UserEntity::class.java,
                ).setParameter("tenantId", tenantId.value)
                .setParameter("username", username)
                .resultList
                .firstOrNull()
                ?.toDomain()
        }

    override fun findByEmail(
        tenantId: TenantId,
        email: String,
    ): Result<User?> =
        runQuery("findByEmail") {
            entityManager
                .createQuery(
                    """
                    SELECT u FROM UserEntity u
                    WHERE u.tenantId = :tenantId AND LOWER(u.email) = LOWER(:email)
                    """.trimIndent(),
                    UserEntity::class.java,
                ).setParameter("tenantId", tenantId.value)
                .setParameter("email", email)
                .resultList
                .firstOrNull()
                ?.toDomain()
        }

    override fun existsByUsername(
        tenantId: TenantId,
        username: String,
    ): Result<Boolean> =
        existsBy(tenantId.value, "username", username, "existsByUsername")

    override fun existsByEmail(
        tenantId: TenantId,
        email: String,
    ): Result<Boolean> =
        existsBy(tenantId.value, "email", email, "existsByEmail")

    override fun save(user: User): Result<User> =
        runCommand("save") {
            val existing = entityManager.find(UserEntity::class.java, user.id.value)
            val managed =
                if (existing != null) {
                    existing.updateFrom(user)
                    entityManager.merge(existing)
                } else {
                    entityManager.merge(UserEntity.from(user))
                }
            entityManager.flush()
            managed.toDomain()
        }.recoverDuplicateConstraint(user)

    private fun runCommand(
        operation: String,
        block: () -> User,
    ): Result<User> =
        try {
            success(block())
        } catch (ex: PersistenceException) {
            mapPersistenceError(operation, ex)
        }

    private fun Result<User>.recoverDuplicateConstraint(user: User): Result<User> =
        when (this) {
            is Result.Success -> this
            is Result.Failure ->
                when (error.code) {
                    "USER_USERNAME_CONSTRAINT" ->
                        failure(
                            code = "USERNAME_IN_USE",
                            message = "Username already exists",
                            details =
                                mapOf(
                                    "tenantId" to user.tenantId.toString(),
                                    "username" to user.username,
                                ),
                            cause = error.cause,
                        )
                    "USER_EMAIL_CONSTRAINT" ->
                        failure(
                            code = "EMAIL_IN_USE",
                            message = "Email already exists",
                            details =
                                mapOf(
                                    "tenantId" to user.tenantId.toString(),
                                    "email" to user.email,
                                ),
                            cause = error.cause,
                        )
                    else -> this
                }
        }

    private fun existsBy(
        tenantId: UUID,
        field: String,
        value: String,
        operation: String,
    ): Result<Boolean> =
        runQuery(operation) {
            entityManager
                .createQuery(
                    """
                    SELECT COUNT(u) FROM UserEntity u
                    WHERE u.tenantId = :tenantId AND LOWER(u.$field) = LOWER(:value)
                    """.trimIndent(),
                    java.lang.Long::class.java,
                ).setParameter("tenantId", tenantId)
                .setParameter("value", value)
                .singleResult > 0
        }

    private fun <T> runQuery(
        operation: String,
        block: () -> T,
    ): Result<T> =
        try {
            success(block())
        } catch (ex: PersistenceException) {
            mapPersistenceError(operation, ex)
        }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(JpaUserRepository::class.java)
    }

    private fun mapPersistenceError(
        operation: String,
        ex: PersistenceException,
    ): Result.Failure {
        val root = ex.rootCause()
        val constraint =
            (root as? ConstraintViolationException)?.constraintName?.lowercase()
                ?: root.message?.lowercase().orEmpty()

        if (constraint.contains("uk_identity_users_username")) {
            return failure(
                code = "USER_USERNAME_CONSTRAINT",
                message = "Username constraint violation",
                details = mapOf("operation" to operation),
                cause = ex,
            )
        }
        if (constraint.contains("uk_identity_users_email")) {
            return failure(
                code = "USER_EMAIL_CONSTRAINT",
                message = "Email constraint violation",
                details = mapOf("operation" to operation),
                cause = ex,
            )
        }

        LOGGER.errorf(ex, "UserRepository.%s failed", operation)
        return failure(
            code = "USER_REPOSITORY_ERROR",
            message = "User repository operation failed",
            details = mapOf("operation" to operation),
            cause = ex,
        )
    }

    private fun Throwable.rootCause(): Throwable =
        generateSequence(this) { it.cause }.last()
}
