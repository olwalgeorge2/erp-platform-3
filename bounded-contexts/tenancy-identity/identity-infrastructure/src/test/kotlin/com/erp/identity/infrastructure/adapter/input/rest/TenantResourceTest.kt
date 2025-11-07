package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.domain.model.tenant.Subscription
import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantStatus
import com.erp.identity.infrastructure.adapter.input.rest.dto.ActivateTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.ProvisionTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.ResumeTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.SuspendTenantRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.SubscriptionPayload
import com.erp.identity.infrastructure.adapter.input.rest.dto.TenantResponse
import com.erp.identity.infrastructure.service.IdentityCommandService
import com.erp.identity.infrastructure.service.IdentityQueryService
import com.erp.shared.types.results.Result
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.PathSegment
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import jakarta.ws.rs.core.UriInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI
import java.time.Instant

class TenantResourceTest {
    private val commandService: IdentityCommandService = mock()
    private val queryService: IdentityQueryService = mock()
    private val resource = TenantResource(commandService, queryService)

    @Test
    fun `provision tenant returns created response`() {
        val request =
            ProvisionTenantRequest(
                name = "Acme Inc",
                slug = "acme-inc",
                subscription =
                    SubscriptionPayload(
                        plan = SubscriptionPlan.STARTER,
                        startDate = Instant.parse("2024-01-01T00:00:00Z"),
                        endDate = null,
                        maxUsers = 25,
                        maxStorage = 5_000,
                        features = setOf("rbac"),
                    ),
                organization = null,
                metadata = mapOf("region" to "us-east-1"),
            )
        val tenant = sampleTenant()

        whenever(commandService.provisionTenant(any())).thenReturn(Result.success(tenant))

        val response = resource.provisionTenant(request, simpleUriInfo())

        assertEquals(Response.Status.CREATED.statusCode, response.status)
        assertTrue(response.location.toString().endsWith("/api/tenants/${tenant.id}"))
        val body = response.entity as TenantResponse
        assertEquals(tenant.slug, body.slug)
        assertEquals(tenant.status, body.status)
        verify(commandService).provisionTenant(any())
    }

    @Test
    fun `provision tenant conflict when slug exists`() {
        val request =
            ProvisionTenantRequest(
                name = "Acme Inc",
                slug = "acme-inc",
                subscription =
                    SubscriptionPayload(
                        plan = SubscriptionPlan.STARTER,
                        startDate = Instant.parse("2024-01-01T00:00:00Z"),
                        endDate = null,
                        maxUsers = 25,
                        maxStorage = 5_000,
                        features = emptySet(),
                    ),
                organization = null,
            )

        whenever(commandService.provisionTenant(any())).thenReturn(
            Result.failure(
                code = "TENANT_SLUG_EXISTS",
                message = "Tenant slug already exists",
                details = mapOf("slug" to request.slug),
            ),
        )

        val response = resource.provisionTenant(request, simpleUriInfo())

        assertEquals(Response.Status.CONFLICT.statusCode, response.status)
        val error = response.entity as ErrorResponse
        assertEquals("TENANT_SLUG_EXISTS", error.code)
        verify(commandService).provisionTenant(any())
    }

    @Test
    fun `activate tenant returns ok`() {
        val tenant = sampleTenant()
        whenever(commandService.activateTenant(any())).thenReturn(Result.success(tenant))

        val response = resource.activateTenant(tenant.id.toString(), ActivateTenantRequest())

        assertEquals(Response.Status.OK.statusCode, response.status)
        verify(commandService).activateTenant(any())
    }

    @Test
    fun `suspend tenant returns ok`() {
        val suspended = sampleTenant().copy(status = TenantStatus.SUSPENDED)
        whenever(commandService.suspendTenant(any())).thenReturn(Result.success(suspended))

        val response =
            resource.suspendTenant(
                suspended.id.toString(),
                SuspendTenantRequest(reason = "Non-payment"),
            )

        assertEquals(Response.Status.OK.statusCode, response.status)
        verify(commandService).suspendTenant(any())
    }

    @Test
    fun `resume tenant returns ok`() {
        val tenant = sampleTenant()
        whenever(commandService.resumeTenant(any())).thenReturn(Result.success(tenant))

        val response =
            resource.resumeTenant(
                tenant.id.toString(),
                ResumeTenantRequest(),
            )

        assertEquals(Response.Status.OK.statusCode, response.status)
        verify(commandService).resumeTenant(any())
    }

    private fun sampleTenant(): Tenant {
        val subscription =
            Subscription(
                plan = SubscriptionPlan.STARTER,
                startDate = Instant.parse("2024-01-01T00:00:00Z"),
                endDate = null,
                maxUsers = 25,
                maxStorage = 5_000,
                features = setOf("rbac"),
            )
        return Tenant.provision(
            name = "Acme Inc",
            slug = "acme-inc",
            subscription = subscription,
            organization = null,
        ).copy(
            status = TenantStatus.ACTIVE,
        )
    }

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

            override fun getPathSegments(decode: Boolean): MutableList<PathSegment> = mutableListOf()

            override fun getPathParameters(): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getPathParameters(decode: Boolean): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getQueryParameters(): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getQueryParameters(decode: Boolean): MultivaluedMap<String, String> = emptyMultiMap()

            override fun getMatchedURIs(): MutableList<String> = mutableListOf()

            override fun getMatchedURIs(decode: Boolean): MutableList<String> = mutableListOf()

            override fun getMatchedResources(): MutableList<Any> = mutableListOf()
            override fun getPathSegments(): MutableList<PathSegment> = mutableListOf()
            override fun resolve(uri: URI?): URI = uri ?: base
            override fun relativize(uri: URI?): URI = uri ?: base
        }

    private fun emptyMultiMap(): MultivaluedMap<String, String> =
        jakarta.ws.rs.core.MultivaluedHashMap()
}
