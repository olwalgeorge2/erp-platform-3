package com.erp.identity.infrastructure.adapter.input.rest

import com.erp.identity.domain.model.identity.Credential
import com.erp.identity.domain.model.identity.HashAlgorithm
import com.erp.identity.domain.model.identity.User
import com.erp.identity.domain.model.identity.UserId
import com.erp.identity.domain.model.identity.UserStatus
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.infrastructure.adapter.input.rest.dto.ActivateUserRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.AuthenticateRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.CreateUserRequest
import com.erp.identity.infrastructure.adapter.input.rest.dto.UserResponse
import com.erp.identity.infrastructure.service.IdentityCommandService
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
import org.mockito.kotlin.whenever
import java.net.URI
import java.time.Instant

class AuthResourceTest {
    private val commandService: IdentityCommandService = mock()
    private val resource = AuthResource(commandService)

    @Test
    fun `authenticate returns user on success`() {
        val user = sampleUser()
        whenever(commandService.authenticate(any())).thenReturn(Result.success(user))

        val request =
            AuthenticateRequest(
                tenantId = user.tenantId.value,
                usernameOrEmail = user.username,
                password = "password123!",
            )

        val response = resource.authenticate(request)

        assertEquals(Response.Status.OK.statusCode, response.status)
        val body = response.entity as UserResponse
        assertEquals(user.id.toString(), body.id)
        assertEquals(user.username, body.username)
    }

    @Test
    fun `create user returns created response`() {
        val user = sampleUser()
        whenever(commandService.createUser(any())).thenReturn(Result.success(user))

        val request =
            CreateUserRequest(
                tenantId = user.tenantId.value,
                username = user.username,
                email = user.email,
                fullName = user.fullName,
                password = "Password123!",
            )

        val response = resource.createUser(request, simpleUriInfo())

        assertEquals(Response.Status.CREATED.statusCode, response.status)
        assertTrue(response.location.toString().endsWith("/api/auth/users/${user.id}"))
        val body = response.entity as UserResponse
        assertEquals(user.email, body.email)
    }

    @Test
    fun `activate user returns OK`() {
        val user = sampleUser().copy(status = UserStatus.PENDING)
        whenever(commandService.activateUser(any())).thenReturn(Result.success(user.copy(status = UserStatus.ACTIVE)))

        val request =
            ActivateUserRequest(
                tenantId = user.tenantId.value,
                requestedBy = "admin",
                requirePasswordReset = false,
            )

        val response = resource.activateUser(user.id.toString(), request)

        assertEquals(Response.Status.OK.statusCode, response.status)
        val body = response.entity as UserResponse
        assertEquals(user.id.toString(), body.id)
    }

    private fun sampleUser(): User {
        val tenantId = TenantId.generate()
        return User(
            id = UserId.generate(),
            tenantId = tenantId,
            username = "john-doe",
            email = "john.doe@example.com",
            fullName = "John Doe",
            credential =
                Credential(
                    passwordHash = "hash",
                    salt = "salt",
                    algorithm = HashAlgorithm.ARGON2,
                    lastChangedAt = Instant.now(),
                ),
            status = UserStatus.ACTIVE,
            roleIds = emptySet(),
            metadata = emptyMap(),
            lastLoginAt = Instant.now(),
            failedLoginAttempts = 0,
            lockedUntil = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
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

    private fun emptyMultiMap(): MultivaluedMap<String, String> =
        jakarta.ws.rs.core.MultivaluedHashMap()
}
