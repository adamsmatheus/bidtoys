package com.leilao.backend.auctions.domain

import com.leilao.backend.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "auction_images")
class AuctionImage(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    val auction: Auction,

    @Column(name = "file_key", nullable = false, length = 512)
    val fileKey: String,

    @Column(name = "file_url", nullable = false, length = 1024)
    val fileUrl: String,

    @Column(name = "position", nullable = false)
    var position: Int = 0

) : BaseEntity()
