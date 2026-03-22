package com.leilao.backend.worker.outbox

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = 'PENDING'
        AND e.availableAt <= :now
        ORDER BY e.availableAt ASC
    """)
    fun findPendingEvents(now: Instant): List<OutboxEvent>

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = 'PENDING'
        AND e.eventType = :eventType
        AND e.availableAt <= :now
        ORDER BY e.availableAt ASC
    """)
    fun findPendingEventsByType(eventType: String, now: Instant): List<OutboxEvent>
}
