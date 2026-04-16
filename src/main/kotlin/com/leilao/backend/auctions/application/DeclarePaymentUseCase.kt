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
class DeclarePaymentUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val userNotificationBroadcastService: UserNotificationBroadcastService
) {

    @Transactional
    fun execute(auctionId: UUID, winnerId: UUID, holdShipment: Boolean = false) {
        val auction = auctionRepository.findByIdWithLock(auctionId)
            .orElseThrow { NoSuchElementException("Leilão $auctionId não encontrado") }

        val fromStatus = auction.status
        auction.declarePayment(winnerId, holdShipment)
        auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = auction,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.PAYMENT_DECLARED,
                changedByUserId = winnerId,
                reason = "Vencedor declarou que realizou o pagamento"
            )
        )

        val payload = mapOf(
            "auctionId" to auction.id.toString(),
            "sellerId" to auction.seller.id.toString(),
            "winnerUserId" to winnerId.toString(),
            "auctionTitle" to auction.title,
            "amount" to auction.currentPriceAmount
        )

        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "AUCTION",
                aggregateId = auction.id,
                eventType = "PAYMENT_DECLARED",
                payloadJson = objectMapper.writeValueAsString(payload)
            )
        )

        userNotificationBroadcastService.notifyPaymentDeclared(
            sellerId = auction.seller.id,
            auctionId = auction.id,
            auctionTitle = auction.title,
            amount = auction.currentPriceAmount
        )
    }
}
