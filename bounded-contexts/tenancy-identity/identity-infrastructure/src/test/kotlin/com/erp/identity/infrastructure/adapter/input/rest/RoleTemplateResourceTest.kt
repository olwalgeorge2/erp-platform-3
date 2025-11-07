package com.erp.identity.infrastructure.adapter.input.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoleTemplateResourceTest {
    private val catalog = RoleTemplateCatalog()
    private val resource = RoleTemplateResource(catalog)

    @Test
    fun `list templates returns catalog`() {
        val response = resource.listTemplates()

        assertEquals(200, response.status)
        @Suppress("UNCHECKED_CAST")
        val templates = response.entity as List<RoleTemplateResponse>
        assertEquals(3, templates.size)
        val tenantAdmin =
            templates.first { it.code == "TENANT_ADMIN" }
        assertEquals("Tenant Administrator", tenantAdmin.name)
        assertEquals(3, tenantAdmin.permissions.size)
    }
}
