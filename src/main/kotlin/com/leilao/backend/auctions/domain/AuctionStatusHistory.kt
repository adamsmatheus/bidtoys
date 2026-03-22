package com.leilao.backend.auctions.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "auction_status_history")
class AuctionStatusHistory(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    val auction: Auction,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    val fromStatus: AuctionStatus?,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    val toStatus: AuctionStatus,

    @Column(name = "changed_by_user_id")
    val changedByUserId: UUID? = null,

    @Column(name = "reason", columnDefinition = "TEXT")
    val reason: String? = null,

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    val metadataJson: String? = null

) : BaseEntity()
