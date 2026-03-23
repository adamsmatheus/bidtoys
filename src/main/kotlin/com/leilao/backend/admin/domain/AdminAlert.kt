package com.leilao.backend.admin.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "admin_alerts")
class AdminAlert(

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    val type: AdminAlertType,

    @Column(name = "auction_id")
    val auctionId: UUID? = null,

    @Column(name = "notification_id")
    val notificationId: UUID? = null,

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    val message: String

) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: AdminAlertStatus = AdminAlertStatus.OPEN
        protected set

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null
        protected set

    @Column(name = "resolved_by_user_id")
    var resolvedByUserId: UUID? = null
        protected set

    fun resolve(resolvedByUserId: UUID) {
        status = AdminAlertStatus.RESOLVED
        this.resolvedAt = Instant.now()
        this.resolvedByUserId = resolvedByUserId
    }
}
