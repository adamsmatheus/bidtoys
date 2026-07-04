package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.domain.DisputeMessage
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.DisputeMessageRepository
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.NotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ListDisputeMessagesUseCase(
    private val auctionRepository: AuctionRepository,
    private val disputeMessageRepository: DisputeMessageRepository
) {

    fun execute(auctionId: UUID, requesterId: UUID): List<DisputeMessage> {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        val isSeller = auction.seller.id == requesterId
        val isWinner = auction.winnerUserId == requesterId
        if (!isSeller && !isWinner) {
            throw ForbiddenException("Apenas o vendedor ou vencedor podem acessar o chat de disputa")
        }

        return disputeMessageRepository.findByAuction_IdOrderByCreatedAtAsc(auctionId)
    }
}
