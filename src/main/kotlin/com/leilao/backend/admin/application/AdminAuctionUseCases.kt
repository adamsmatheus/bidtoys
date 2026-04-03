package com.leilao.backend.admin.application

import com.leilao.backend.auctions.domain.Auction
import com.leilao.backend.auctions.domain.AuctionStatus
import com.leilao.backend.auctions.domain.AuctionStatusHistory
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.AuctionStatusHistoryRepository
import com.leilao.backend.shared.exception.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListPendingAuctionsUseCase(
    private val auctionRepository: AuctionRepository
) {
    @Transactional(readOnly = true)
    fun execute(pageable: Pageable): Page<Auction> =
        auctionRepository.findByStatus(AuctionStatus.PENDING_APPROVAL, pageable)
}

@Service
class ApproveAuctionUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository
) {
    @Transactional
    fun execute(auctionId: UUID, adminUserId: UUID): Auction {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        val fromStatus = auction.status
        auction.approve(adminUserId)
        val saved = auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = saved,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.READY_TO_START,
                changedByUserId = adminUserId
            )
        )

        return saved
    }
}

@Service
class RejectAuctionUseCase(
    private val auctionRepository: AuctionRepository,
    private val statusHistoryRepository: AuctionStatusHistoryRepository
) {
    @Transactional
    fun execute(auctionId: UUID, adminUserId: UUID, reason: String): Auction {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        val fromStatus = auction.status
        auction.reject(adminUserId, reason)
        val saved = auctionRepository.save(auction)

        statusHistoryRepository.save(
            AuctionStatusHistory(
                auction = saved,
                fromStatus = fromStatus,
                toStatus = AuctionStatus.REJECTED,
                changedByUserId = adminUserId,
                reason = reason
            )
        )

        return saved
    }
}
