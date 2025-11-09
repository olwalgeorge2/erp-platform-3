package com.erp.apigateway.proxy

import com.erp.apigateway.routing.RouteResolver
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class ProxyController
    @Inject
    constructor(
        private val routeResolver: RouteResolver,
        private val proxyService: ProxyService,
    ) {
        @GET
        @Path("{path: .+}")
        fun proxyGet(
            @PathParam("path") subPath: String,
            @Context uriInfo: UriInfo,
            @Context headers: HttpHeaders,
        ): Response {
            val resolved = routeResolver.resolve("/" + subPath)
            return proxyService.forwardGet(
                route = resolved,
                incomingPath = "/" + subPath,
                queryParams = uriInfo.queryParameters,
                incomingHeaders = headers.requestHeaders,
            )
        }
    }
