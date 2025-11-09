package com.erp.apigateway.proxy

import com.erp.apigateway.routing.ServiceRoute
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@ApplicationScoped
class ProxyService {
    private val hopByHopHeaders =
        setOf(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length",
        )

    fun forwardGet(
        route: ServiceRoute,
        incomingPath: String,
        queryParams: MultivaluedMap<String, String>,
        incomingHeaders: MultivaluedMap<String, String>,
    ): Response =
        forward(
            route = route,
            method = "GET",
            incomingPath = incomingPath,
            queryParams = queryParams,
            incomingHeaders = incomingHeaders,
            body = null,
        )

    fun forwardDelete(
        route: ServiceRoute,
        incomingPath: String,
        queryParams: MultivaluedMap<String, String>,
        incomingHeaders: MultivaluedMap<String, String>,
    ): Response =
        forward(
            route = route,
            method = "DELETE",
            incomingPath = incomingPath,
            queryParams = queryParams,
            incomingHeaders = incomingHeaders,
            body = null,
        )

    fun forwardWithBody(
        route: ServiceRoute,
        method: String,
        incomingPath: String,
        queryParams: MultivaluedMap<String, String>,
        incomingHeaders: MultivaluedMap<String, String>,
        body: ByteArray,
    ): Response =
        forward(
            route = route,
            method = method,
            incomingPath = incomingPath,
            queryParams = queryParams,
            incomingHeaders = incomingHeaders,
            body = body,
        )

    private fun forward(
        route: ServiceRoute,
        method: String,
        incomingPath: String,
        queryParams: MultivaluedMap<String, String>,
        incomingHeaders: MultivaluedMap<String, String>,
        body: ByteArray?,
    ): Response {
        val base = route.target.baseUrl.trimEnd('/')
        val path = if (incomingPath.startsWith("/")) incomingPath else "/$incomingPath"
        val query = buildQueryString(queryParams)
        val targetUri = URI.create("$base$path$query")

        val client =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(route.target.timeoutSeconds.toLong()))
                .build()

        val builder = HttpRequest.newBuilder().uri(targetUri)

        // Apply method + body
        when (method.uppercase()) {
            "GET" -> builder.GET()
            "DELETE" -> builder.DELETE()
            "POST", "PUT", "PATCH" -> {
                val publisher = HttpRequest.BodyPublishers.ofByteArray(body ?: ByteArray(0))
                // PATCH not directly on builder prior to JDK 21; use method()
                builder.method(method.uppercase(), publisher)
            }
            else -> builder.method(method.uppercase(), HttpRequest.BodyPublishers.noBody())
        }

        // Copy headers except hop-by-hop; content-length is recomputed by client
        incomingHeaders.forEach { (name, values) ->
            val lower = name.lowercase()
            if (!hopByHopHeaders.contains(lower)) {
                values.forEach { v -> builder.header(name, v) }
            }
        }

        val request = builder.build()
        val upstream = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

        val responseBuilder = Response.status(upstream.statusCode())
        upstream.headers().map().forEach { (name, values) ->
            val lower = name.lowercase()
            if (!hopByHopHeaders.contains(lower)) {
                values.forEach { v -> responseBuilder.header(name, v) }
            }
        }
        return responseBuilder.entity(upstream.body()).build()
    }

    private fun buildQueryString(params: MultivaluedMap<String, String>): String {
        if (params.isEmpty()) return ""
        val encoded =
            params.entries.joinToString("&") { (k, vs) ->
                vs.joinToString("&") { v ->
                    "${encode(k)}=${encode(v)}"
                }
            }
        return if (encoded.isEmpty()) "" else "?$encoded"
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8)
}
