package com.erp.identity.infrastructure.persistence.entity

import com.erp.identity.domain.model.tenant.Organization
import com.erp.identity.domain.model.tenant.Subscription
import com.erp.identity.domain.model.tenant.SubscriptionPlan
import com.erp.identity.domain.model.tenant.Tenant
import com.erp.identity.domain.model.tenant.TenantId
import com.erp.identity.domain.model.tenant.TenantStatus
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
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
    name = "identity_tenants",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_tenants_slug", columnNames = ["slug"]),
    ],
)
class TenantEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "name", nullable = false, length = 200)
    var name: String = "",
    @Column(name = "slug", nullable = false, length = 50)
    var slug: String = "",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: TenantStatus = TenantStatus.PROVISIONING,
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 32)
    var subscriptionPlan: SubscriptionPlan = SubscriptionPlan.STARTER,
    @Column(name = "subscription_start_date", nullable = false)
    var subscriptionStartDate: Instant = Instant.now(),
    @Column(name = "subscription_end_date")
    var subscriptionEndDate: Instant? = null,
    @Column(name = "subscription_max_users", nullable = false)
    var subscriptionMaxUsers: Int = 1,
    @Column(name = "subscription_max_storage", nullable = false)
    var subscriptionMaxStorage: Long = 1024,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "identity_tenant_features",
        joinColumns = [JoinColumn(name = "tenant_id")],
    )
    @Column(name = "feature", nullable = false, length = 100)
    var subscriptionFeatures: MutableSet<String> = mutableSetOf(),
    @Embedded
    var organization: OrganizationEmbeddable? = null,
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "identity_tenant_metadata",
        joinColumns = [JoinColumn(name = "tenant_id")],
    )
    @MapKeyColumn(name = "metadata_key", length = 100)
    @Column(name = "metadata_value", length = 500)
    var metadata: MutableMap<String, String> = mutableMapOf(),
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): Tenant =
        Tenant(
            id = TenantId(id),
            name = name,
            slug = slug,
            status = status,
            subscription =
                Subscription(
                    plan = subscriptionPlan,
                    startDate = subscriptionStartDate,
                    endDate = subscriptionEndDate,
                    maxUsers = subscriptionMaxUsers,
                    maxStorage = subscriptionMaxStorage,
                    features = subscriptionFeatures.toSet(),
                ),
            organization = organization?.toDomain(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            metadata = metadata.toMap(),
        )

    companion object {
        fun from(domain: Tenant): TenantEntity =
            TenantEntity(
                id = domain.id.value,
                name = domain.name,
                slug = domain.slug,
                status = domain.status,
                subscriptionPlan = domain.subscription.plan,
                subscriptionStartDate = domain.subscription.startDate,
                subscriptionEndDate = domain.subscription.endDate,
                subscriptionMaxUsers = domain.subscription.maxUsers,
                subscriptionMaxStorage = domain.subscription.maxStorage,
                subscriptionFeatures = domain.subscription.features.toMutableSet(),
                organization = domain.organization?.let { OrganizationEmbeddable.from(it) },
                metadata = domain.metadata.toMutableMap(),
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
    }
}

@Embeddable
class OrganizationEmbeddable(
    @Column(name = "organization_legal_name", length = 200)
    var legalName: String? = null,
    @Column(name = "organization_tax_id", length = 100)
    var taxId: String? = null,
    @Column(name = "organization_industry", length = 100)
    var industry: String? = null,
    @Embedded
    var address: AddressEmbeddable? = null,
    @Column(name = "organization_contact_email", length = 200)
    var contactEmail: String? = null,
    @Column(name = "organization_contact_phone", length = 50)
    var contactPhone: String? = null,
) {
    fun toDomain(): Organization =
        Organization(
            legalName = legalName ?: "",
            taxId = taxId,
            industry = industry,
            address = address?.toDomain(),
            contactEmail = contactEmail ?: "",
            contactPhone = contactPhone,
        )

    companion object {
        fun from(domain: Organization): OrganizationEmbeddable =
            OrganizationEmbeddable(
                legalName = domain.legalName,
                taxId = domain.taxId,
                industry = domain.industry,
                address = domain.address?.let { AddressEmbeddable.from(it) },
                contactEmail = domain.contactEmail,
                contactPhone = domain.contactPhone,
            )
    }
}

@Embeddable
class AddressEmbeddable(
    @Column(name = "address_street", length = 200)
    var street: String? = null,
    @Column(name = "address_city", length = 100)
    var city: String? = null,
    @Column(name = "address_state", length = 100)
    var state: String? = null,
    @Column(name = "address_postal_code", length = 50)
    var postalCode: String? = null,
    @Column(name = "address_country", length = 2)
    var country: String? = null,
) {
    fun toDomain(): com.erp.identity.domain.model.tenant.Address =
        com.erp.identity.domain.model.tenant.Address(
            street = street ?: "",
            city = city ?: "",
            state = state,
            postalCode = postalCode ?: "",
            country = country ?: "",
        )

    companion object {
        fun from(domain: com.erp.identity.domain.model.tenant.Address): AddressEmbeddable =
            AddressEmbeddable(
                street = domain.street,
                city = domain.city,
                state = domain.state,
                postalCode = domain.postalCode,
                country = domain.country,
            )
    }
}
