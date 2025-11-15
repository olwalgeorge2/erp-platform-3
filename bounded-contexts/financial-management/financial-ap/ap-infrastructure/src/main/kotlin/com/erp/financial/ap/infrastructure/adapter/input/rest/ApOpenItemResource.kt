package com.erp.financial.ap.infrastructure.adapter.input.rest

import com.erp.financial.ap.application.port.input.query.ApOpenItemQueryUseCase
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.AgingRequest
import com.erp.financial.ap.infrastructure.adapter.input.rest.dto.toResponse
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@ApplicationScoped
@Path("/api/v1/finance/ap/open-items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AP Open Items")
class ApOpenItemResource {
    @Inject
    lateinit var queryUseCase: ApOpenItemQueryUseCase

    @GET
    @Path("/aging/detail")
    @Operation(summary = "Retrieve AP open-item aging detail")
    fun agingDetail(@Valid @BeanParam request: AgingRequest) =
        queryUseCase
            .getAgingDetail(request.toQuery())
            .toResponse()

    @GET
    @Path("/aging/summary")
    @Operation(summary = "Retrieve AP open-item aging summary")
    fun agingSummary(@Valid @BeanParam request: AgingRequest) =
        queryUseCase
            .getAgingSummary(request.toQuery())
            .toResponse()
}
