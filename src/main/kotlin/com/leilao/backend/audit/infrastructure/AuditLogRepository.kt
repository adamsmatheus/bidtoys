package com.leilao.backend.audit.infrastructure

import com.leilao.backend.audit.domain.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, UUID> {
    fun findByEntityTypeAndEntityId(entityType: String, entityId: UUID, pageable: Pageable): Page<AuditLog>
    fun findByActorUserId(actorUserId: UUID, pageable: Pageable): Page<AuditLog>
}
