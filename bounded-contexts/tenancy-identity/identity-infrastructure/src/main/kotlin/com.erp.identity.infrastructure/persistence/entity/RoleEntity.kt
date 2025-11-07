package com.erp.identity.infrastructure.persistence.entity

import com.erp.identity.domain.model.identity.Permission
import com.erp.identity.domain.model.identity.PermissionScope
import com.erp.identity.domain.model.identity.Role
import com.erp.identity.domain.model.identity.RoleId
import com.erp.identity.domain.model.tenant.TenantId
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_roles",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_roles_name", columnNames = ["tenant_id", "name"]),
    ],
)
class RoleEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID = UUID.randomUUID(),
    @Column(name = "name", nullable = false, length = 100)
    var name: String = "",
    @Column(name = "description", nullable = false, length = 500)
    var description: String = "",
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "identity_role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
    )
    var permissions: MutableSet<PermissionEmbeddable> = mutableSetOf(),
    @Column(name = "is_system", nullable = false)
    var isSystem: Boolean = false,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "identity_role_metadata",
        joinColumns = [JoinColumn(name = "role_id")],
    )
    @MapKeyColumn(name = "metadata_key", length = 100)
    @Column(name = "metadata_value", length = 500)
    var metadata: MutableMap<String, String> = mutableMapOf(),
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): Role =
        Role(
            id = RoleId(id),
            tenantId = TenantId(tenantId),
            name = name,
            description = description,
            permissions = permissions.map { it.toDomain() }.toSet(),
            isSystem = isSystem,
            metadata = metadata.toMap(),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(domain: Role): RoleEntity =
            RoleEntity(
                id = domain.id.value,
                tenantId = domain.tenantId.value,
                name = domain.name,
                description = domain.description,
                permissions = domain.permissions.map { PermissionEmbeddable.from(it) }.toMutableSet(),
                isSystem = domain.isSystem,
                metadata = domain.metadata.toMutableMap(),
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}

@Embeddable
class PermissionEmbeddable(
    @Column(name = "resource", nullable = false, length = 100)
    var resource: String = "",
    @Column(name = "action", nullable = false, length = 50)
    var action: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 16)
    var scope: PermissionScope = PermissionScope.TENANT,
) {
    fun toDomain(): Permission =
        Permission(
            resource = resource,
            action = action,
            scope = scope,
        )

    companion object {
        fun from(domain: Permission): PermissionEmbeddable =
            PermissionEmbeddable(
                resource = domain.resource,
                action = domain.action,
                scope = domain.scope,
            )
    }
}
