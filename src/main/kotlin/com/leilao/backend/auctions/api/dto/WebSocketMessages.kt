package com.leilao.backend.auctions.api.dto

import com.leilao.backend.auctions.domain.AuctionStatus
import java.time.Instant
import java.util.UUID

data class NewBidMessage(
    val type: String = "NEW_BID",
    val bidId: UUID,
    val auctionId: UUID,
    val bidderId: UUID,
    val amount: Int,
    val newCurrentPrice: Int,
    val nextMinimumBid: Int,
    val endsAt: Instant?,
    val bidAt: Instant
)

data class AuctionFinishedMessage(
    val type: String = "AUCTION_FINISHED",
    val auctionId: UUID,
    val status: AuctionStatus,
    val winnerUserId: UUID?,
    val finalPrice: Int,
    val finishedAt: Instant?
)
