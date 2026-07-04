package com.leilao.backend.auctions.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "dispute_messages")
class DisputeMessage(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    val auction: Auction,

    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,

    @Column(name = "sender_name", nullable = false, length = 255)
    val senderName: String,

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    val message: String

) : BaseEntity()
