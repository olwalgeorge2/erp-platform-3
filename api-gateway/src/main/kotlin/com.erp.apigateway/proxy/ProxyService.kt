package com.erp.apigateway.proxy

import com.erp.apigateway.metrics.GatewayMetrics
import com.erp.apigateway.routing.ServiceRoute
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@ApplicationScoped
class ProxyService {
    @Inject
    lateinit var metrics: GatewayMetrics
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
        val start = System.nanoTime()
        val base = route.target.baseUrl.trimEnd('/')
        val rawPath = if (incomingPath.startsWith("/")) incomingPath else "/$incomingPath"
        val path = route.mapUpstreamPath(rawPath)
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
        return try {
            val upstream =
                sendWithRetry(
                    client = client,
                    request = request,
                    retries = route.target.retries,
                    backoffInitialMs = route.target.backoffInitialMs,
                    backoffMaxMs = route.target.backoffMaxMs,
                    backoffJitterMs = route.target.backoffJitterMs,
                )
            val durationMs = (System.nanoTime() - start) / 1_000_000

            val responseBuilder = Response.status(upstream.statusCode())
            upstream.headers().map().forEach { (name, values) ->
                val lower = name.lowercase()
                if (!hopByHopHeaders.contains(lower)) {
                    values.forEach { v -> responseBuilder.header(name, v) }
                }
            }
            val resp = responseBuilder.entity(upstream.body()).build()
            metrics.recordRequest(method, path, upstream.statusCode(), durationMs)
            resp
        } catch (e: java.net.http.HttpTimeoutException) {
            val durationMs = (System.nanoTime() - start) / 1_000_000
            metrics.markError("proxy_timeout")
            metrics.recordRequest(method, path, 504, durationMs)
            Response.status(504).entity(byteArrayOf()).build()
        } catch (e: Exception) {
            val durationMs = (System.nanoTime() - start) / 1_000_000
            metrics.markError("proxy_exception")
            metrics.recordRequest(method, path, 502, durationMs)
            Response.status(502).entity(byteArrayOf()).build()
        }
    }

    private fun sendWithRetry(
        client: HttpClient,
        request: HttpRequest,
        retries: Int,
        backoffInitialMs: Long,
        backoffMaxMs: Long,
        backoffJitterMs: Long,
    ): HttpResponse<ByteArray> {
        var attempt = 0
        var lastException: Exception? = null
        while (attempt <= retries) {
            try {
                val resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                if (resp.statusCode() >= 500 && attempt < retries) {
                    sleepWithBackoff(attempt, backoffInitialMs, backoffMaxMs, backoffJitterMs)
                    attempt += 1
                    continue
                }
                return resp
            } catch (e: java.net.http.HttpTimeoutException) {
                lastException = e
                if (attempt >= retries) throw e
                sleepWithBackoff(attempt, backoffInitialMs, backoffMaxMs, backoffJitterMs)
                attempt += 1
            } catch (e: java.io.IOException) {
                lastException = e
                if (attempt >= retries) throw e
                sleepWithBackoff(attempt, backoffInitialMs, backoffMaxMs, backoffJitterMs)
                attempt += 1
            }
        }
        throw lastException ?: IllegalStateException("Retry logic failed without exception")
    }

    private fun sleepWithBackoff(
        attempt: Int,
        initialMs: Long,
        maxMs: Long,
        jitterMs: Long,
    ) {
        val base = initialMs * (1L shl attempt).coerceAtLeast(1)
        val capped = base.coerceAtMost(maxMs)
        val jitter =
            if (jitterMs > 0) {
                java.util.concurrent.ThreadLocalRandom
                    .current()
                    .nextLong(0, jitterMs + 1)
            } else {
                0
            }
        val delay = (capped + jitter).coerceAtMost(maxMs)
        try {
            Thread.sleep(delay)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
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
