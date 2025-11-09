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

        val builder = HttpRequest.newBuilder().GET().uri(targetUri)

        // Copy headers except hop-by-hop
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
