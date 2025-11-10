package com.erp.apigateway.tracing

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import java.security.SecureRandom
import java.util.UUID

@Provider
@Priority(Priorities.HEADER_DECORATOR)
class TracingFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    @Inject
    lateinit var traceContext: TraceContext

    override fun filter(requestContext: ContainerRequestContext) {
        val traceId = requestContext.getHeaderString("X-Trace-Id") ?: UUID.randomUUID().toString()
        traceContext.traceId = traceId
        requestContext.headers.putSingle("X-Trace-Id", traceId)

        // W3C traceparent support: if missing, synthesize a valid header
        val incomingTraceparent = requestContext.getHeaderString("traceparent")
        if (incomingTraceparent == null || incomingTraceparent.isBlank()) {
            val w3c = generateTraceparent()
            requestContext.headers.putSingle("traceparent", w3c)
        }
        // passthrough tracestate if present; nothing to do otherwise
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val id = traceContext.traceId ?: requestContext.getHeaderString("X-Trace-Id")
        if (!id.isNullOrBlank()) {
            responseContext.headers.putSingle("X-Trace-Id", id)
        }
        val tp = requestContext.getHeaderString("traceparent")
        if (!tp.isNullOrBlank()) {
            responseContext.headers.putSingle("traceparent", tp)
        }
        val ts = requestContext.getHeaderString("tracestate")
        if (!ts.isNullOrBlank()) {
            responseContext.headers.putSingle("tracestate", ts)
        }
    }

    private fun generateTraceparent(): String {
        // version 00 - trace-id (16 bytes) - span-id (8 bytes) - flags 01 (sampled)
        val rnd = secureRandom.get()
        val traceId = ByteArray(16).also { rnd.nextBytes(it) }.toHex()
        val spanId = ByteArray(8).also { rnd.nextBytes(it) }.toHex()
        return "00-$traceId-$spanId-01"
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { b -> ((b.toInt() and 0xff) + 0x100).toString(16).substring(1) }

    companion object {
        private val secureRandom: ThreadLocal<SecureRandom> = ThreadLocal.withInitial { SecureRandom() }
    }
}
