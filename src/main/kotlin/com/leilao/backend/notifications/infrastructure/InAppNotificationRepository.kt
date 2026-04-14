package com.leilao.backend.notifications.infrastructure

import com.leilao.backend.notifications.domain.InAppNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface InAppNotificationRepository : JpaRepository<InAppNotification, UUID> {

    fun findTop50ByUserIdOrderByCreatedAtDesc(userId: UUID): List<InAppNotification>

    @Modifying
    @Query("UPDATE InAppNotification n SET n.read = TRUE WHERE n.userId = :userId AND n.read = FALSE")
    fun markAllAsReadByUserId(userId: UUID): Int
}
