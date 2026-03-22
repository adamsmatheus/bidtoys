package com.leilao.backend.worker.outbox

import com.leilao.backend.shared.domain.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class OutboxEventStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}

@Entity
@Table(name = "outbox_events")
class OutboxEvent(

    @Column(name = "aggregate_type", nullable = false, length = 50)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    val payloadJson: String,

    @Column(name = "available_at", nullable = false)
    val availableAt: Instant = Instant.now()

) : AuditableEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OutboxEventStatus = OutboxEventStatus.PENDING
        private set

    @Column(name = "processed_at")
    var processedAt: Instant? = null
        private set

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        private set

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null
        private set

    fun markProcessing() {
        status = OutboxEventStatus.PROCESSING
    }

    fun markProcessed() {
        status = OutboxEventStatus.PROCESSED
        processedAt = Instant.now()
    }

    fun markFailed(error: String) {
        status = OutboxEventStatus.FAILED
        lastError = error
        retryCount++
    }

    fun resetToPending() {
        status = OutboxEventStatus.PENDING
    }
}
