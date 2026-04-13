package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.domain.AuctionStatusHistory
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.AuctionStatusHistoryRepository
import com.leilao.backend.bids.infrastructure.BidRepository
import com.leilao.backend.worker.outbox.OutboxEventRepository
import com.leilao.backend.worker.outbox.OutboxEvent
import com.leilao.backend.worker.outbox.OutboxEventStatus
import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.notifications.application.UserNotificationBroadcastService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FinishAuctionUseCase(
    private val auctionRepository: AuctionRepository,
    private val bidRepository: BidRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository,
    private val objectMapper: ObjectMapper,
    private val auctionBroadcastService: AuctionBroadcastService,
    private val userNotificationBroadcastService: UserNotificationBroadcastService
) {

    private val log = LoggerFactory.getLogger(FinishAuctionUseCase::class.java)

    @Transactional
    fun execute(auctionId: UUID) {
        val auction = auctionRepository.findByIdWithLock(auctionId)
            .orElse(null) ?: return

        if (auction.status != AuctionStatus.ACTIVE) {
            log.debug("Leilão {} não está ativo ({}), ignorando encerramento", auctionId, auction.status)
            return
        }

        if (!auction.hasExpired()) {
            log.debug("Leilão {} ainda não expirou, ignorando", auctionId)
            return
        }

        val fromStatus = auction.status
        val bidCount = bidRepository.countByAuction_Id(auctionId)

        if (bidCount == 0L) {
            auction.finishNoBids()
            saveStatusHistory(auction, fromStatus, AuctionStatus.FINISHED_NO_BIDS, null)
            log.info("Leilão {} encerrado sem lances", auctionId)
        } else {
            val winnerBid = bidRepository.findById(auction.leadingBidId!!)
                .orElseThrow { IllegalStateException("Lance vencedor não encontrado: ${auction.leadingBidId}") }

            auction.finishWithWinner(winnerBid.bidderId)
            saveStatusHistory(auction, fromStatus, AuctionStatus.FINISHED_WITH_WINNER, null)

            publishOutboxEvent(auction)
            userNotificationBroadcastService.notifyAuctionWon(
                userId = winnerBid.bidderId,
                auctionId = auction.id,
                auctionTitle = auction.title,
                finalAmount = auction.currentPriceAmount
            )
            log.info("Leilão {} encerrado com vencedor {}", auctionId, winnerBid.bidderId)
        }

        auctionRepository.save(auction)
        auctionBroadcastService.broadcastAuctionFinished(auction)
    }

    private fun saveStatusHistory(
        auction: Auction,
        from: AuctionStatus,
        to: AuctionStatus,
        changedBy: UUID?
    ) {
        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = auction,
                fromStatus = from,
                toStatus = to,
                changedByUserId = changedBy,
                reason = "Encerramento automático pelo worker"
            )
        )
    }

    private fun publishOutboxEvent(auction: Auction) {
        val payload = mapOf(
            "auctionId" to auction.id.toString(),
            "winnerUserId" to auction.winnerUserId.toString(),
            "finalAmount" to auction.currentPriceAmount,
            "leadingBidId" to auction.leadingBidId.toString()
        )

        val event = OutboxEvent(
            aggregateType = "AUCTION",
            aggregateId = auction.id,
            eventType = "AUCTION_FINISHED_WITH_WINNER",
            payloadJson = objectMapper.writeValueAsString(payload)
        )

        outboxEventRepository.save(event)
    }
}
