package com.leilao.backend.audit.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "audit_logs")
class AuditLog(

    @Column(name = "actor_user_id")
    val actorUserId: UUID? = null,

    @Column(name = "entity_type", nullable = false, length = 50)
    val entityType: String,

    @Column(name = "entity_id", nullable = false)
    val entityId: UUID,

    @Column(name = "action", nullable = false, length = 50)
    val action: String,

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    val metadataJson: String? = null

) : BaseEntity()
