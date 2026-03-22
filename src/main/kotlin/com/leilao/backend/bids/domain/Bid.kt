package com.leilao.backend.bids.domain

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.shared.domain.BaseEntity
import com.leilao.backend.users.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "bids")
class Bid(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    val auction: Auction,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    val bidder: User,

    @Column(name = "amount", nullable = false)
    val amount: Int,

    /**
     * ID idempotente enviado pelo cliente para evitar lances duplicados
     * em caso de retry de rede.
     */
    @Column(name = "request_id", unique = true, length = 64)
    val requestId: String? = null

) : BaseEntity() {

    val bidderId: UUID get() = bidder.id

    val auctionId: UUID get() = auction.id
}
