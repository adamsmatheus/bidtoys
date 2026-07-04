package com.leilao.backend.auctions.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.domain.AuctionStatusHistory
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.AuctionStatusHistoryRepository
import com.leilao.backend.notifications.application.UserNotificationBroadcastService
import com.leilao.backend.worker.outbox.OutboxEvent
import com.leilao.backend.worker.outbox.OutboxEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ConfirmPaymentUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val userNotificationBroadcastService: UserNotificationBroadcastService
) {

    @Transactional
    fun execute(auctionId: UUID, sellerId: UUID) {
        val auction = auctionRepository.findByIdWithLock(auctionId)
            .orElseThrow { NoSuchElementException("Leilão $auctionId não encontrado") }

        val fromStatus = auction.status
        auction.confirmPayment(sellerId)
        auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = auction,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.PAYMENT_CONFIRMED,
                changedByUserId = sellerId,
                reason = "Vendedor confirmou o recebimento do pagamento"
            )
        )

        val winnerUserId = auction.winnerUserId!!
        val payload = mapOf(
            "auctionId" to auction.id.toString(),
            "winnerUserId" to winnerUserId.toString(),
            "sellerId" to sellerId.toString(),
            "auctionTitle" to auction.title,
            "amount" to auction.currentPriceAmount
        )

        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "AUCTION",
                aggregateId = auction.id,
                eventType = "PAYMENT_CONFIRMED",
                payloadJson = objectMapper.writeValueAsString(payload)
            )
        )

        userNotificationBroadcastService.notifyPaymentConfirmed(
            winnerId = winnerUserId,
            auctionId = auction.id,
            auctionTitle = auction.title
        )
    }
}
