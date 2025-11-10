package com.erp.apigateway.tracing

import jakarta.enterprise.context.RequestScoped

@RequestScoped
class TraceContext {
    var traceId: String? = null
}
