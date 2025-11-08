package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.domain.model.identity.Permission
import com.erp.identity.domain.model.identity.PermissionScope
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.adapter.input.rest.dto.CreateRoleRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.PermissionPayload
import com.erp.identity.infrastructure.adapter.input.rest.dto.UpdateRoleRequest
import com.erp.identity.infrastructure.service.IdentityCommandService
import com.erp.identity.infrastructure.service.IdentityQueryService
import com.erp.identity.infrastructure.service.security.AuthorizationService
import com.erp.identity.infrastructure.web.RequestPrincipal
import com.erp.identity.infrastructure.web.RequestPrincipalContext
import com.erp.shared.types.results.Result
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.PathSegment
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import jakarta.ws.rs.core.UriInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.net.URI
import java.time.Instant

class RoleResourceTest {
    private val commandService: IdentityCommandService = mock()
    private val queryService: IdentityQueryService = mock()
    private val authorizationService: AuthorizationService = AuthorizationService()
    private val resource = RoleResource(commandService, queryService, authorizationService)

    @BeforeEach
    fun resetMocks() {
        reset(commandService, queryService)
        // Default: system admin to bypass auth in tests unless overridden
        RequestPrincipalContext.set(
            RequestPrincipal(
                userId = "test-user",
                tenantId = null,
                roles = setOf("SYSTEM_ADMIN"),
                permissions = emptySet(),
            ),
        )
    }

    @Test
    fun `create role returns created response`() {
        val tenantId = TenantId.generate()
        val request =
            CreateRoleRequest(
                name = "admin",
                description = "Administrator",
                permissions = setOf(PermissionPayload("user", "manage", PermissionScope.TENANT)),
            )
        val role = sampleRole(tenantId)
        whenever(commandService.createRole(any())).thenReturn(Result.success(role))

        val response = resource.createRole(tenantId.toString(), request, simpleUriInfo())

        assertEquals(Response.Status.CREATED.statusCode, response.status)
        assertNotNull(response.location)
        verify(commandService).createRole(any())
    }

    @Test
    fun `update role returns ok`() {
        val tenantId = TenantId.generate()
        val role = sampleRole(tenantId)
        whenever(commandService.updateRole(any())).thenReturn(Result.success(role))

        val response =
            resource.updateRole(
                tenantId.toString(),
                role.id.toString(),
                UpdateRoleRequest(
                    name = "admin",
                    description = "updated",
                ),
            )

        assertEquals(Response.Status.OK.statusCode, response.status)
        verify(commandService).updateRole(any())
    }

    @Test
    fun `delete role returns no content`() {
        val tenantId = TenantId.generate()
        whenever(commandService.deleteRole(any())).thenReturn(Result.success(Unit))

        val response = resource.deleteRole(tenantId.toString(), RoleId.generate().toString())

        assertEquals(Response.Status.NO_CONTENT.statusCode, response.status)
        verify(commandService).deleteRole(any())
    }

    @Test
    fun `list roles returns data`() {
        val tenantId = TenantId.generate()
        whenever(queryService.listRoles(any())).thenReturn(Result.success(listOf(sampleRole(tenantId))))

        val response = resource.listRoles(tenantId.toString(), 10, 0)

        assertEquals(Response.Status.OK.statusCode, response.status)
        verify(queryService).listRoles(any())
    }

    @Test
    fun `get role returns bad request for invalid identifiers`() {
        val response = resource.getRole("not-a-tenant", "not-a-role")

        assertEquals(Response.Status.BAD_REQUEST.statusCode, response.status)
        verifyNoInteractions(queryService)
    }

    @Test
    fun `delete role validates tenant id separately`() {
        val tenantId = TenantId.generate()
        val response = resource.deleteRole(tenantId.toString(), "not-a-role")

        assertEquals(Response.Status.BAD_REQUEST.statusCode, response.status)
        verify(commandService, never()).deleteRole(any())
    }

    @Test
    fun `create role fails when authorization denies`() {
        val tenantId = TenantId.generate()
        // Override principal to simulate denial
        RequestPrincipalContext.set(
            RequestPrincipal(
                userId = "user",
                tenantId = tenantId.value.toString(),
                roles = emptySet(),
                permissions = emptySet(),
            ),
        )

        val response =
            resource.createRole(
                tenantId.toString(),
                CreateRoleRequest(
                    name = "reader",
                    description = "desc",
                ),
                simpleUriInfo(),
            )

        assertEquals(Response.Status.FORBIDDEN.statusCode, response.status)
        verifyNoInteractions(commandService)
    }

    private fun sampleRole(tenantId: TenantId): Role =
        Role
            .create(
                tenantId = tenantId,
                name = "admin",
                description = "Administrator",
                permissions = setOf(Permission.create("user")),
            ).copy(
                id = RoleId.generate(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

    private fun simpleUriInfo(): UriInfo =
        object : UriInfo {
            private val base = URI.create("http://localhost/")

            override fun getBaseUri(): URI = base

            override fun getBaseUriBuilder(): UriBuilder = UriBuilder.fromUri(base)

            override fun getAbsolutePath(): URI = base

            override fun getAbsolutePathBuilder(): UriBuilder = UriBuilder.fromUri(base)

            override fun getRequestUri(): URI = base

            override fun getRequestUriBuilder(): UriBuilder = UriBuilder.fromUri(base)

            override fun getPath(): String = ""

            override fun getPath(decode: Boolean): String = ""

            override fun getPathSegments(): MutableList<PathSegment> = mutableListOf()

            override fun getPathSegments(decode: Boolean): MutableList<PathSegment> = mutableListOf()

            override fun getPathParameters(): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getPathParameters(decode: Boolean): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getQueryParameters(): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getQueryParameters(decode: Boolean): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getMatchedURIs(): MutableList<String> = mutableListOf()

            override fun getMatchedURIs(decode: Boolean): MutableList<String> = mutableListOf()

            override fun getMatchedResources(): MutableList<Any> = mutableListOf()

            override fun resolve(uri: URI?): URI = uri ?: base

            override fun relativize(uri: URI?): URI = uri ?: base
        }

    private fun emptyMultiMap(): MultivaluedMap<String, String> = MultivaluedHashMap()
}
