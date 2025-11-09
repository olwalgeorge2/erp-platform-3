package com.erp.identity.infrastructure.consumer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_processed_events",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_processed_events_fingerprint", columnNames = ["fingerprint"]),
    ],
)
class ProcessedEventEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),
    @Column(name = "fingerprint", nullable = false, length = 128)
    var fingerprint: String = "",
    @Column(name = "processed_at", nullable = false)
    var processedAt: Instant = Instant.now(),
)
