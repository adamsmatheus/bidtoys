package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.api.dto.AuctionFinishedMessage
import com.leilao.backend.auctions.api.dto.NewBidMessage
import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.bids.domain.Bid
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class AuctionBroadcastService(
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(AuctionBroadcastService::class.java)

    fun broadcastNewBid(bid: Bid, auction: Auction) {
        val destination = "/topic/auctions/${auction.id}"
        messagingTemplate.convertAndSend(
            destination,
            NewBidMessage(
                bidId           = bid.id,
                auctionId       = auction.id,
                bidderId        = bid.bidderId,
                amount          = bid.amount,
                newCurrentPrice = auction.currentPriceAmount,
                nextMinimumBid  = auction.nextMinimumBid(),
                endsAt          = auction.endsAt,
                bidAt           = bid.createdAt
            )
        )
        log.debug("Broadcast NEW_BID para {}", destination)
    }

    fun broadcastAuctionFinished(auction: Auction) {
        val destination = "/topic/auctions/${auction.id}"
        messagingTemplate.convertAndSend(
            destination,
            AuctionFinishedMessage(
                auctionId    = auction.id,
                status       = auction.status,
                winnerUserId = auction.winnerUserId,
                finalPrice   = auction.currentPriceAmount,
                finishedAt   = auction.finishedAt
            )
        )
        log.debug("Broadcast AUCTION_FINISHED para {}", destination)
    }
}
