package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.domain.ShipmentStatus
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.notifications.application.UserNotificationBroadcastService
import com.leilao.backend.shared.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateShipmentStatusUseCase(
    private val auctionRepository: AuctionRepository,
    private val userNotificationBroadcastService: UserNotificationBroadcastService
) {

    @Transactional
    fun execute(auctionId: UUID, sellerId: UUID, newStatus: ShipmentStatus, trackingCode: String?) {
        val auction = auctionRepository.findByIdWithLock(auctionId)
            .orElseThrow { NotFoundException("Leilão $auctionId não encontrado") }

        auction.updateShipmentStatus(sellerId, newStatus, trackingCode)
        auctionRepository.save(auction)

        val winnerUserId = auction.winnerUserId!!
        userNotificationBroadcastService.notifyShipmentStatusChanged(
            winnerId = winnerUserId,
            auctionId = auction.id,
            auctionTitle = auction.title,
            newStatus = newStatus,
            trackingCode = auction.trackingCode
        )
    }
}
