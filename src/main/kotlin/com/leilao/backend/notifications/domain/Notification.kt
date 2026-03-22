package com.leilao.backend.notifications.domain

import com.leilao.backend.shared.domain.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification(

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "auction_id")
    val auctionId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    val type: NotificationType,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    val channel: NotificationChannel,

    @Column(name = "payload_json", columnDefinition = "TEXT")
    val payloadJson: String? = null

) : AuditableEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: NotificationStatus = NotificationStatus.PENDING
        private set

    @Column(name = "provider_message_id", length = 255)
    var providerMessageId: String? = null
        private set

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null
        private set

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        private set

    @Column(name = "sent_at")
    var sentAt: Instant? = null
        private set

    fun markSent(providerMessageId: String? = null) {
        status = NotificationStatus.SENT
        this.providerMessageId = providerMessageId
        this.sentAt = Instant.now()
    }

    fun markFailed(errorMessage: String) {
        status = NotificationStatus.FAILED
        this.errorMessage = errorMessage
        this.retryCount++
    }

    fun markDelivered() {
        status = NotificationStatus.DELIVERED
    }
}
