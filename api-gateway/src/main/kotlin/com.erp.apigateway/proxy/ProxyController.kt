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

        @jakarta.ws.rs.POST
        @Path("{path: .+}")
        fun proxyPost(
            @PathParam("path") subPath: String,
            @Context uriInfo: UriInfo,
            @Context headers: HttpHeaders,
            requestBody: ByteArray,
        ): Response {
            val resolved = routeResolver.resolve("/" + subPath)
            return proxyService.forwardWithBody(
                route = resolved,
                method = "POST",
                incomingPath = "/" + subPath,
                queryParams = uriInfo.queryParameters,
                incomingHeaders = headers.requestHeaders,
                body = requestBody,
            )
        }

        @jakarta.ws.rs.PUT
        @Path("{path: .+}")
        fun proxyPut(
            @PathParam("path") subPath: String,
            @Context uriInfo: UriInfo,
            @Context headers: HttpHeaders,
            requestBody: ByteArray,
        ): Response {
            val resolved = routeResolver.resolve("/" + subPath)
            return proxyService.forwardWithBody(
                route = resolved,
                method = "PUT",
                incomingPath = "/" + subPath,
                queryParams = uriInfo.queryParameters,
                incomingHeaders = headers.requestHeaders,
                body = requestBody,
            )
        }

        @jakarta.ws.rs.PATCH
        @Path("{path: .+}")
        fun proxyPatch(
            @PathParam("path") subPath: String,
            @Context uriInfo: UriInfo,
            @Context headers: HttpHeaders,
            requestBody: ByteArray,
        ): Response {
            val resolved = routeResolver.resolve("/" + subPath)
            return proxyService.forwardWithBody(
                route = resolved,
                method = "PATCH",
                incomingPath = "/" + subPath,
                queryParams = uriInfo.queryParameters,
                incomingHeaders = headers.requestHeaders,
                body = requestBody,
            )
        }

        @jakarta.ws.rs.DELETE
        @Path("{path: .+}")
        fun proxyDelete(
            @PathParam("path") subPath: String,
            @Context uriInfo: UriInfo,
            @Context headers: HttpHeaders,
        ): Response {
            val resolved = routeResolver.resolve("/" + subPath)
            return proxyService.forwardDelete(
                route = resolved,
                incomingPath = "/" + subPath,
                queryParams = uriInfo.queryParameters,
                incomingHeaders = headers.requestHeaders,
            )
        }
    }
