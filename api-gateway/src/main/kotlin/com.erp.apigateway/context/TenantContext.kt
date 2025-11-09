package com.erp.apigateway.context

import jakarta.enterprise.context.RequestScoped

@RequestScoped
class TenantContext {
    var tenantId: String? = null
    var userId: String? = null
}
