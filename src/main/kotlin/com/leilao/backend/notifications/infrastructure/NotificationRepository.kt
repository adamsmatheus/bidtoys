package com.leilao.backend.notifications.infrastructure

import com.leilao.backend.notifications.domain.Notification
import com.leilao.backend.notifications.domain.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByAuctionIdAndStatus(auctionId: UUID, status: NotificationStatus): List<Notification>
}
