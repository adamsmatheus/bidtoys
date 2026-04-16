package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.api.dto.BuyerAuctionItem
import com.leilao.backend.auctions.api.dto.BuyerSummaryResponse
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.users.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListBuyersUseCase(
    private val auctionRepository: AuctionRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun execute(sellerId: UUID): List<BuyerSummaryResponse> {
        val auctions = auctionRepository.findBySellerIdAndWinnerUserIdIsNotNull(sellerId)

        val winnerIds = auctions.mapNotNull { it.winnerUserId }.distinct()
        val usersById = userRepository.findAllById(winnerIds).associateBy { it.id }

        return auctions
            .groupBy { it.winnerUserId!! }
            .map { (winnerId, winnerAuctions) ->
                val buyerName = usersById[winnerId]?.name ?: "Usuário desconhecido"
                BuyerSummaryResponse(
                    buyerId = winnerId,
                    buyerName = buyerName,
                    auctionCount = winnerAuctions.size,
                    totalAmount = winnerAuctions.sumOf { it.currentPriceAmount },
                    auctions = winnerAuctions
                        .sortedByDescending { it.finishedAt }
                        .map { a ->
                            BuyerAuctionItem(
                                id = a.id,
                                title = a.title,
                                currentPriceAmount = a.currentPriceAmount,
                                status = a.status,
                                finishedAt = a.finishedAt,
                                holdShipment = a.holdShipment,
                                shipmentStatus = a.shipmentStatus,
                                trackingCode = a.trackingCode
                            )
                        }
                )
            }
            .sortedBy { it.buyerName }
    }
}
