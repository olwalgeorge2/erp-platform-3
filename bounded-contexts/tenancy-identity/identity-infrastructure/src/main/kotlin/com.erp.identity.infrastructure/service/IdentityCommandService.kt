package com.erp.identity.infrastructure.service

import com.erp.identity.application.port.input.command.ActivateUserCommand
import com.erp.identity.application.port.input.command.ActivateTenantCommand
import com.erp.identity.application.port.input.command.AssignRoleCommand
import com.erp.identity.application.port.input.command.AuthenticateUserCommand
import com.erp.identity.application.port.input.command.CreateUserCommand
import com.erp.identity.application.port.input.command.ProvisionTenantCommand
import com.erp.identity.application.port.input.command.ResumeTenantCommand
import com.erp.identity.application.port.input.command.SuspendTenantCommand
import com.erp.identity.application.port.input.command.UpdateCredentialsCommand
import com.erp.identity.application.service.command.TenantCommandHandler
import com.erp.identity.application.service.command.UserCommandHandler
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.shared.types.results.Result
import com.erp.shared.types.results.Result.Failure
import com.erp.shared.types.results.Result.Success
import io.micrometer.core.annotation.Counted
import io.micrometer.core.annotation.Timed
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import jakarta.validation.Valid
import java.time.Duration
import java.util.UUID
import org.slf4j.MDC

