package com.leilao.backend.auctions.application

import com.leilao.backend.admin.infrastructure.AdminAlertRepository
import com.leilao.backend.auctions.infrastructure.AuctionImageRepository
import com.leilao.backend.auctions.infrastructure.AuctionRepository
import com.leilao.backend.auctions.infrastructure.AuctionStatusHistoryRepository
import com.leilao.backend.bids.infrastructure.BidRepository
import com.leilao.backend.notifications.infrastructure.NotificationRepository
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import com.leilao.backend.shared.storage.StorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteAuctionUseCase(
    private val auctionRepository: AuctionRepository,
    private val auctionImageRepository: AuctionImageRepository,
    private val auctionStatusHistoryRepository: AuctionStatusHistoryRepository,
    private val bidRepository: BidRepository,
    private val notificationRepository: NotificationRepository,
    private val adminAlertRepository: AdminAlertRepository,
    private val storageService: StorageService
) {

    @Transactional
    fun execute(auctionId: UUID, userId: UUID) {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NotFoundException("Leilão não encontrado") }

        if (!auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para excluir este leilão")
        }

        if (!auction.status.canDelete()) {
            throw InvalidStateException(
                "Não é possível excluir um leilão finalizado",
                "INVALID_STATE_FOR_DELETE"
            )
        }

        // Remove arquivos de imagem do storage antes de deletar do banco
        val images = auctionImageRepository.findByAuction_IdOrderByPositionAsc(auctionId)
        images.forEach { storageService.delete(it.fileKey) }

        // Remove registros dependentes sem CASCADE
        notificationRepository.deleteByAuctionId(auctionId)
        adminAlertRepository.deleteByAuctionId(auctionId)
        bidRepository.deleteByAuction_Id(auctionId)
        auctionStatusHistoryRepository.deleteByAuction_Id(auctionId)

        // auction_images são deletadas automaticamente via ON DELETE CASCADE
        auctionRepository.delete(auction)
    }
}
