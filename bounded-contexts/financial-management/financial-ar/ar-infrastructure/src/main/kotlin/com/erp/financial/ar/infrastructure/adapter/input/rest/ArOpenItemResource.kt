package com.erp.financial.ar.infrastructure.adapter.input.rest

import com.erp.financial.ar.application.port.input.query.ArOpenItemQueryUseCase
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.ArAgingDetailResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.ArAgingRequest
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.ArAgingSummaryResponse
import com.erp.financial.ar.infrastructure.adapter.input.rest.dto.toResponse
import com.erp.financial.shared.validation.preferredLocale
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.Locale

@ApplicationScoped
@Path("/api/v1/finance/ar/open-items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AR Open Items")
class ArOpenItemResource {
    @Inject
    lateinit var useCase: ArOpenItemQueryUseCase

    @Context
    lateinit var httpHeaders: HttpHeaders

    @GET
    @Path("/aging/detail")
    @Operation(summary = "Retrieve AR open-item aging detail")
    fun detail(@Valid @BeanParam request: ArAgingRequest): ArAgingDetailResponse =
        useCase
            .getAgingDetail(request.toQuery(currentLocale()))
            .toResponse()

    @GET
    @Path("/aging/summary")
    @Operation(summary = "Retrieve AR open-item aging summary")
    fun summary(@Valid @BeanParam request: ArAgingRequest): ArAgingSummaryResponse =
        useCase
            .getAgingSummary(request.toQuery(currentLocale()))
            .toResponse()

    private fun currentLocale(): Locale = httpHeaders.preferredLocale()
}
