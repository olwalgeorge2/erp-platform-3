package com.erp.apigateway

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/health")
class HealthResource {
    @GET
    @Path("/live")
    @Produces(MediaType.TEXT_PLAIN)
    fun live(): String = "OK"

    @GET
    @Path("/ready")
    @Produces(MediaType.TEXT_PLAIN)
    fun ready(): String = "READY"
}
