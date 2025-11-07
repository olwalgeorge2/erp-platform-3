package com.erp.identity.application.service.command

import com.erp.identity.application.port.input.command.ActivateTenantCommand
import com.erp.identity.application.port.input.command.ProvisionTenantCommand
import com.erp.identity.application.port.input.command.ResumeTenantCommand
import com.erp.identity.application.port.input.command.SuspendTenantCommand
import com.erp.identity.application.port.output.EventPublisherPort
import com.erp.identity.application.port.output.TenantRepository
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.services.TenantProvisioningService
import com.erp.identity.domain.services.TenantSlugUniquenessChecker
import com.erp.shared.types.results.Result

class TenantCommandHandler(
    private val tenantRepository: TenantRepository,
    private val eventPublisher: EventPublisherPort,
) {
    fun provisionTenant(command: ProvisionTenantCommand): Result<Tenant> {
        val provisioningService =
            TenantProvisioningService(
                slugUniquenessChecker =
                    TenantSlugUniquenessChecker { slug ->
                        tenantRepository
                            .existsBySlug(slug)
                            .map { exists -> !exists }
                    },
            )

        return provisioningService
            .provisionTenant(
                name = command.name,
                slug = command.slug,
                subscription = command.subscription,
                organization = command.organization,
                metadata = command.metadata,
                requestedBy = command.requestedBy,
            ).flatMap { result ->
                tenantRepository
                    .save(result.tenant)
                    .onSuccess { saved -> eventPublisher.publish(result.event) }
            }
    }

    fun activateTenant(command: ActivateTenantCommand): Result<Tenant> =
        updateTenant(command.tenantId) { it.activate() }

    fun suspendTenant(command: SuspendTenantCommand): Result<Tenant> =
        updateTenant(command.tenantId) { it.suspend(command.reason) }

    fun resumeTenant(command: ResumeTenantCommand): Result<Tenant> =
        updateTenant(command.tenantId) { it.reactivate() }

    private fun updateTenant(
        tenantId: TenantId,
        operator: (Tenant) -> Tenant,
    ): Result<Tenant> =
        when (val current = tenantRepository.findById(tenantId)) {
            is Result.Failure -> current
            is Result.Success -> {
                val tenant = current.value
                if (tenant == null) {
                    Result.failure(
                        code = "TENANT_NOT_FOUND",
                        message = "Tenant not found",
                        details = mapOf("tenantId" to tenantId.toString()),
                    )
                } else {
                    runCatching { operator(tenant) }
                        .fold(
                            onSuccess = { updated -> tenantRepository.save(updated) },
                            onFailure = {
                                Result.failure(
                                    code = "TENANT_STATE_INVALID",
                                    message = it.message ?: "Invalid tenant state transition",
                                    details = mapOf("tenantId" to tenantId.toString()),
                                    cause = it,
                                )
                            },
                        )
                }
            }
        }
}
