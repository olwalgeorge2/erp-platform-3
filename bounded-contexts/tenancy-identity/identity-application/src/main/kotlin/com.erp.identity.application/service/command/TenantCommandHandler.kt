package com.erp.identity.application.service.command

import com.erp.identity.application.port.input.command.ProvisionTenantCommand
import com.erp.identity.application.port.output.EventPublisherPort
import com.erp.identity.application.port.output.TenantRepository
import com.erp.identity.domain.model.tenant.Tenant
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
}
