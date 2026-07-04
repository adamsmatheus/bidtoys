package com.leilao.backend.auctions.application

import com.leilao.backend.auctions.infrastructure.AuctionImageRepository
import com.leilao.backend.shared.exception.ForbiddenException
import com.leilao.backend.shared.exception.InvalidStateException
import com.leilao.backend.shared.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SetCoverImageUseCase(
    private val auctionImageRepository: AuctionImageRepository
) {

    @Transactional
    fun execute(auctionId: UUID, imageId: UUID, userId: UUID) {
        val image = auctionImageRepository.findById(imageId)
            .orElseThrow { NotFoundException("Imagem não encontrada") }

        if (image.auction.id != auctionId) {
            throw NotFoundException("Imagem não encontrada")
        }

        if (!image.auction.isOwnedBy(userId)) {
            throw ForbiddenException("Você não tem permissão para alterar esta imagem")
        }

        if (!image.auction.status.canEdit()) {
            throw InvalidStateException(
                "Não é possível alterar imagens de um leilão no estado ${image.auction.status}",
                "INVALID_STATE_FOR_IMAGE_EDIT"
            )
        }

        if (image.position == 0) return // já é a capa

        val allImages = auctionImageRepository.findByAuction_IdOrderByPositionAsc(auctionId)

        // Remove o alvo da lista e reinsere no início, mantendo a ordem relativa dos demais
        val reordered = listOf(image) + allImages.filter { it.id != imageId }
        reordered.forEachIndexed { idx, img -> img.position = idx }
        auctionImageRepository.saveAll(reordered)
    }
}
