package com.leilao.backend.notifications.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "in_app_notifications")
class InAppNotification(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    val auctionId: UUID? = null,

    @Column(nullable = false)
    var read: Boolean = false,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
