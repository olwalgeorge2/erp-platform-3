package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.infrastructure.adapter.input.rest.dto.PermissionPayload
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

open class BaseRoleTemplateResource() {
    @Inject
    protected lateinit var catalog: RoleTemplateCatalog

    constructor(catalog: RoleTemplateCatalog) : this() {
        this.catalog = catalog
    }

    @GET
    fun listTemplates(): Response = Response.ok(catalog.templates()).build()
}

@Path("$IDENTITY_API_V1_PREFIX/roles/templates")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
open class RoleTemplateResource() : BaseRoleTemplateResource() {
    constructor(catalog: RoleTemplateCatalog) : this() {
        this.catalog = catalog
    }
}

@Path("$IDENTITY_API_COMPAT_PREFIX/roles/templates")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Deprecated("Use /api/v1/identity/roles/templates")
open class LegacyRoleTemplateResource() : BaseRoleTemplateResource() {
    constructor(catalog: RoleTemplateCatalog) : this() {
        this.catalog = catalog
    }
}

@ApplicationScoped
class RoleTemplateCatalog {
    fun templates(): List<RoleTemplateResponse> = TEMPLATES

    companion object {
        private val TEMPLATES =
            listOf(
                RoleTemplateResponse(
                    code = "TENANT_ADMIN",
                    name = "Tenant Administrator",
                    description = "Full control over tenant lifecycle, users, and roles.",
                    permissions =
                        setOf(
                            PermissionPayload("tenants", "read"),
                            PermissionPayload("roles", "manage"),
                            PermissionPayload("users", "manage"),
                        ),
                ),
                RoleTemplateResponse(
                    code = "SUPPORT_AGENT",
                    name = "Support Agent",
                    description = "Read/write access to user profiles, read-only tenant metadata.",
                    permissions =
                        setOf(
                            PermissionPayload("users", "read"),
                            PermissionPayload("users", "update"),
                            PermissionPayload("tenants", "read"),
                        ),
                ),
                RoleTemplateResponse(
                    code = "BILLING_MANAGER",
                    name = "Billing Manager",
                    description = "Read access to tenant info plus manage billing profiles.",
                    permissions =
                        setOf(
                            PermissionPayload("tenants", "read"),
                            PermissionPayload("billing-profiles", "manage"),
                        ),
                ),
            )
    }
}

data class RoleTemplateResponse(
    val code: String,
    val name: String,
    val description: String,
    val permissions: Set<PermissionPayload>,
)