@ApplicationScoped
class IdentityCommandService(
    private val userCommandHandler: UserCommandHandler,
    private val tenantCommandHandler: TenantCommandHandler,
) {
    @Counted(
        value = "identity.user.creation.attempts",
        description = "Total number of user creation attempts",
    )
    @Timed(
        value = "identity.user.creation.duration",
        description = "Duration of user creation command execution",
        percentiles = [0.5, 0.95, 0.99],
    )
    @Transactional(TxType.REQUIRED)
    fun createUser(@Valid command: CreateUserCommand): Result<User> {
        val traceId = ensureTraceId()
        ensureTenantMdc(command.tenantId.toString())
        Log.infof(
            "[%s] createUser - tenant=%s, username=%s, email=%s",
            traceId,
            command.tenantId,
            command.username,
            command.email,
        )
        val start = System.nanoTime()
        val result = userCommandHandler.createUser(command)
        logResult(
            traceId = traceId,
            operation = "createUser",
            startNano = start,
            result = result,
            successContext = { user ->
                "tenant=${user.tenantId}, userId=${user.id}, status=${user.status}"
            },
            failureContext = {
                "tenant=${command.tenantId}, username=${command.username}"
            },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun assignRole(@Valid command: AssignRoleCommand): Result<User> {
        val traceId = ensureTraceId()
        ensureTenantMdc(command.tenantId.toString())
        Log.infof(
            "[%s] assignRole - tenant=%s, userId=%s, roleId=%s",
            traceId,
            command.tenantId,
            command.userId,
            command.roleId,
        )
        val start = System.nanoTime()
        val result = userCommandHandler.assignRole(command)
        logResult(
            traceId = traceId,
            operation = "assignRole",
            startNano = start,
            result = result,
            successContext = { user ->
                "tenant=${user.tenantId}, userId=${user.id}, roles=${user.roleIds.size}"
            },
            failureContext = {
                "tenant=${command.tenantId}, userId=${command.userId}, roleId=${command.roleId}"
            },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun updateCredentials(@Valid command: UpdateCredentialsCommand): Result<User> {
        val traceId = ensureTraceId()
        ensureTenantMdc(command.tenantId.toString())
        Log.infof(
            "[%s] updateCredentials - tenant=%s, userId=%s",
            traceId,
            command.tenantId,
            command.userId,
        )
        val start = System.nanoTime()
        val result = userCommandHandler.updateCredentials(command)
        logResult(
            traceId = traceId,
            operation = "updateCredentials",
            startNano = start,
            result = result,
            successContext = { user ->
                "tenant=${user.tenantId}, userId=${user.id}"
            },
            failureContext = {
                "tenant=${command.tenantId}, userId=${command.userId}"
            },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun activateUser(@Valid command: ActivateUserCommand): Result<User> {
        val traceId = ensureTraceId()
        ensureTenantMdc(command.tenantId.toString())
        Log.infof(
            "[%s] activateUser - tenant=%s, userId=%s",
            traceId,
            command.tenantId,
            command.userId,
        )
        val start = System.nanoTime()
        val result = userCommandHandler.activateUser(command)
        logResult(
            traceId = traceId,
            operation = "activateUser",
            startNano = start,
            result = result,
            successContext = { user ->
                "tenant=${user.tenantId}, userId=${user.id}, status=${user.status}"
            },
            failureContext = {
                "tenant=${command.tenantId}, userId=${command.userId}"
            },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun authenticate(@Valid command: AuthenticateUserCommand): Result<User> {
        val traceId = ensureTraceId()
        ensureTenantMdc(command.tenantId.toString())
        Log.infof(
            "[%s] authenticate - tenant=%s, identifier=%s",
            traceId,
            command.tenantId,
            command.usernameOrEmail,
        )
        val start = System.nanoTime()
        val result = userCommandHandler.authenticate(command)
        logResult(
            traceId = traceId,
            operation = "authenticate",
            startNano = start,
            result = result,
            successContext = { user ->
                "tenant=${user.tenantId}, userId=${user.id}, status=${user.status}"
            },
            failureContext = {
                "tenant=${command.tenantId}, identifier=${command.usernameOrEmail}"
            },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun provisionTenant(@Valid command: ProvisionTenantCommand): Result<Tenant> {
        val traceId = ensureTraceId()
        Log.infof(
            "[%s] provisionTenant - slug=%s, name=%s",
            traceId,
            command.slug,
            command.name,
        )
        val start = System.nanoTime()
        val result = tenantCommandHandler.provisionTenant(command)
        logResult(
            traceId = traceId,
            operation = "provisionTenant",
            startNano = start,
            result = result,
            successContext = { tenant ->
                "tenant=${tenant.id}, slug=${tenant.slug}, status=${tenant.status}"
            },
            failureContext = {
                "slug=${command.slug}, name=${command.name}"
            },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun activateTenant(@Valid command: ActivateTenantCommand): Result<Tenant> {
        val traceId = ensureTraceId()
        Log.infof("[%s] activateTenant - tenant=%s", traceId, command.tenantId)
        val start = System.nanoTime()
        val result = tenantCommandHandler.activateTenant(command)
        logResult(
            traceId = traceId,
            operation = "activateTenant",
            startNano = start,
            result = result,
            successContext = { tenant -> "tenant=${tenant.id}, status=${tenant.status}" },
            failureContext = { "tenant=${command.tenantId}" },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun suspendTenant(@Valid command: SuspendTenantCommand): Result<Tenant> {
        val traceId = ensureTraceId()
        Log.infof("[%s] suspendTenant - tenant=%s", traceId, command.tenantId)
        val start = System.nanoTime()
        val result = tenantCommandHandler.suspendTenant(command)
        logResult(
            traceId = traceId,
            operation = "suspendTenant",
            startNano = start,
            result = result,
            successContext = { tenant -> "tenant=${tenant.id}, status=${tenant.status}" },
            failureContext = { "tenant=${command.tenantId}" },
        )
        return result
    }

    @Transactional(TxType.REQUIRED)
    fun resumeTenant(@Valid command: ResumeTenantCommand): Result<Tenant> {
        val traceId = ensureTraceId()
        Log.infof("[%s] resumeTenant - tenant=%s", traceId, command.tenantId)
        val start = System.nanoTime()
        val result = tenantCommandHandler.resumeTenant(command)
        logResult(
            traceId = traceId,
            operation = "resumeTenant",
            startNano = start,
            result = result,
            successContext = { tenant -> "tenant=${tenant.id}, status=${tenant.status}" },
            failureContext = { "tenant=${command.tenantId}" },
        )
        return result
    }

    private fun ensureTraceId(): String {
        val existing = MDC.get("traceId")?.toString()
        return existing ?: UUID.randomUUID().toString().also { MDC.put("traceId", it) }
    }

    private fun ensureTenantMdc(tenantId: String?) {
        if (!tenantId.isNullOrBlank() && MDC.get("tenantId") == null) {
            MDC.put("tenantId", tenantId)
        }
    }

    private fun <T> logResult(
        traceId: String,
        operation: String,
        startNano: Long,
        result: Result<T>,
        successContext: (T) -> String,
        failureContext: (Failure) -> String,
    ) {
        val durationMs = Duration.ofNanos(System.nanoTime() - startNano).toMillis()
        when (result) {
            is Success ->
                Log.infof(
                    "[%s] %s succeeded in %dms - %s",
                    traceId,
                    operation,
                    durationMs,
                    successContext(result.value),
                )
            is Failure ->
                Log.warnf(
                    "[%s] %s failed in %dms - code=%s message=%s context=%s",
                    traceId,
                    operation,
                    durationMs,
                    result.error.code,
                    result.error.message,
                    failureContext(result),
                )
        }
    }
}
