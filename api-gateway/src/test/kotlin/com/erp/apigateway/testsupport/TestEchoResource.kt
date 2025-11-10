package com.erp.apigateway.testsupport

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/test/echo")
class TestEchoResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun echo(): Response = Response.ok(mapOf("ok" to true)).build()
}
