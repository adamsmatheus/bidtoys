package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.domain.AuctionStatusHistory
import com.leilao.backend.auctions.domain.DisputeMessage
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.AuctionStatusHistoryRepository
import com.leilao.backend.auctions.infrastructure.DisputeMessageRepository
import com.leilao.backend.notifications.application.UserNotificationBroadcastService
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DisputePaymentUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository,
    private val disputeMessageRepository: DisputeMessageRepository,
    private val userRepository: UserRepository,
    private val userNotificationBroadcastService: UserNotificationBroadcastService
) {

    @Transactional
    fun execute(auctionId: UUID, sellerId: UUID, reason: String) {
        val auction = auctionRepository.findByIdWithLock(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        val seller = userRepository.findById(sellerId)
            .orElseThrow { NotFoundException("Usuário não encontrado") }

        val fromStatus = auction.status
        auction.disputePayment(sellerId, reason)
        auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = auction,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.PAYMENT_DISPUTED,
                changedByUserId = sellerId,
                reason = "Vendedor contestou: $reason"
            )
        )

        // Primeira mensagem do chat é automaticamente o motivo da contestação
        disputeMessageRepository.save(
            DisputeMessage(
                auction = auction,
                senderId = sellerId,
                senderName = seller.name,
                message = reason
            )
        )

        userNotificationBroadcastService.notifyPaymentDisputed(
            winnerId = auction.winnerUserId!!,
            auctionId = auction.id,
            auctionTitle = auction.title
        )
    }
}
