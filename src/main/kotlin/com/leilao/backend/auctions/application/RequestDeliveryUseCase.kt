package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.notifications.application.UserNotificationBroadcastService
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RequestDeliveryUseCase(
    private val auctionRepository: AuctionRepository,
    private val userRepository: UserRepository,
    private val userNotificationBroadcastService: UserNotificationBroadcastService
) {

    @Transactional
    fun execute(auctionId: UUID, winnerId: UUID) {
        val auction = auctionRepository.findByIdWithLock(auctionId)
            .orElseThrow { NotFoundException("Leilão $auctionId não encontrado") }

        auction.requestDelivery(winnerId)
        auctionRepository.save(auction)

        val buyer = userRepository.findById(winnerId)
            .orElseThrow { NotFoundException("Usuário $winnerId não encontrado") }

        userNotificationBroadcastService.notifyDeliveryRequested(
            sellerId = auction.seller.id,
            auctionId = auction.id,
            auctionTitle = auction.title,
            buyerName = buyer.name
        )
    }
}
