package com.erp.apigateway.proxy

import com.erp.apigateway.routing.RouteResolver
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class ProxyController
    @Inject
    constructor(
        private val routeResolver: RouteResolver,
    ) {
        @GET
        @Path("{path: .+}")
        fun proxyGet(
            @PathParam("path") subPath: String,
        ): Response {
            val resolved = routeResolver.resolve("/" + subPath)
            val body =
                mapOf(
                    "status" to "stub",
                    "targetBaseUrl" to resolved.target.baseUrl,
                    "matchedPattern" to resolved.pattern,
                    "requestedPath" to "/" + subPath,
                )
            return Response.ok(body).build()
        }
    }
